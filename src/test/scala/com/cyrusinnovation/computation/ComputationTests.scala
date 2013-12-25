package com.cyrusinnovation.computation

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class ComputationTests extends FlatSpec with ShouldMatchers{

  "A simple computation" should "apply the Scala expression to get the test entity with the maximum test value" in {
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))
    val newFacts = TestRules.maxValueComputation.compute(facts)

    newFacts('maxTestValue) should be(Map('b -> 5))
  }

  "A simple computation" should "propagate exceptions when indicated" in {
    val facts: Map[Symbol, Any] = Map('maxTestValue -> Map('unused -> 3))
    evaluating {
      TestRules.exceptionThrowingComputation(true).compute(facts)
    } should produce [Exception]
  }

  "A simple computation" should "not propagate exceptions otherwise" in {
    val facts: Map[Symbol, Any] = Map('maxTestValue -> Map('unused -> 10))
    TestRules.exceptionThrowingComputation(false).compute(facts)
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