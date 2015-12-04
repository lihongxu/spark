package org.apache.spark.scheduler

import java.io.{FileOutputStream, DataOutputStream}
import java.nio.{ByteOrder, ByteBuffer}
import java.util.concurrent.atomic.AtomicLong

import com.google.protobuf.ByteString
import org.apache.spark.ui.scope.{RDDOperationNode, RDDOperationCluster, RDDOperationGraph, RDDOperationGraphListener}
import org.apache.spark.{SparkConf, SparkContext, Logging}
import tensorflow.Graph.{GraphDef, NodeDef}
import tensorflow._
import scala.collection.JavaConversions._
import scala.collection.mutable

class TensorFlowListener(conf: SparkConf) extends SparkListener with Logging {

  val rddops = new RDDOperationGraphListener(conf)

  val file1 = "/Users/tjhunter/work/tensorflow_mount/logs/events.out.tfevents.1449086059.a"
//  val file2 = "/Users/tjhunter/work/tensorflow_mount/logs/events.out.tfevents.1449086059.b"

  lazy val stream1 = new DataOutputStream(new FileOutputStream(file1))
//  lazy val stream2 = new DataOutputStream(new FileOutputStream(file2))

  val stepCount = new AtomicLong(0)

  def write(s: DataOutputStream, b: Array[Byte]): Unit = {
    val buffer = ByteBuffer.allocate(1000000)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    buffer.putLong(b.length)
    buffer.putInt(0)
    buffer.put(b)
    buffer.putInt(0)
    val n = buffer.position()
    buffer.rewind()
    val arr = Array.fill[Byte](n)(0.toByte)
    buffer.get(arr)
    s.write(arr)
  }

  def write(e: EventOuterClass.Event): Unit = {
    write(stream1, e.toByteArray)
  }

  def writeSummary(tag: String, value: Float): Unit = {
    TensorFlowListener.writeSummaries(this, tag->value)
  }

  def writeSummaries(pairs: (String, Float)*): Unit = {
    TensorFlowListener.writeSummaries(this, pairs:_*)
  }

  def writeGraph(g: GraphDef): Unit = {
    val t = (System.nanoTime() / 1000000).toDouble
    write(EventOuterClass.Event.newBuilder()
      .setWallTime(t)
      .setStep(stepCount.incrementAndGet())
      .setGraphDef(g)
      .build())
  }

  def init(): Unit = {
    // Initial event
    val event1 = EventOuterClass.Event.newBuilder()
      .setWallTime(0)
      .setFileVersion("brain.Event:1").build()
    write(event1)
    stream1.flush()
  }

  /**
   * Called when a stage completes successfully or fails, with information on the completed stage.
   */
  override def onStageCompleted(stageCompleted: SparkListenerStageCompleted): Unit = {
    println(s"onStageCompleted $stageCompleted")
    rddops.onStageCompleted(stageCompleted)
  }

  /**
   * Called when a stage is submitted
   */
  override def onStageSubmitted(stageSubmitted: SparkListenerStageSubmitted) {
    println(s"onStageSubmitted $stageSubmitted")
    rddops.onStageSubmitted(stageSubmitted)
  }

  /**
   * Called when a task starts
   */
  override def onTaskStart(taskStart: SparkListenerTaskStart) {
    logInfo(s"onTaskStart $taskStart")
    rddops.onTaskStart(taskStart)
  }

  /**
   * Called when a task begins remotely fetching its result (will not be called for tasks that do
   * not need to fetch the result remotely).
   */
  override def onTaskGettingResult(taskGettingResult: SparkListenerTaskGettingResult) {
    logInfo(s"onTaskGettingResult $taskGettingResult")
    rddops.onTaskGettingResult(taskGettingResult)
  }

  /**
   * Called when a task ends
   */
  override def onTaskEnd(taskEnd: SparkListenerTaskEnd) {
    logInfo(s"onTaskEnd $taskEnd")
    rddops.onTaskEnd(taskEnd)
  }

  /**
   * Called when a job starts
   */
  override def onJobStart(jobStart: SparkListenerJobStart) {
    logInfo(s"jobStart: $jobStart")
    rddops.onJobStart(jobStart)
  }

