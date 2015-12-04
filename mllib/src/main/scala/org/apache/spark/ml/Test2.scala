package org.apache.spark.ml

import org.apache.spark.SparkContext

/**
 * Created by tjhunter on 12/3/15.
 */
object Test2 {
  val sc: SparkContext = null




  import org.apache.spark.mllib.regression.LabeledPoint
  import org.apache.spark.ui.scope.RDDOperationGraph
  import org.apache.spark.scheduler._
  import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
  import org.apache.spark.mllib.linalg.Vectors
  import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
  import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
  import org.apache.spark.mllib.util.MLUtils

  val tfl = sc.tfListener
  tfl.init()

  val xs = (1 to 10000).map(i => Vectors.dense(Array.fill(10)(scala.math.random)))
  val data = sc.parallelize(xs).cache()
  val labeled = data.map(z => {
    val x = if (scala.math.random > 0.5) 1.0 else 0.0
    LabeledPoint(x, z)
  })

  sc.scope("test 4") {

    // Cluster the data into two classes using KMeans
    val numClusters = 2
    val numIterations = 20
    val clusters = KMeans.train(data, numClusters, numIterations)

    // Evaluate clustering by computing Within Set Sum of Squared Errors
    val WSSSE = clusters.computeCost(data)

    val b = new GraphBuilder(sc)
    val g = b.addGraph(tfl.rddops.getAllOperationGraphs)
    tfl.writeGraph(g)
    tfl.stream1.flush()
  }

  sc.scope("example using SVMSGD") {
    val splits = labeled.randomSplit(Array(0.6, 0.4), seed = 11L)
    val training = splits(0).cache()
    val test = splits(1)

    // Run training algorithm to build the model
    val numIterations = 100
    val model = SVMWithSGD.train(training, numIterations)

    // Clear the default threshold.
    model.clearThreshold()

    sc.scope("evaluation phase") {
      // Compute raw scores on the test set.
      val scoreAndLabels = test.map { point =>
        val score = model.predict(point.features)
        (score, point.label)
      }

      // Get evaluation metrics.
      val metrics = new BinaryClassificationMetrics(scoreAndLabels)
      val auROC = metrics.areaUnderROC()

      val b = new GraphBuilder(sc)
      val g = b.addGraph(tfl.rddops.getAllOperationGraphs)
      tfl.writeGraph(g)
      tfl.stream1.flush()

    }

  }


}
