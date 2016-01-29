package org.apache.spark.ml.spark2

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
    val df: DataFrame = ???
    val est = Word2Vec()
    // Set parameters...
    // Simple estimatiow, we are waiting for the answer.
    est.fit(df)

    // More complex estimation: we are also checking what is happening.
    val fut = Future { est.fit(df) }
    // Hack and check what is happening...
    est.transform(df).first()

    // After a while we think we are done.
    est.stopFit()
    // Can estimate again:
    est.transform(df).first()
  }
}

//class KMeans extends EstimatorWithState[Int, KMeans]