  /**
   * Called when a job ends
   */
  override def onJobEnd(jobEnd: SparkListenerJobEnd) {
    logInfo(s"jobEnd $jobEnd")
    rddops.onJobEnd(jobEnd)
  }

  /**
   * Called when the application starts
   */
  override def onApplicationStart(applicationStart: SparkListenerApplicationStart) {
    logInfo(s"applicationStart $applicationStart")
    rddops.onApplicationStart(applicationStart)
  }

  /**
   * Called when the application ends
   */
  override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd) {
    logInfo(s"applicationEnd $applicationEnd")
    rddops.onApplicationEnd(applicationEnd)
  }

  /**
   * Called when the driver receives task metrics from an executor in a heartbeat.
   */
  override def onExecutorMetricsUpdate(executorMetricsUpdate: SparkListenerExecutorMetricsUpdate) {
    logInfo(s"")
    rddops.onExecutorMetricsUpdate(executorMetricsUpdate)
  }


  def test(): Unit = {
    val event2 = EventOuterClass.Event.newBuilder()
      .setWallTime(2.0)
      .setStep(1L).build()
    val g = Graph.GraphDef.newBuilder()
    val n = Graph.NodeDef.newBuilder()
    n.setName("/node")
    n.setOp("op_name")
    n.setDevice("/job:worker/cpu:*")
    g.addNode(n)
    val event3 = EventOuterClass.Event.newBuilder()
      .setWallTime(3.0)
      .setStep(2L)
      .setGraphDef(g).build()
    val events = Seq(event2, event3)
    events.foreach { e =>
      val b = e.toByteArray
      write(stream1, b)
      //      e.writeDelimitedTo(stream1)
      //      e.writeDelimitedTo(stream2)
    }
  }
}

class GraphBuilder() {
  val rddIds: mutable.HashMap[Int, String] = mutable.HashMap.empty
  val rddClusterIds: mutable.HashMap[String, String] = mutable.HashMap.empty
  val nodes: mutable.HashMap[String, NodeDef] = mutable.HashMap.empty
  val orderedNodes: mutable.ArrayBuffer[NodeDef] = mutable.ArrayBuffer.empty
  val rddIdOutgoingEdges: mutable.HashMap[Int, mutable.ArrayBuffer[Int]] = mutable.HashMap.empty
  val rddIdIncomingEdges: mutable.HashMap[Int, mutable.ArrayBuffer[Int]] = mutable.HashMap.empty

  val zero = addNode0(TensorFlowListener.node0("zero", "ZERO"))

  def addNode0(n: NodeDef): NodeDef = {
    nodes += n.getName -> n
    orderedNodes += n
    n
  }

  def addEdge(rddIdFrom: Int, rddIdTo: Int): Unit = {
    println(s"addEdge $rddIdFrom $rddIdTo")
    rddIdOutgoingEdges.getOrElseUpdate(rddIdFrom, mutable.ArrayBuffer.empty).add(rddIdTo)
    rddIdIncomingEdges.getOrElseUpdate(rddIdTo, mutable.ArrayBuffer.empty).add(rddIdFrom)
  }

  def addNode(node: RDDOperationNode, scope: String=""): NodeDef = {
    val p = s"${scope}/${node.name}"
    rddIds.put(node.id, p)
    val depRddIds = rddIdIncomingEdges.getOrElse(node.id, Nil)
    val depRddPaths = depRddIds.flatMap(rddIds.get)
    val deps = depRddPaths.flatMap(nodes.get)
//    println(s"addNode: ${node.id} -> $p [${depRddIds}] -> [${depRddPaths}] -> [${deps.map(_.getName)}]")
    addNode0(TensorFlowListener.node(p, "RDD", deps))
  }

