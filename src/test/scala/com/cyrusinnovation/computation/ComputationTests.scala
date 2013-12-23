package com.cyrusinnovation.computation

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class ComputationTests extends FlatSpec with ShouldMatchers{

  "The computation" should "apply the Scala computation to get the test entity with the maximum test value" in {
    val inputKey = 'testValues
    val resultKey = 'maxTestValue
    val step = new SimpleComputation( "test.computations",
                                      "MaximumTestValueComputation",
                                      "Take the maximum of the testValue attribute of the testEntity entity",
                                      List("scala.collection.mutable.{Map => MutableMap}", "scala.collection.mutable.{Set => MutableSet}"),
                                      """
                                        |{  val toTestImports = MutableSet()
                                        |   val maxTuple = testValues.maxBy(aTuple => aTuple._2)
                                        |   Some(MutableMap(maxTuple)) }
                                      """.stripMargin,
                                      Map("testValues: Map[String, Int]" -> 'testValues),
                                      'maxTestValue,
                                      shouldContinueIfThisComputationApplies = true,
                                      shouldPropagateExceptions = true
    )
    val steps = List(step)
    val computation = new SequentialComputation(steps)

    val innerMap = Map("1" -> 2, "2" -> 5)
    val facts: Map[Any, Any] = Map(inputKey -> innerMap)

    val newFacts = computation.compute(facts)

    newFacts(resultKey) should be(Map("2" -> 5))
  }

  "The computation" should "construct a Scala function from the given expression and the input and output maps" in {
    val expression = "Some(a)"
    val inputMap = Map("a: Int" -> 'foo)
    val outputMap = 'A

    val expectedFunctionString =
      """if(domainFacts.get('foo).isEmpty) Map() else {
            val a: Int = domainFacts.get('foo).get.asInstanceOf[Int]
            (Some(a) : Option[Any]) match {
              case Some(value) => Map('A -> value)
              case None => Map()
            }
         }"""

    val actualOutput = SimpleComputation.createFunctionBody(expression, inputMap, outputMap)

    normalizeSpace(actualOutput) should be(normalizeSpace(expectedFunctionString))
  }

  "The computation" should "correctly construct a Scala function with multiple values in the input map" in {
    val expression = "Some(a,b)"
    val inputMap = Map("a: Int" -> 'foo, "b: String" -> 'bar)
    val outputMap = 'A

    val expectedFunctionString =
      """if(domainFacts.get('foo).isEmpty || domainFacts.get('bar).isEmpty) Map() else {
            val a: Int = domainFacts.get('foo).get.asInstanceOf[Int]
            val b: String = domainFacts.get('bar).get.asInstanceOf[String]
            (Some(a,b) : Option[Any]) match {
              case Some(value) => Map('A -> value)
              case None => Map()
            }
         }"""

    val actualOutput = SimpleComputation.createFunctionBody(expression, inputMap, outputMap)

    normalizeSpace(actualOutput) should be(normalizeSpace(expectedFunctionString))
  }

  def normalizeSpace(str: String) = str.replaceAll("""\s+""", " ")
}