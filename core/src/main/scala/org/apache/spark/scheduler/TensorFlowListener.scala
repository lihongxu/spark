package org.apache.spark.scheduler

import java.io.{FileOutputStream, DataOutputStream}
import java.nio.{ByteOrder, ByteBuffer}

import org.apache.spark.Logging
import tensorflow.EventOuterClass

class TensorFlowListener extends SparkListener with Logging {

  val file1 = "/Users/tjhunter/work/tensorflow_mount/logs/events.out.tfevents.1449086059.a"
//  val file2 = "/Users/tjhunter/work/tensorflow_mount/logs/events.out.tfevents.1449086059.b"

  lazy val stream1 = new DataOutputStream(new FileOutputStream(file1))
//  lazy val stream2 = new DataOutputStream(new FileOutputStream(file2))

  def init(): Unit = {
    val event1 = EventOuterClass.Event.newBuilder()
      .setWallTime(1449086059)
      .setFileVersion("brain.Event:1").build()
    val event2 = EventOuterClass.Event.newBuilder()
      .setWallTime(2.0)
      .setStep(1L).build()
    val events = Seq(event1, event2)
    def write(s: DataOutputStream, b: Array[Byte]): Unit = {
      val buffer = ByteBuffer.allocate(1000)
      buffer.order(ByteOrder.LITTLE_ENDIAN)
      buffer.putLong(b.length)
      buffer.putInt(0)
      buffer.put(b)
      buffer.putInt(0)
      val n = buffer.position()
      buffer.rewind()
      println(s"Buffer: n=$n l=${b.size}")
      val arr = Array.fill[Byte](n)(0.toByte)
      buffer.get(arr)
      s.write(arr)
    }
    events.foreach { e =>
      val b = e.toByteArray
      write(stream1, b)
//      e.writeDelimitedTo(stream1)
//      e.writeDelimitedTo(stream2)
    }
    stream1.flush()
//    stream2.flush()
  }
}
