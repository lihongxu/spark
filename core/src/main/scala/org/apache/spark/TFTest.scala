package org.apache.spark

/**
 * Created by tjhunter on 12/3/15.
 */
object TFTest {
  val sc: SparkContext = null




  import org.apache.spark.scheduler._

  val tfl = sc.tfListener
  tfl.init()

  val rdd1 = sc.parallelize(1 to 100).map(_.toFloat)
  rdd1.groupBy(_.hashCode() % 10).mapValues(_.size).collect()
  rdd1.count()
  val b = new GraphBuilder()
  b.addGraph(tfl.rddops.getAllOperationGraphs, scope="repl")
  println(b.build())
  tfl.writeGraph(b.build())
  tfl.stream1.flush()



}


