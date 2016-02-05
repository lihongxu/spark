package org.apache.spark.ml.spark_immutable

import org.apache.spark.sql.DataFrame

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

// *** Experiments ***

object Test {

  val training_input1: DataFrame = ???
  val training_input2: DataFrame = ???
  val eval_data: DataFrame = ???


  val est = KMeans()
  val transformer = new Word2VecModel().setModelType("Bernoulli")

  // *** Simple use case for estimation ***
  val model = est.fit(training_input1)
  val out1 = model.transform(eval_data)
  val out2 = transformer.transform(out1)

  // *** Online update + changing parameters on the fly ***
  // More complex estimation: we are also checking what is happening.
  val iFit = est.interactiveFit(training_input1)
  // Launch the computations in a separate thread
  val fut = Future { iFit.waitForCompletion() }
  // Hack and check what is happening...
  iFit.currentResult.transform(training_input1).first()

  // After a while we think we are done.
  iFit.stop()
  iFit.currentResult.transform(training_input1).first()

  // ** Derive a new estimator without changing this one ***
  {
    // `est` is not modified.
    val new_est = est.setInputCol("thatInput").fit(training_input2)
    val out = new_est.transform(eval_data)
  }

  // *** Train multiple models at once ***
  // Since immutable, it can perform fits or interactive fits in parallel. This is useful for training pipeline stages
  // in parallel.
  {
    val iFit1 = est.interactiveFit(training_input1)
    val iFit2 = est.interactiveFit(training_input2)
    val fut1 = Future { iFit1.waitForCompletion() }
    val fut2 = Future { iFit2.waitForCompletion() }
    // We can modify iFit1 and iFit2 and `est` independently
  }

  // *** Pipeline ***
  {
    // Pass a sequence of models and estimators
    val p: EstimatorPipeline = EstimatorPipeline(transformer, est)
    // Will not change
    val pModel = p.fit(training_input1)
    // Can also do interactive fits at the same time, or with different arguments
    val iFit1 = p.interactiveFit(training_input1)
    val pModel2 = p.copy(paramMap = ???).fit(training_input2)
  }

  // *** Pipeline without refitting the elements already fit ***
  {
    // By construction, we cannot try to use estimators within a pure transformer pipeline. This fails to compile:
    //val p: TransformerPipeline = TransformerPipeline(transformer, est)

    val trans2 = new Word2Vec()
    val bigPipeline: EstimatorPipeline = EstimatorPipeline(transformer, trans2)
    // This will fit est2 but not est
    val dfOut = bigPipeline.fit(training_input1)
  }}