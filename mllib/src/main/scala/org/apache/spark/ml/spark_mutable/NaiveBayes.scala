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

package org.apache.spark.ml.spark_mutable

import org.apache.hadoop.fs.Path

import org.apache.spark.SparkException
import org.apache.spark.annotation.{Experimental, Since}
import org.apache.spark.ml.PredictorParams
import org.apache.spark.ml.param.{DoubleParam, Param, ParamMap, ParamValidators}
import org.apache.spark.ml.util._
import org.apache.spark.mllib.classification.{NaiveBayes => OldNaiveBayes}
import org.apache.spark.mllib.classification.{NaiveBayesModel => OldNaiveBayesModel}
import org.apache.spark.mllib.linalg._
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType

/**
 * Params for Naive Bayes Classifiers.
 */
private[ml] trait NaiveBayesParams extends PredictorParams {

  type BaseType
  /**
   * The smoothing parameter.
   * (default = 1.0).
   * @group param
   */
  final val smoothing: DoubleParam = new DoubleParam(this, "smoothing", "The smoothing parameter.",
    ParamValidators.gtEq(0))

  /** @group getParam */
  final def getSmoothing: Double = $(smoothing)


  /**
   * Set the smoothing parameter.
   * Default is 1.0.
   * @group setParam
   */
  @Since("1.5.0")
  def setSmoothing(value: Double): BaseType = set(smoothing, value).asInstanceOf[BaseType]
  setDefault(smoothing -> 1.0)


  /**
   * The model type which is a string (case-sensitive).
   * Supported options: "multinomial" and "bernoulli".
   * (default = multinomial)
   * @group param
   */
  final val modelType: Param[String] = new Param[String](this, "modelType", "The model type " +
    "which is a string (case-sensitive). Supported options: multinomial (default) and bernoulli.",
    ParamValidators.inArray[String](OldNaiveBayes.supportedModelTypes.toArray))

  /** @group getParam */
  final def getModelType: String = $(modelType)
}

class NaiveBayes @Since("1.5.0") (
    @Since("1.5.0") override val uid: String) extends Estimator[NaiveBayes]
  with NaiveBayesParams with DefaultParamsWritable {

  override type BaseType = NaiveBayes

  @Since("1.5.0")
  def this() = this(Identifiable.randomUID("nb"))

  /**
   * Set the model type using a string (case-sensitive).
   * Supported options: "multinomial" and "bernoulli".
   * Default is "multinomial"
   * @group setParam
   */
  @Since("1.5.0")
  def setModelType(value: String): this.type = set(modelType, value)
  setDefault(modelType -> OldNaiveBayes.Multinomial)

  @Since("1.5.0")
  override def copy(extra: ParamMap): NaiveBayes = defaultCopy(extra)

  override def fit(data: DataFrame): Unit = {}

  override def transform(d: DataFrame): DataFrame = ???

  override def transformSchema(schema: StructType): StructType = ???

  override def stopFit(): Unit = {}
}


