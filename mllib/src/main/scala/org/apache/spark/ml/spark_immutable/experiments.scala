package org.apache.spark.ml.spark_immutable

import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.DataFrame

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
  def currentResult: M
  def stop(): Unit = {}
  def update(params: ParamMap): Unit = {}
  // Some setters, etc.
}

abstract class BatchEstimator[
  M <: Transformer[M],
  E <: Estimator[M, InteractiveFit[M], E]] extends Estimator[M, InteractiveFit[M], E] {

  private class MyBatchFit(df: DataFrame) extends InteractiveFit[M] {
    override def currentResult = fit(df)
  }

  override def interactiveFit(df: DataFrame): InteractiveFit[M] = new MyBatchFit(df)
}



case class KMeansModel() extends Transformer[KMeansModel] {
  def copy(paramMap: ParamMap): KMeansModel = ???
  def transform(df: DataFrame): DataFrame = ???
}

case class KMeans() extends BatchEstimator[KMeansModel, KMeans] {
  def copy(paramMap: ParamMap) = ???
  def fit(df: DataFrame): KMeansModel = ???
}

object Test {
  val df: DataFrame = ???
  val m = KMeans().fit(df).transform(df)
}