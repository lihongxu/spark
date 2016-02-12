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

package org.apache.spark.sql.execution.stat

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

import org.apache.spark.Logging
import org.apache.spark.sql.{Column, DataFrame, Row}
import org.apache.spark.sql.catalyst.expressions.{Cast, GenericMutableRow}
import org.apache.spark.sql.catalyst.plans.logical.LocalRelation
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String

private[sql] object StatFunctions extends Logging {

  import QuantileSummaries.Stats

  /** Calculate the approximate quantile for the given column */
  def approxQuantile(
      df: DataFrame,
      col: String,
      quantile: Double,
      epsilon: Double = QuantileSummaries.defaultEpsilon): Double = {
    require(quantile >= 0.0 && quantile <= 1.0, "Quantile must be in the range of (0.0, 1.0).")
    val summaries = collectQuantileSummaries(df, col, epsilon)
    summaries.query(quantile)
  }

  def approxQuantiles(
      df: DataFrame,
      col: String,
      quantiles: Array[Double],
      epsilon: Double): Array[Double] = {
    require(quantiles.min >= 0.0 && quantiles.max <= 1.0, "Quantile must be in the range of (0.0, 1.0).")
    val summaries = collectQuantileSummaries(df, col, epsilon)
    quantiles.map(summaries.query)
  }

  /**
   * For efficiency, runs multiple quantile computations in a single pass.
   * @param df the dataframe
   * @param cols columns of the dataframe
   * @param quantiles target quantiles to compute
   * @param epsilon the precision to achieve
   * @return for each column, returns the requested approximations
   */
  def multipleApproxQuantiles(df: DataFrame, cols: Seq[String], quantiles: Seq[Double], epsilon: Double): Seq[Seq[Double]] = {
    val columns: Seq[Column] = cols.map { colName =>
      val field = df.schema(colName)
      require(field.dataType.isInstanceOf[NumericType],
        s"Quantile calculation for column $colName with data type ${field.dataType} is not supported.")
      Column(Cast(Column(colName).expr, DoubleType))
    }
    val emptySummaries = Array.fill(cols.size)(
      new QuantileSummaries(QuantileSummaries.defaultCompressThreshold, epsilon))

    def apply(summaries: Array[QuantileSummaries], row: Row): Array[QuantileSummaries] = {
      var i = 0
      while (i < summaries.length) {
        summaries(i) = summaries(i).insert(row.getDouble(i))
        i += 1
      }
      summaries
    }

    def merge(sum1: Array[QuantileSummaries], sum2: Array[QuantileSummaries]): Array[QuantileSummaries] = {
      sum1.zip(sum2).map { case (s1, s2) => s1.compress().merge(s2.compress()) }
    }
    val summaries = df.select(columns: _*).rdd.aggregate(emptySummaries)(apply, merge)

    summaries.map { summary => quantiles.map(summary.query) }
  }

  private def collectQuantileSummaries(
      df: DataFrame,
      col: String,
      epsilon: Double): QuantileSummaries = {
    val data = df.schema.fields.find(_.name == col)
    require(data.nonEmpty, s"Couldn't find column with name $col")
    require(data.get.dataType.isInstanceOf[NumericType], "Quantile calculation for column " +
        s"with dataType ${data.get.dataType} not supported.")

    val column = Column(Cast(Column(col).expr, DoubleType))
    df.select(column).rdd.aggregate(new QuantileSummaries(QuantileSummaries.defaultCompressThreshold, epsilon))(
      seqOp = (summaries, row) => {
        summaries.insert(row.getDouble(0))
      },
      combOp = (baseSummaries, other) => {
        baseSummaries.merge(other)
    })
  }

  /**
   * Helper class to compute approximate quantile summary.
   * This implementation is based on the algorithm proposed in the paper:
   * "Space-efficient Online Computation of Quantile Summaries" by Greenwald, Michael
   * and Khanna, Sanjeev. (http://dl.acm.org/citation.cfm?id=375670)
   * 
   */
  class QuantileSummaries(
      val compressThreshold: Int,
      val epsilon: Double,
      val sampled: ArrayBuffer[Stats] = ArrayBuffer.empty,
      // The count of the elements we have sampled
      private var count: Long = 0L,
      private val recentlySampled: ArrayBuffer[Double] = ArrayBuffer.empty) extends Serializable {

    // Instead of reinserting at each step, we maintain an internal buffer on which we
    // append the most recent samples. When this buffer is full, we sort it and merge it
    // with the current samples.
//    private val recentlySampled: ArrayBuffer[Double] = ArrayBuffer.empty

    private def getConstant(): Double = 2 * epsilon * count

    def insert(x: Double): QuantileSummaries = {
      recentlySampled.append(x)
      if (recentlySampled.size >= compressThreshold) {
        return compress()
      }
      return this
      var idx = sampled.indexWhere(_.value > x)
      if (idx == -1) {
        idx = sampled.size
      } 
      val delta = if (idx == 0 || idx == sampled.size) {
        0
      } else {
        math.floor(getConstant()).toInt
      } 
      val tuple = Stats(x, 1, delta)
      sampled.insert(idx, tuple)
      count += 1

      if (sampled.size > compressThreshold) {
        compress()
      }
      this
    }

    // Compresses the data. If the data got compressed, it is going to return a new object.
    def compress(): QuantileSummaries = {
      if (recentlySampled.isEmpty) {
        val mergeThreshold = 2 * epsilon * count
        val compressed = QuantileSummaries.compressImmut(sampled, mergeThreshold)
        if (compressed.size < sampled.size) {
          return new QuantileSummaries(
            compressThreshold, epsilon,
            sampled = compressed, count = count)
        } else {
          // Compression did not change the current samples
          return this
        }
      } else {
        // Always merge the current samples when asked to compress.
        // TODO(tjh) these are the wrong stats
        val newSamples = recentlySampled.sorted.map { d => Stats(d, 1, 0) } .toArray
        val merged = QuantileSummaries.mergeSamples(newSamples, sampled)
        val recentCount = recentlySampled.size
        val newCount = count + recentCount
        val mergeThreshold = 2 * epsilon * newCount
        val compressed = QuantileSummaries.compressImmut(merged, mergeThreshold)
        return new QuantileSummaries(
          compressThreshold, epsilon,
          sampled = compressed, count = count)
      }
      val mergeThreshold = 2 * epsilon * count
      val compressed = QuantileSummaries.compressImmut(sampled, mergeThreshold)
      sampled.clear()
      sampled.appendAll(compressed)
      return this
      var i = 0
      while (i < sampled.size - 1) {
        val sample1 = sampled(i)
        val sample2 = sampled(i + 1)
        if (sample1.g + sample2.g + sample2.delta < math.floor(getConstant())) {
          sampled.update(i + 1, Stats(sample2.value, sample1.g + sample2.g, sample2.delta))
          sampled.remove(i)
        }
        i += 1
      }
      ???
    }

    def printBuffer(buff: Seq[Stats]): String = {
      var rankMin = 0
      buff.map { s =>
        rankMin += s.g
//        (s.value, rankMin, rankMin + rankMin + s.delta, s.g, s.delta)
        s"${s.value}:$rankMin"
      }.mkString(" ")
    }

    def merge(other: QuantileSummaries): QuantileSummaries = {
      return mergeImmutable(other)
      if (other.count > 0 && count > 0) {
        other.sampled.foreach { sample =>
          val idx = sampled.indexWhere(s => s.value > sample.value)
          if (idx == 0) {
            val new_sampled = Stats(sampled(0).value, sampled(0).g, sampled(1).delta / 2)
            sampled.update(0, new_sampled)
            val new_sample = Stats(sample.value, sample.g, 0)
            sampled.insert(0, new_sample)
          } else if (idx == -1) {
            val new_sampled = Stats(sampled(sampled.size - 1).value, sampled(sampled.size - 1).g,
              (sampled(sampled.size - 2).delta * 2 * epsilon).toInt)
            sampled.update(sampled.size - 1, new_sampled)
            val new_sample = Stats(sample.value, sample.g, 0)
            sampled.insert(sampled.size, new_sample)
          } else {
            val new_sample = Stats(sample.value, sample.g, (sampled(idx - 1).delta + sampled(idx).delta) / 2)
            sampled.insert(idx, new_sample)
          }
        }
        count += other.count
        compress()
        this
      } else if (other.count > 0) {
        other
      } else {
        this
      }
    }

    def mergeImmutable(other: QuantileSummaries): QuantileSummaries = {
      if (other.count == 0) {
        this
      } else if (count == 0) {
        other
      } else {
        val res = QuantileSummaries.mergeSamples(sampled, other.sampled)
        val mergeThreshold = 2 * epsilon * count
        val comp = QuantileSummaries.compressImmut(res, mergeThreshold)
        new QuantileSummaries(other.compressThreshold, other.epsilon, comp, other.count + count)
      }
    }


    def query(quantile: Double): Double = {
      require(quantile >= 0 && quantile <= 1.0, "quantile should be in the range [0.0, 1.0]")

      if (quantile <= epsilon) {
        return sampled.head.value
      }

      if (quantile >= 1 - epsilon) {
        return sampled.last.value
      }

      // Target rank
      val rank = math.ceil(quantile * count).toInt
      val targetError = math.ceil(epsilon * count)
      println(s"query: quantile=$quantile, rank=$rank, targetError=$targetError")
      // Minimum rank at current sample
      var minRank = 0
      var i = 1
      while (i < sampled.size - 1) {
        val curSample = sampled(i)
        minRank += curSample.g
        val maxRank = minRank + curSample.delta
//        println(s"bracket $i: minRank=$minRank maxRank=$maxRank")
        if (maxRank - targetError <= rank && rank <= minRank + targetError) {
          return curSample.value
        }
        i += 1
      }
      sampled.last.value
    }
  }

  object QuantileSummaries {
    /**
     * The default value for the compression threshold.
     */
    val defaultCompressThreshold: Int = 1000

    /**
     * The default value for epsilon.
     */
    val defaultEpsilon: Double = 0.01

    /**
     * Statisttics from the Greenwald-Khanna paper.
     * @param value the sampled value
     * @param g the minimum rank jump from the previous value's minimum rank
     * @param delta the maximum span of the rank.
     */
    case class Stats(value: Double, g: Int, delta: Int)

    def mergeSamples(samples1: IndexedSeq[Stats], samples2: IndexedSeq[Stats]): ArrayBuffer[Stats] = {
      // We rely on the fact that they are ordered to efficiently interleave them.
      val thisSampled = samples1.toList
      val otherSampled = samples2.toList
      val res: ArrayBuffer[Stats] = ArrayBuffer.empty

      @tailrec
      def mergeCurrent(thisList: List[Stats], otherList: List[Stats]): Unit = (thisList, otherList) match {
        case (Nil, l) =>
          res.appendAll(l)
        case (l, Nil) =>
          res.appendAll(l)
        case (h1 :: t1, h2 :: t2) if h1.value > h2.value =>
          mergeCurrent(otherList, thisList)
        case (h1 :: t1, l) =>
          // We know that h1.value <= all values in l
          // TODO(thunterdb) do we need to adjust g and delta?
          res.append(h1)
          mergeCurrent(t1, l)
      }

      mergeCurrent(thisSampled, otherSampled)

      res
    }

    def compressImmut(currentSamples: IndexedSeq[Stats], mergeThreshold: Double): ArrayBuffer[Stats] = {
      val res: ArrayBuffer[Stats] = ArrayBuffer.empty
      // Start for the last element, which is always part of the set.
      // The head contains the current new head, that may be merged with the current element.
      var head = currentSamples.last
      var i = currentSamples.size - 2
      // Do not compress the last element
      while (i >= 1) {
        // The current sample:
        val sample1 = currentSamples(i)
        // Do we need to compress?
        if (sample1.g + head.g + head.delta < mergeThreshold) {
          // Do not insert yet, just merge the current element into the head.
          head = head.copy(g = head.g + sample1.g)
        } else {
          // Prepend the current head, and keep the current sample as target for merging.
          res.prepend(head)
          head = sample1
        }
        i -= 1
      }
      res.prepend(head)
      // If necessary, add the minimum element:
      res.prepend(currentSamples.head)
      //      println(s"compressImmut: threshold=$mergeThreshold, \nbefore=${printBuffer(currentSamples)}\nafter=${printBuffer(res)}")
      res
    }


  }

  /** Calculate the Pearson Correlation Coefficient for the given columns */
  private[sql] def pearsonCorrelation(df: DataFrame, cols: Seq[String]): Double = {
    val counts = collectStatisticalData(df, cols, "correlation")
    counts.Ck / math.sqrt(counts.MkX * counts.MkY)
  }

  /** Helper class to simplify tracking and merging counts. */
  private class CovarianceCounter extends Serializable {
    var xAvg = 0.0 // the mean of all examples seen so far in col1
    var yAvg = 0.0 // the mean of all examples seen so far in col2
    var Ck = 0.0 // the co-moment after k examples
    var MkX = 0.0 // sum of squares of differences from the (current) mean for col1
    var MkY = 0.0 // sum of squares of differences from the (current) mean for col2
    var count = 0L // count of observed examples
    // add an example to the calculation
    def add(x: Double, y: Double): this.type = {
      val deltaX = x - xAvg
      val deltaY = y - yAvg
      count += 1
      xAvg += deltaX / count
      yAvg += deltaY / count
      Ck += deltaX * (y - yAvg)
      MkX += deltaX * (x - xAvg)
      MkY += deltaY * (y - yAvg)
      this
    }
    // merge counters from other partitions. Formula can be found at:
    // http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
    def merge(other: CovarianceCounter): this.type = {
      if (other.count > 0) {
        val totalCount = count + other.count
        val deltaX = xAvg - other.xAvg
        val deltaY = yAvg - other.yAvg
        Ck += other.Ck + deltaX * deltaY * count / totalCount * other.count
        xAvg = (xAvg * count + other.xAvg * other.count) / totalCount
        yAvg = (yAvg * count + other.yAvg * other.count) / totalCount
        MkX += other.MkX + deltaX * deltaX * count / totalCount * other.count
        MkY += other.MkY + deltaY * deltaY * count / totalCount * other.count
        count = totalCount
      }
      this
    }
    // return the sample covariance for the observed examples
    def cov: Double = Ck / (count - 1)
  }

  private def collectStatisticalData(df: DataFrame, cols: Seq[String],
              functionName: String): CovarianceCounter = {
    require(cols.length == 2, s"Currently $functionName calculation is supported " +
      "between two columns.")
    cols.map(name => (name, df.schema.fields.find(_.name == name))).foreach { case (name, data) =>
      require(data.nonEmpty, s"Couldn't find column with name $name")
      require(data.get.dataType.isInstanceOf[NumericType], s"Currently $functionName calculation " +
        s"for columns with dataType ${data.get.dataType} not supported.")
    }
    val columns = cols.map(n => Column(Cast(Column(n).expr, DoubleType)))
    df.select(columns: _*).queryExecution.toRdd.aggregate(new CovarianceCounter)(
      seqOp = (counter, row) => {
        counter.add(row.getDouble(0), row.getDouble(1))
      },
      combOp = (baseCounter, other) => {
        baseCounter.merge(other)
    })
  }

  /**
   * Calculate the covariance of two numerical columns of a DataFrame.
   * @param df The DataFrame
   * @param cols the column names
   * @return the covariance of the two columns.
   */
  private[sql] def calculateCov(df: DataFrame, cols: Seq[String]): Double = {
    val counts = collectStatisticalData(df, cols, "covariance")
    counts.cov
  }

  /** Generate a table of frequencies for the elements of two columns. */
  private[sql] def crossTabulate(df: DataFrame, col1: String, col2: String): DataFrame = {
    val tableName = s"${col1}_$col2"
    val counts = df.groupBy(col1, col2).agg(count("*")).take(1e6.toInt)
    if (counts.length == 1e6.toInt) {
      logWarning("The maximum limit of 1e6 pairs have been collected, which may not be all of " +
        "the pairs. Please try reducing the amount of distinct items in your columns.")
    }
    def cleanElement(element: Any): String = {
      if (element == null) "null" else element.toString
    }
    // get the distinct values of column 2, so that we can make them the column names
    val distinctCol2: Map[Any, Int] =
      counts.map(e => cleanElement(e.get(1))).distinct.zipWithIndex.toMap
    val columnSize = distinctCol2.size
    require(columnSize < 1e4, s"The number of distinct values for $col2, can't " +
      s"exceed 1e4. Currently $columnSize")
    val table = counts.groupBy(_.get(0)).map { case (col1Item, rows) =>
      val countsRow = new GenericMutableRow(columnSize + 1)
      rows.foreach { (row: Row) =>
        // row.get(0) is column 1
        // row.get(1) is column 2
        // row.get(2) is the frequency
        val columnIndex = distinctCol2.get(cleanElement(row.get(1))).get
        countsRow.setLong(columnIndex + 1, row.getLong(2))
      }
      // the value of col1 is the first value, the rest are the counts
      countsRow.update(0, UTF8String.fromString(cleanElement(col1Item)))
      countsRow
    }.toSeq
    // Back ticks can't exist in DataFrame column names, therefore drop them. To be able to accept
    // special keywords and `.`, wrap the column names in ``.
    def cleanColumnName(name: String): String = {
      name.replace("`", "")
    }
    // In the map, the column names (._1) are not ordered by the index (._2). This was the bug in
    // SPARK-8681. We need to explicitly sort by the column index and assign the column names.
    val headerNames = distinctCol2.toSeq.sortBy(_._2).map { r =>
      StructField(cleanColumnName(r._1.toString), LongType)
    }
    val schema = StructType(StructField(tableName, StringType) +: headerNames)

    new DataFrame(df.sqlContext, LocalRelation(schema.toAttributes, table)).na.fill(0.0)
  }
}
