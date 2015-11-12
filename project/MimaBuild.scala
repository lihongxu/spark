/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._
import sbt.Keys.version

import scala.reflect.runtime.{universe => ru}
import java.net.URLClassLoader
import java.nio.file.Files
import java.io.{FileInputStream, File}
import java.util.zip.ZipInputStream
import scala.collection.mutable.HashSet

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.MissingClassProblem
import com.typesafe.tools.mima.core.MissingTypesProblem
import com.typesafe.tools.mima.core.ProblemFilters._
import com.typesafe.tools.mima.plugin.MimaKeys.{binaryIssueFilters, previousArtifact}
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings


object MimaBuild {

  private def getAssemblyFile: File = {
    val f = new File(".").getCanonicalFile
    new File(f, "assembly/target/scala-2.10/spark-assembly-1.6.0-SNAPSHOT-hadoop2.2.0.jar")
  }

  // Only the spark files, in sorted order.
  private def allAssemblyClassNames(f: File): Seq[String] = {
    var allClassNames: List[String] = Nil
    val s = new ZipInputStream(new FileInputStream(f))
    var e = s.getNextEntry
    while (e != null) {
      if (!e.isDirectory() && e.getName().endsWith(".class")) {
        val c = e.getName.replace('/', '.').dropRight(6) // Drop the .class suffix
        if (c.startsWith("org.apache.spark")) {
          allClassNames ::= c
        }
      }
      e = s.getNextEntry
    }
    allClassNames.sorted
  }

  // Reflection is not thread-safe in scala 2.10
  private def packagePrivate(f: File): Seq[String] = this.synchronized {
    val classLoader = new URLClassLoader(Array(f.toURI.toURL), classOf[String].getClassLoader)
    val m = ru.runtimeMirror(classLoader)

    val allClassNames = allAssemblyClassNames(f)
    val ppSet: HashSet[String] = HashSet.empty

    for (className <- allClassNames) {
      val idx = className.indexOf("$")
      val refClassName = if (idx >= 0) { className.take(idx) } else { className }
      if (idx < 0) {
        // Access the reflection information
        try {
          val clazz = m.staticClass(refClassName)
          if (clazz.privateWithin.fullName != "<none>") {
            ppSet.add(className)
          }
        } catch {
          case _: UnsupportedOperationException =>
          case _: AssertionError =>
          case _: ArrayIndexOutOfBoundsException =>
        }
      }
      if (ppSet.contains(refClassName)) {
        ppSet.add(className)
      }
    }
    println(s"packagePrivate: ${allClassNames.size} classes, ${ppSet.size} package private")
    ppSet.toSeq.sorted
  }

  lazy val filterPackagePrivate = {
    val ass = getAssemblyFile
    println(s"filterPackagePrivate: $ass")
    val pp = packagePrivate(ass)
    pp.flatMap(excludeClass)
  }

//  val f = new File(".").getCanonicalFile
//  val f2 = new File(f, "assembly/target/scala-2.10/spark-assembly-1.6.0-SNAPSHOT-hadoop2.2.0.jar")
//  val classLoader = new URLClassLoader(Array(f2.toURI.toURL), getClass.getClassLoader)
//  val m = ru.runtimeMirror(classLoader)
//
//  var allClassNames: List[String] = Nil
//  val s = new ZipInputStream(new FileInputStream(f2))
//  var e = s.getNextEntry
//  while (e != null) {
//    if (!e.isDirectory() && e.getName().endsWith(".class")) {
//      val c = e.getName.replace('/', '.').dropRight(6) // Drop the .class suffix
//      if (c.startsWith("org.apache.spark")) {
//        allClassNames ::= c
//      }
//    }
//    e = s.getNextEntry
//  }
//  allClassNames = allClassNames.sorted
//
//  val ppSet: HashSet[String] = HashSet.empty
//
//  for (className <- allClassNames) {
//    val idx = className.indexOf("$")
//    val refClassName = if (idx >= 0) { className.take(idx) } else { className }
//    if (idx < 0) {
//      // Access the reflection information
//      val clazz = m.staticClass(refClassName)
//      if (clazz.privateWithin.fullName != "<none>") {
//        ppSet.add(className)
//      } else {
//        println(s"class $className is whitelisted")
//      }
//    }
//    if (ppSet.contains(refClassName)) {
//      ppSet.add(className)
//    }
//  }

  def excludeMember(fullName: String) = Seq(
      ProblemFilters.exclude[MissingMethodProblem](fullName),
      // Sometimes excluded methods have default arguments and
      // they are translated into public methods/fields($default$) in generated
      // bytecode. It is not possible to exhaustively list everything.
      // But this should be okay.
      ProblemFilters.exclude[MissingMethodProblem](fullName+"$default$2"),
      ProblemFilters.exclude[MissingMethodProblem](fullName+"$default$1"),
      ProblemFilters.exclude[MissingFieldProblem](fullName),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](fullName),
      ProblemFilters.exclude[IncompatibleMethTypeProblem](fullName),
      ProblemFilters.exclude[IncompatibleFieldTypeProblem](fullName)
    )

  // Exclude a single class and its corresponding object
  def excludeClass(className: String) = Seq(
      excludePackage(className),
      ProblemFilters.exclude[MissingClassProblem](className),
      ProblemFilters.exclude[MissingTypesProblem](className),
      excludePackage(className + "$"),
      ProblemFilters.exclude[MissingClassProblem](className + "$"),
      ProblemFilters.exclude[MissingTypesProblem](className + "$")
    )

  // Exclude a Spark class, that is in the package org.apache.spark
  def excludeSparkClass(className: String) = {
    excludeClass("org.apache.spark." + className)
  }

  // Exclude a Spark package, that is in the package org.apache.spark
  def excludeSparkPackage(packageName: String) = {
    excludePackage("org.apache.spark." + packageName)
  }

  def ignoredABIProblems(base: File, currentSparkVersion: String) = {

    // Excludes placed here will be used for all Spark versions
    val defaultExcludes = Seq()

    // Read package-private excludes from file
    val classExcludeFilePath = file(base.getAbsolutePath + "/.generated-mima-class-excludes")
    val memberExcludeFilePath = file(base.getAbsolutePath + "/.generated-mima-member-excludes")

    val ignoredClasses: Seq[String] =
      if (!classExcludeFilePath.exists()) {
        Seq()
      } else {
        IO.read(classExcludeFilePath).split("\n")
      }

    val ignoredMembers: Seq[String] =
      if (!memberExcludeFilePath.exists()) {
      Seq()
    } else {
      IO.read(memberExcludeFilePath).split("\n")
    }

    defaultExcludes ++ ignoredClasses.flatMap(excludeClass) ++
    ignoredMembers.flatMap(excludeMember) ++ MimaExcludes.excludes(currentSparkVersion)
  }

  def mimaSettings(sparkHome: File, projectRef: ProjectRef) = {
    val organization = "org.apache.spark"
    val previousSparkVersion = "1.5.0"
    val fullId = "spark-" + projectRef.project + "_2.10"
    mimaDefaultSettings ++
    Seq(previousArtifact := Some(organization % fullId % previousSparkVersion),
      binaryIssueFilters ++= ignoredABIProblems(sparkHome, version.value))
  }

}
