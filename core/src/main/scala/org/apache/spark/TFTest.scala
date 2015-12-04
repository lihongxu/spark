package org.apache.spark



/**
 * Created by tjhunter on 12/3/15.
 */
object TFTest {
  val sc: SparkContext = null




  import org.apache.spark.ui.scope.RDDOperationGraph
  import org.apache.spark.scheduler._

  val tfl = sc.tfListener
  tfl.init()


  val rdd1 = sc.parallelize(1 to 100).map(_.toFloat).cache()
  val rdd2 = sc.parallelize(1 to 100).map(_.toFloat).count()
  rdd1.groupBy(_.hashCode() % 10).mapValues(_.size).collect()
  rdd1.count()
  val b = new GraphBuilder(sc)
  val g = b.addGraph(tfl.rddops.getAllOperationGraphs)
  println(g)
  tfl.writeGraph(g)
  tfl.stream1.flush()


//  println(b.build())
//  tfl.writeGraph(b.build())
//  tfl.stream1.flush()

  RDDOperationGraph.makeDotFile(tfl.rddops.getOperationGraphForJob(0).head)



}


object TFTest2 {
  val sc: SparkContext = null




  import org.apache.spark.ui.scope.RDDOperationGraph
  import org.apache.spark.scheduler._

  val tfl = sc.tfListener
  tfl.init()

  val rdd1 = sc.scope("scope1") {
    sc.parallelize(1 to 100).map(_.toFloat).cache()
  }

  val rdd2 = sc.scope("scope2") {
    sc.scope("inner1")(rdd1.map(_ / 2).filter(_ > 3)).filter(_ <= 90)
  }
  rdd1.groupBy(_.hashCode() % 10).mapValues(_.size).collect()
  rdd2.count()
  val b = new GraphBuilder(sc)
  val g = b.addGraph(tfl.rddops.getAllOperationGraphs)
//  println(b.build())
  tfl.writeGraph(g)
  tfl.stream1.flush()

  RDDOperationGraph.makeDotFile(tfl.rddops.getOperationGraphForJob(0).head)



}