  def addNode1(node: RDDOperationNode, opName: String, scope: String=""): NodeDef = {
    val p = s"${scope}/${opName}"
    rddIds.put(node.id, p)
    val depRddIds = rddIdIncomingEdges.getOrElse(node.id, Nil)
    val depRddPaths = depRddIds.flatMap(rddIds.get)
    val deps = depRddPaths.flatMap(nodes.get)
    //    println(s"addNode: ${node.id} -> $p [${depRddIds}] -> [${depRddPaths}] -> [${deps.map(_.getName)}]")
    addNode0(TensorFlowListener.node(p, "RDD", deps,
      "rdd_id" -> node.id,
      "type" -> node.name,
      "cached" -> node.cached,
      "callsite" -> node.callsite.shortForm))
  }

  def addCluster(cluster: RDDOperationCluster, scope: String = ""): Unit = {
    val p = s"$scope/${cluster.name}"
    rddClusterIds += cluster.id -> p
    // nodes first, then sub clusters
    println(s"addCluster $p")
//    addNode(TensorFlowListener.node0(p, "Stage"))
    cluster.childNodes.sortBy(_.id).foreach(addNode(_, p))
    cluster.childClusters.sortBy(_.id).foreach(addCluster(_, p))

  }

  def addStageCluster(cluster: RDDOperationCluster, scope: String = ""): Unit = {
    val p = s"$scope/${cluster.name}"
    rddClusterIds += cluster.id -> p
    // nodes first, then sub clusters
    println(s"addStageCluster $p")
    // Find all the RDDs that are relevant to this stage:
    val stageRDDs = cluster.childClusters
      .sortBy(_.id) // This is the operation id, it should still give an ordering
      .flatMap { c =>
      assert(c.childClusters.isEmpty, s"$p ${c.id}")
      val children = c.childNodes
      assert(children.size <= 1, s"$p ${c.id} $children")
      children.map { child =>
        c.name -> child
      }
    } .toIndexedSeq
    // Compact the representation: if some RDDs have already been created, just add a reference to them
    val createdRDDs = stageRDDs.flatMap { case (name, rddOp) =>
        if (rddIds.contains(rddOp.id)) {
          None
        } else {
          // Create a RDD node
          val n = addNode1(rddOp, name, p)
          Some(rddOp.id -> n)
        }
    }
    val allStageDeps = stageRDDs.flatMap { case (name, rddOp) =>
        rddIdIncomingEdges.getOrElse(rddOp.id, Nil)
    } .toSet
    val explainedStageDeps = createdRDDs.map(_._1).flatMap(rddIdIncomingEdges.getOrElse(_, Nil)).toSet
    val unexplainedStageDepIds = (allStageDeps -- explainedStageDeps).toSeq.sorted
    val unexplainedStageDeps = unexplainedStageDepIds.flatMap(rddIds.get).flatMap(nodes.get)
    if (unexplainedStageDeps.nonEmpty) {
      addNode0(TensorFlowListener.node(p, "Stage", unexplainedStageDeps))
    }
  }

  def addGraph(graphs: Map[Int, Seq[RDDOperationGraph]], scope:String=""): Unit = {
    addGraphs(graphs.toSeq.flatMap { case (jobId, seq) => seq.map(x => jobId -> x)}, scope)
  }

  def addGraphs(graphs: Seq[(Int, RDDOperationGraph)], scope:String="", depth: Int =0): Unit = {
    if (graphs.isEmpty) {
      return
    }
    // Find all the edges that we have treated
    val (a, b) = graphs.partition { case (jobId, g) =>
        g.incomingEdges.forall(e => rddIds.contains(e.fromId))
    }
    a.foreach { case (jobId, g) =>
      val s = s"$scope/job $jobId"
      g.edges.foreach(e => addEdge(e.fromId, e.toId))
      g.outgoingEdges.foreach(e => addEdge(e.fromId, e.toId))
      g.incomingEdges.foreach(e => addEdge(e.fromId, e.toId))
      addStageCluster(g.rootCluster, s)
    }
    // Limit the depth so that we make progress
    if (depth >= 20) {
      throw new Exception("" + graphs)
    }
    addGraphs(b, scope, depth+1)
  }

  def build(): GraphDef = {
    val g = Graph.GraphDef.newBuilder()
    g.addAllNode(orderedNodes)
    g.build()
  }
}

object TensorFlowListener {
  import tensorflow.{Graph, EventOuterClass, SummaryOuterClass}

