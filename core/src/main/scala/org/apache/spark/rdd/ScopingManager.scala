package org.apache.spark.rdd

import org.apache.spark.{SparkContext, Logging}
import scala.collection.mutable

class ScopingManager(sc: SparkContext) extends Logging {

  import ScopingManager._

  private val _scopes: mutable.HashMap[Int, String] = mutable.HashMap.empty

  private def curr() = Option(scopes.get()).getOrElse(Nil)

  def enterScope(s: String): Unit = synchronized {
    scopes.set(s :: curr())
  }

  def exitScope(): Unit = synchronized {
    curr() match {
      case Nil =>
      case x :: _ =>
        exitScope(x)
    }
  }

  def exitScope(s: String): Unit = synchronized {
    val c = curr()
    c match {
      case Nil =>
        val m = s"Asked to exit scope $s but current path is empty (${Thread.currentThread().getName}})"
        println(m)
        logError(m)
      case x :: r if x != s =>
        val m = s"Asked to exit scope $s but current path is '${x :: r}' (${Thread.currentThread().getName}})"
        println(m)
        logError(m)
        scopes.set(r)
      case _ :: r =>
        scopes.set(r)
    }
  }

  def scope[A](s: String)(f: => A): A = {
    enterScope(s)
    try {
      RDDOperationScope.withScope(sc, s, allowNesting = true, ignoreParent = false)({ f })
    } finally {
      exitScope(s)
    }
  }

  def currentScope(): String = {
    curr().reverse.mkString("/")
  }

  def currentFullScope(): String = {
    val scopeKey = SparkContext.RDD_SCOPE_KEY
    val oldScopeJson = sc.getLocalProperty(scopeKey)
    val oldScope = Option(oldScopeJson).map(RDDOperationScope.fromJson)
    oldScope.map(_.getAllScopes.map(_.name).mkString("/")).getOrElse("")
  }

  def registerRDD(rddId: Int): String = synchronized {
    _scopes.getOrElseUpdate(rddId, currentScope())
  }

  def getScope(rddId: Int): Option[String] = synchronized {
    _scopes.get(rddId)
  }
}

object ScopingManager {
  val scopes = new InheritableThreadLocal[List[String]]
}
