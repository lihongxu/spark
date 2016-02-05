package org.apache.spark.ml.spark_mutable

import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType

/**
 * Implements the transformer interface, and implements the most tedious aspects of the interfaces. It is recommended
 * developers use this class instead of directly subclassing Transformer.
 * @tparam T
 */
abstract class AbstractTransformer[T <: AbstractTransformer[T]] extends Transformer[T] {

  // *** Internal developers APIs ***

  // Developers should reimplement these methods.

  // Will be called by the default implementation, which adds some extra sanity checks before and after.
  // Note: could be performed automatically based on the inferring the columns from parameters.
  protected def performSchemaTransform(schema: StructType): StructType

  // To be implemented by developers.
  // All the checks for the input and output types are being done outside this method, no need to do it here.
  protected def performTransform(df: DataFrame): DataFrame

  // Default copy does nothing
  override def copy(paramMap: ParamMap): T = self

  // *** Default implementations ***

  // Should not be touched by developers

  // Reference to self. Useful as a reference when some more type casting is needed (when writing traits or
  // abstract classes).
  final protected def self: T = this.asInstanceOf[T]

  // Default implementation performs a number of checks and delegates to the custom implementation.
  final override def transformSchema(schema: StructType): StructType = {
    // Validate parameters first
    performSchemaTransform(schema)
    // Should do extra checks here to check that the columns that were supposed to be added are here.
  }

  final override def transform(df: DataFrame): DataFrame = {
    // Check that the schema is compatible, and that the parameters are valid.
    val expectedSchema = transformSchema(df.schema)
    val res = performTransform(df)
    // Check that the schema of the output dataframe respects the expectedSchema
    res
  }
}

abstract class AbstractEstimator[T <: AbstractEstimator[T]] extends AbstractTransformer[T] with Estimator[T] {

}

/**
 * Implements the estimator interface and manages all the most tedious complicated aspect for the developer.
 *
 * Assumes the fitting produces an internal state that can then be used to perform the transform. The content of the
 * state is left to the fitting procedure to detail.
 *
 * @tparam State
 */
abstract class EstimatorWithState[State, T <: AbstractEstimator[T]] extends AbstractEstimator[T] {

  import EstimatorWithState._

  // Could be replaced by option + exception
  protected sealed trait StateUpdate
  // Done
  protected case class Done() extends StateUpdate
  // Done, with a last update
  protected case class DoneSuccess(s: State) extends StateUpdate
  // Finished an update, still more work to do.
  protected case class StateUpdateSuccess(s: State) extends StateUpdate
  // Finished an update, and it triggered an error that prevents further work.
  protected case class StateUpdateError(msg: String) extends StateUpdate

  // *** Functions to be implemented by the developer ***
  /**
   * The initial state before any fitting is performed. It should depend on the parameters only.
   * @return
   */
  @throws[Exception]("If state cannot be initialized")
  protected def initialState: State

  /**
   * This method should not change state outside, and should only read parameters.
   * @param currentState
   * @param data
   * @return
   */
  @throws[Exception]("If progress cannot be made due to error")
  protected def performUpdate(currentState: State, data: DataFrame): StateUpdate

  /**
   * Use this method to update the output parameters if needed be.
   *
   * This method should only change parameters.
   * @param state
   */
  protected def commitState(state: State): Unit = {}

  // *** Functions that can be used by the developer ***

  /**
   * The current state, accessible to perform estimation.
   *
   * @return
   */
  // TODO on the side of parameters: make sure that noone else is accessing the parameters at the same time,
  // and call the validation after the parameters are updated.
  protected def currentState: State = getOrCreateState()

  // IMPLEMENTATION OF THE STATE MACHINE

  private[this] val lock = new Object
  // Empty first, so that parameters can be initialized after construction.
  private[this] var state: Option[State] = None
  private[this] var fittingState: EstimatorState = ReadyToFit
  private[this] var lastFittingResult: Option[FittingResult] = None


  @throws[Exception]("If state cannot be initialized")
  private[this] def getOrCreateState(): State = lock.synchronized {
    state match {
      case Some(s) => s
      case None =>
        val s = initialState
        state = Some(s)
        s
    }
  }

  // **** Implementation of the public methods ****

  override def fit(data: DataFrame): Unit = {
    // We could add the stack call of the other procedure doing the fit.
    lock.synchronized {
      require(fittingState == ReadyToFit, "This estimator is already in the process of fitting")
      fittingState = CurrentlyFitting
    }
    while(fittingState == CurrentlyFitting) {
      val s = getOrCreateState()
      performUpdate(s, data) match {
        case Done() =>
          lock.synchronized {
            fittingState = ReadyToFit
            lastFittingResult = Some(Success)
          }

        case DoneSuccess(s2) =>
          lock.synchronized {
            state = Some(s2)
            fittingState = ReadyToFit
            lastFittingResult = Some(Success)
          }

        case StateUpdateSuccess(s2) =>
          lock.synchronized {
            state = Some(s2)
          }
        case StateUpdateError(msg) =>
          lock.synchronized {
            fittingState = ReadyToFit
            lastFittingResult = Some(Failure(msg))
          }
      }
    }
    lock.synchronized {
      fittingState = ReadyToFit
    }
    lastFittingResult.get
  }

  override def stopFit(): Unit = {
    lock.synchronized {
      require(fittingState != ReadyToFit, "Not currently fitting")
    }
    lock.synchronized {
      fittingState = ShouldStopFit
    }
    // This is broken, because the fitting method may return, and be restarted again (clearing the last fitting result)
    // Not important for now.
    while (fittingState != ReadyToFit) {
      Thread.sleep(100)
    }
    assert(lastFittingResult.isDefined)
    lastFittingResult.get
  }
}

object EstimatorWithState {
  private sealed trait EstimatorState
  private object ReadyToFit extends EstimatorState
  private object CurrentlyFitting extends EstimatorState
  private object ShouldStopFit extends EstimatorState
}