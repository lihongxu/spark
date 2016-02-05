package org.apache.spark.ml.spark_current

//import org.apache.spark.ml.spark_mutable.{Word2Vec, TransformerPipeline, EstimatorPipeline, NaiveBayes}

import org.apache.spark.ml.{PipelineStage, Pipeline}
import org.apache.spark.ml.classification.{NaiveBayesModel, NaiveBayes}
import org.apache.spark.ml.feature.Word2Vec
import org.apache.spark.sql.DataFrame

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by tjhunter on 2/4/16.
 */
object experiments {
  val training_input1: DataFrame = ???
  val training_input2: DataFrame = ???
  val eval_data: DataFrame = ???


  val est = new Word2Vec()
  val transformer: NaiveBayesModel = new NaiveBayes().setSmoothing(0.1).fit(???)

  // *** Simple use case for estimation ***
  val model = est.fit(training_input1)
  val out1 = model.transform(eval_data)
  val out2 = transformer.transform(out1)

  // *** Online update + changing parameters on the fly ***
  // More complex estimation: we are also checking what is happening.
  val fut = Future { est.fit(training_input1) }
  // Sadly, the estimator usually does a defensive copy of itself before calling fit on it, and it loses the changes
  // -> cannot access to the online update
  // We could change the internal implementation to store somewhere the current model, but we will need to make
  // big changes to implement stopFit().

  // ** Derive a new estimator without changing this one ***
  // Need to make a defensive copy
  {
    val new_est = est.copy(null)
    val model = new_est.setInputCol("thatInput").fit(training_input2)
    val out = model.transform(eval_data)
  }

  // *** Train multiple models at once ***
  // Current implementation does a defensive copy internally
  {
    val fut1 = Future { est.fit(training_input1) }
    val fut2 = Future { est.setInputCol("").fit(training_input2) }
    // Parameter changes do not affect the current model
  }

  // *** Pipeline ***
  {
    // Fitting happens in place:
    val p: Pipeline = new Pipeline().setStages(Array(est, transformer))
    // Will not change est
    val mod = p.fit(training_input1)
    mod.transform(eval_data)
  }

  // *** Pipeline without refitting the elements already fit ***
  // Not sure how to do it currently
}