  def writeSummaries(listener: TensorFlowListener, pairs: (String,Float)*): Unit = {
    val e = {
      val b = EventOuterClass.Event.newBuilder()
      val t = (System.nanoTime() / 1000000).toDouble
      b.setWallTime(t)
        .setStep(listener.stepCount.incrementAndGet())
      val sum = {
        val b = SummaryOuterClass.Summary.newBuilder()
        for ((tag, value) <- pairs) {
          val v = {
            val b = SummaryOuterClass.Summary.Value.newBuilder()

            b.setTag(tag)
            b.setSimpleValue(value)
            b.build()
          }
          b.addValue(v)
        }
        b.build()
      }
      b.setSummary(sum)
      b.build()
    }
    listener.write(e)
  }

  def node(n: String, op: String, deps: Seq[Graph.NodeDef], attrs: (String, Any)*): Graph.NodeDef = {
    val b = Graph.NodeDef.newBuilder()
    deps.map(_.getName).foreach(b.addInput)
    val ats = attrs.map { z =>
      val b = AttrValueOuterClass.AttrValue.newBuilder()
      z._2 match {
        case x: String =>
          b.setS(ByteString.copyFrom(x, "ASCII"))
        case x: Double =>
          b.setF(x.toFloat)
        case x: Float =>
          b.setF(x)
        case x: Int =>
          b.setI(x.toLong)
        case x: Long =>
          b.setI(x.toLong)
        case x: Boolean =>
          b.setB(x)
      }
      z._1 -> b.build()
    } .toMap
    b.putAllAttr(ats)
    .setOp(op)
    .setName(n)
    .build()
  }

  def node0(n: String, op: String, attrs: (String, Any)*) = node(n, op, Nil, attrs:_*)
  def node0(n: String, attrs: (String, Any)*) = node(n, "Constant", Nil, attrs:_*)


  def x(sc: SparkContext): Unit = {

    val tfl = sc.tfListener
    tfl.init()

    val e = {
      val b = EventOuterClass.Event.newBuilder()
      val t = (System.nanoTime() / 1000000).toDouble
      b.setWallTime(t)
        .setStep(tfl.stepCount.incrementAndGet())
      val sum = {
        val b = SummaryOuterClass.Summary.newBuilder()
        val v = {
          val b = SummaryOuterClass.Summary.Value.newBuilder()
          b.setTag("tag")
          b.setSimpleValue(0.1.toFloat)
          b.build()
        }
        b.addValue(v)
        b.build()
      }
      b.setSummary(sum)
      b.build()
    }
    tfl.write(e)
    tfl.stream1.flush()

    val e2 = {
      val t = (System.nanoTime() / 1000000).toDouble
      val n1 = node0("C")
      val n2 = node("D", "Mul", List(n1))
      val n3 = node("n3", "Mul", List(n1, n2))
      val n31 = node("n3/n31", "Mul", List(n1))
      val nodes = Seq(n1, n2, n3, n31)
//      val n = {
//        val attr = AttrValueOuterClass.AttrValue.newBuilder().setType(Types.DataType.DT_FLOAT).build()
//        Graph.NodeDef.newBuilder()
//        .setName("node")
//        .setOp("Constant")
//          .putAllAttr(Map("dtype"->attr))
//        .build()
//      }
//
//      val n20 = {
//        val attr = AttrValueOuterClass.AttrValue.newBuilder().setType(Types.DataType.DT_FLOAT).build()
//        Graph.NodeDef.newBuilder()
//          .setName("node2")
//          .setOp("MyBigOpEUEOU")
//          .addInput("node")
////          .putAllAttr(Map("dtype"->attr))
//          .build()
//      }

      val g = Graph.GraphDef.newBuilder().addAllNode(nodes).build()
      EventOuterClass.Event.newBuilder()
        .setWallTime(t)
        .setStep(tfl.stepCount.incrementAndGet())
        .setGraphDef(g)
        .build()
    }
    tfl.write(e2)
    tfl.stream1.flush()

    val rdd = sc.parallelize(1 to 10)

  }


}
