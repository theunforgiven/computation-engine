package com.cyrusinnovation.computation

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalamock.scalatest.MockFactory
import com.cyrusinnovation.computation.util.Log
import org.scalamock.{Mock, FunctionAdapter2}

class ComputationTests extends FlatSpec with ShouldMatchers with MockFactory with Mock {
  val noopLogger = mock[Log]
  val testRules = TestRules(noopLogger)

  implicit def toFunctionAdapter2[T1, T2, R](f: (T1, T2) => R) = {
    new FunctionAdapter2[T1, T2, R](f)
  }

  "A simple computation" should "apply the Scala expression to get the test entity with the maximum test value" in {
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))
    val newFacts = testRules.maxValueComputation.compute(facts)

    newFacts('maxTestValue) should be(Map('b -> 5))
  }

  "A simple computation" should "propagate exceptions when indicated" in {
    val facts: Map[Symbol, Any] = Map('maxTestValue -> Map('unused -> 3))
    evaluating {
      testRules.exceptionThrowingComputation(true).compute(facts)
    } should produce [Exception]
  }

  "A simple computation" should "not propagate exceptions otherwise" in {
    val facts: Map[Symbol, Any] = Map('maxTestValue -> Map('unused -> 10))
    testRules.exceptionThrowingComputation(false).compute(facts)
  }

  "A simple computation" should "just log if it fails to compile and exceptions are not propagated" in {
    val logger = stub[Log]
    testRules.computationWithSyntaxError(logger, false)

    (logger.error(_:String, _:Throwable)).verify{   // Uses implicit conversion above
      (msg: String, t: Throwable) => {
          msg == "Computation failed to compile" &&
          t.getClass == classOf[com.googlecode.scalascriptengine.CompilationError] &&
          t.getMessage.startsWith("2 error(s) occured")
      }
    }
  }
  "A simple computation" should "log and throw an exception if it fails to compile and exceptions are propagated" in {
    val logger = stub[Log]

    evaluating {
      testRules.computationWithSyntaxError(logger, true)
    } should produce[com.googlecode.scalascriptengine.CompilationError]

    (logger.error(_:String, _:Throwable)).verify{   // Uses implicit conversion above
      (msg: String, t: Throwable) => {
          msg == "Computation failed to compile" &&
          t.getClass == classOf[com.googlecode.scalascriptengine.CompilationError] &&
          t.getMessage.startsWith("2 error(s) occured")
      }
    }
  }

  "A simple computation" should "inactivate itself if it fails to compile and exceptions are not propagated" in {
    val facts: Map[Symbol, Any] = Map('maxTestValue -> Map('unused -> 10))

    val logger = stub[Log]
    val computation = testRules.computationWithSyntaxError(logger, false)

    computation.compute(facts)
    (logger.warn(_:String)).verify("Disabled computation called: test.computations.ExceptionThrowingComputation")
  }

  "When creating the function body string, the computation" should "construct Scala function code from the given expression" in {
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

  "When creating the function body string, the computation" should "construct Scala function code with multiple values in the input map" in {
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

  "When creating the function body string, the computation" should "not throw exceptions with an empty input map" in {
    val expression = "Some(a,b)"
    val inputMap: Map[String, Symbol] = Map()
    val outputMap = 'A

    val expectedFunctionString =
      """if() Map() else {
            (Some(a,b) : Option[Any]) match {
              case Some(value) => Map('A -> value)
              case None => Map()
            }
         }"""

    val actualOutput = SimpleComputation.createFunctionBody(expression, inputMap, outputMap)

    normalizeSpace(actualOutput) should be(normalizeSpace(expectedFunctionString))
  }

  "When creating the function body string, the computation" should "not throw exceptions with a null input map" in {
    val expression = "Some(a,b)"
    val inputMap: Map[String, Symbol] = null
    val outputMap = 'A

    val expectedFunctionString =
      """if() Map() else {
            (Some(a,b) : Option[Any]) match {
              case Some(value) => Map('A -> value)
              case None => Map()
            }
         }"""

    val actualOutput = SimpleComputation.createFunctionBody(expression, inputMap, outputMap)

    normalizeSpace(actualOutput) should be(normalizeSpace(expectedFunctionString))
  }

  "When creating the function body string, the computation" should "not throw exceptions with an empty expression" in {
    val expression = ""
    val inputMap = Map("a: Int" -> 'foo, "b: String" -> 'bar)
    val outputMap = 'A

    val expectedFunctionString =
      """if(domainFacts.get('foo).isEmpty || domainFacts.get('bar).isEmpty) Map() else {
            val a: Int = domainFacts.get('foo).get.asInstanceOf[Int]
            val b: String = domainFacts.get('bar).get.asInstanceOf[String]
            ( : Option[Any]) match {
              case Some(value) => Map('A -> value)
              case None => Map()
            }
         }"""

    val actualOutput = SimpleComputation.createFunctionBody(expression, inputMap, outputMap)

    normalizeSpace(actualOutput) should be(normalizeSpace(expectedFunctionString))
  }

  def normalizeSpace(str: String) = str.replaceAll("""\s+""", " ")
}