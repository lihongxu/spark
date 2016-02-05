package org.apache.spark.ml.spark_immutable

import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.DataFrame

import scala.concurrent.Future

// *** Interfaces ***

abstract class PipelineStage[S <: PipelineStage[S]] {
  protected def repr: S = this.asInstanceOf[S]
  def copy(paramMap: ParamMap): S
}

abstract class Transformer[M <: Transformer[M]] extends PipelineStage[M] {
  def transform(df: DataFrame): DataFrame
}

abstract class Estimator[
M <: Transformer[M],
IF <: InteractiveFit[M],
E <: Estimator[M, IF, E]] extends PipelineStage[E] {
  def fit(df: DataFrame): M
  def interactiveFit(df: DataFrame): IF
}

abstract class InteractiveFit[M <: Transformer[M]] {
  def waitForCompletion(): M
  def currentResult: M
  def stop(): Unit = {}
  def update(params: ParamMap): Unit = {}
  // Some setters, etc.
}

// For estimators that have to wait until the end of fit() to be changed, use this class that implements the batch fit.
abstract class BatchEstimator[
M <: Transformer[M],
E <: Estimator[M, InteractiveFit[M], E]] extends Estimator[M, InteractiveFit[M], E] {

  private class MyBatchFit(df: DataFrame) extends InteractiveFit[M] {
    override def currentResult = fit(df)
    override def waitForCompletion() = fit(df)
  }

  final override def interactiveFit(df: DataFrame): InteractiveFit[M] = new MyBatchFit(df)
}

// Separate the pipeline into transformer pipelines (which do not have fitting methods) from estimator pipelines.
trait TransformerPipeline extends Transformer[TransformerPipeline] {
  def stages: Seq[Transformer[_]] = ???
}

object TransformerPipeline {
  // Can only pass transformers (guaranteed to be immutable).
  def apply(stages: Transformer[_]*): TransformerPipeline = ???
}

// There are different options for interactive fitting
// one stage after the other, and possibility to interrupt the current step if it converges fast enough
// start with a subset of the data and perform a whole training, and gradually consider a larger fraction of the data
// Since it is immutable, multiple strategies can be launched in parallel...
trait PipelineInteractiveFit extends InteractiveFit[TransformerPipeline]

// This calls the interactiveFit() or fit() methods or each of the steps if necessary.
trait EstimatorPipeline extends Estimator[TransformerPipeline, PipelineInteractiveFit, EstimatorPipeline]{

  def stages: Seq[Either[Transformer[_], Estimator[_ <: Transformer[_], _, _]]] = ???

  // Simple batch fit implementation
  override def fit(df: DataFrame): TransformerPipeline = {
    var data = df
    val transformerStages = stages.map {
      case Right(m) =>
        val model = m.fit(data)
        data = model.transform(data)
        model
      case Left(m) =>
        data = m.transform(data)
        m
    }
    TransformerPipeline(transformerStages: _*)
  }

  override def interactiveFit(df: DataFrame): PipelineInteractiveFit = ???
}

object EstimatorPipeline {
  // Can pass any pipeline stage, the estimators will be fit.
  def apply(stages: PipelineStage[_]*): EstimatorPipeline = {
    ???
  }
}

// *** A few models ***

case class KMeansModel() extends Transformer[KMeansModel] {
  def copy(paramMap: ParamMap): KMeansModel = ???
  def transform(df: DataFrame): DataFrame = ???
}

trait KMeansModelIF extends InteractiveFit[KMeansModel] {
  // With more traits to change the parameters, etc.
}

case class KMeans() extends Estimator[KMeansModel, KMeansModelIF, KMeans] {
  def copy(paramMap: ParamMap): KMeans = ???
  override def fit(df: DataFrame): KMeansModel = interactiveFit(df).waitForCompletion()
  override def interactiveFit(df: DataFrame): KMeansModelIF = ??? //new KMeansModelIF(this)
  def setInputCol(s: String): KMeans = this
}

// Simple implementation using the batch implementation:

class Word2VecModel() extends Transformer[Word2VecModel] {
  def copy(paramMap: ParamMap): Word2VecModel = ???
  def transform(df: DataFrame): DataFrame = ???
  def setModelType(s: String): Word2VecModel = ???
}

case class Word2Vec() extends BatchEstimator[Word2VecModel, Word2Vec] {
  def copy(paramMap: ParamMap) = ???
  override def fit(df: DataFrame): Word2VecModel = ??? // The existing implementation

}
