package org.apache.spark.scheduler

import java.io.{FileOutputStream, DataOutputStream}

import org.apache.spark.Logging
import tensorflow.EventOuterClass

class TensorFlowListener extends SparkListener with Logging {

  val file = "/Users/tjhunter/work/tensorflow_mount/logs/out"

  lazy val stream = new DataOutputStream(new FileOutputStream(file))

  def init(): Unit = {
    val event = EventOuterClass.Event.newBuilder().setWallTime(1.0).setStep(1L).build()
    stream.write(event.toByteArray)
    stream.flush()
  }
}
