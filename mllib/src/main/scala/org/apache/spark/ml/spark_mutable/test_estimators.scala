package org.apache.spark.ml.spark_mutable

import org.apache.spark.ml.Pipeline
import org.apache.spark.mllib.linalg.{Vectors, Vector}
import org.apache.spark.mllib.stat.Statistics
import org.apache.spark.sql.{Row, DataFrame}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.StructType

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class Word2Vec extends EstimatorWithState[Word2Vec.State, Word2Vec] {

  import Word2Vec.State
  val inputCol: String = ???
  val outputCol: String = ???

  def setInputCol(s: String): Word2Vec = this

  override protected def performSchemaTransform(schema: StructType): StructType = {
    schema
  }

  override protected def performTransform(df: DataFrame): DataFrame = {
    val State(min, max) = currentState
    // Use min and max...
    val reScale = udf { (vector: Vector) => vector }
    df.withColumn(outputCol, reScale(col(inputCol)))
  }

  override protected def initialState = State(Vectors.dense(0), Vectors.dense(1))

  override protected def performUpdate(currentState: State, data: DataFrame): StateUpdate = {
    val input = data.select(inputCol).map { case Row(v: Vector) => v }
    val summary = Statistics.colStats(input)
    DoneSuccess(State(summary.min, summary.max))
  }

}

object Word2Vec {
  case class State(min: Vector, max: Vector)
  def apply(): Word2Vec = new Word2Vec
}

object TestEstimators {
  def f1(): Unit = {
    val training_input1: DataFrame = ???
    val training_input2: DataFrame = ???
    val eval_data: DataFrame = ???


    val est = Word2Vec()
    val transformer = new NaiveBayes().setModelType("Bernoulli")

    // *** Simple use case for estimation ***
    est.fit(training_input1)
    val out1 = est.transform(eval_data)
    val out2 = transformer.transform(out1)

    // *** Online update + changing parameters on the fly ***
    // More complex estimation: we are also checking what is happening.
    val fut = Future { est.fit(training_input1) }
    // Hack and check what is happening...
    est.transform(training_input1).first()

    // After a while we think we are done.
    est.stopFit()
    // Can estimate again:
    est.transform(training_input1).first()

    // ** Derive a new estimator without changing this one ***
    // Need to make a defensive copy
    {
      val new_est = est.copy(null)
      new_est.setInputCol("thatInput").fit(training_input2)
      val out = new_est.transform(eval_data)
    }

    // *** Train multiple models at once ***
    // Need to make a defensive copy
    {
      val fut1 = Future { est.fit(training_input1) }
      // This triggers an error:
      est.fit(training_input2)
      // Need to make a copy:
      val fut2 = Future { est.copy(null).fit(training_input2) }
      // What happens when trying to train multiple models at once and trying to change the parameters for them?
    }

    // *** Pipeline ***
    {
      // Fitting happens in place:
      val p: EstimatorPipeline = EstimatorPipeline(transformer, est)
      // Will change est
      p.fit(training_input1)
    }

    // *** Pipeline without refitting the elements already fit ***
    {
      // Need to wrap the fitter within a transformer pipeline
      val p: TransformerPipeline = TransformerPipeline(transformer, est)
      val est2 = new NaiveBayes()
      val biggedP: EstimatorPipeline = EstimatorPipeline(p, est2)
      // This will fit est2 but not est
      biggedP.fit(training_input1)
    }
  }
}

//class KMeans extends EstimatorWithState[Int, KMeans]