package org.apache.spark.ml

import org.apache.spark.SparkContext

/**
 * Created by tjhunter on 12/3/15.
 */
object Test2 {
  val sc: SparkContext = null




  import org.apache.spark.ui.scope.RDDOperationGraph
  import org.apache.spark.scheduler._
  import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
  import org.apache.spark.mllib.linalg.Vectors

  val tfl = sc.tfListener
  tfl.init()

  val xs = (1 to 100).map(i => Vectors.dense(Array.fill(10)(scala.math.random)))
  val z = sc.parallelize(xs).cache()

  // Cluster the data into two classes using KMeans
  val numClusters = 2
  val numIterations = 20
  val clusters = KMeans.train(z, numClusters, numIterations)

  // Evaluate clustering by computing Within Set Sum of Squared Errors
  val WSSSE = clusters.computeCost(z)

  val b = new GraphBuilder(sc)
  val g = b.addGraph(tfl.rddops.getAllOperationGraphs)
  tfl.writeGraph(g)
  tfl.stream1.flush()


}
