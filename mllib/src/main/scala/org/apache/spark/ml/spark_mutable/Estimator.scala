package org.apache.spark.ml.spark_mutable

import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType

sealed trait FittingResult
case object Success extends FittingResult
case class Failure(msg: String) extends FittingResult

/**
 * A transformer
 * @tparam T
 */
trait Transformer[T <: Transformer[T]] {

  /**
   * Predicts the data based on the fit.
   *
   * This method is reentrant. If this class happens to be also an estimator, it can be called while the estimator
   * is fitting.
   *
   * Implementation note:
   * This method should have no visible side effect.
   * @param data
   * @return
   */
  def transform(data: DataFrame): DataFrame

  def copy(paramMap: ParamMap): T

  // Dev API
  def transformSchema(schema: StructType): StructType
}

/**
 * An estimator in Spark.
 *
 * The estimator can be fit onto some data, and then be used to transform data.
 */
trait Estimator[T <: Estimator[T]] extends Transformer[T] {

  /**
   * Fits the current estimator onto some data.
   *
   * For each estimator, only one fitting can happen at a time.
   * @param data
   * @return
   */
  def fit(data: DataFrame): Unit

  /**
   * Stops the fitting procedure.
   *
   * After this method returns, the fitting procedure is guaranteed to have terminated.
   * @return
   */
  def stopFit(): Unit

  // Make clear that copy() will return something that can be fit again

  // Add a method to reset the state?
//  def resetState(): Estimator

  // Add a method to check if it is fitting?
  // Add a method to join on the fit?
  // Add a method to return a completion percentage?
}


// Separate the pipeline into transformer pipelines (for which fit() is a no-op) and EstimatorPipeline
trait TransformerPipeline extends Transformer[TransformerPipeline] {
  def stages: Seq[Transformer[_]] = ???
}

object TransformerPipeline {
  def apply(stages: Transformer[_]*): TransformerPipeline = ???
}

trait EstimatorPipeline extends Estimator[EstimatorPipeline]{

  //
  def stages: Seq[Either[Transformer[_], Estimator[_]]] = ???

  override def fit(df: DataFrame): Unit = {
    var data = df
    stages.foreach {
      case Right(m) =>
        m.fit(data)
        data = m.transform(data)
      case Left(m) =>
        data = m.transform(data)
    }
  }
}

object EstimatorPipeline {
  def apply(stages: Transformer[_]*): EstimatorPipeline = {
    ???
  }
}
