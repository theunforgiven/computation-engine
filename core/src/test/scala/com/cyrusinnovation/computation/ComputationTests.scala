package com.cyrusinnovation.computation
/*
 * Copyright 2014 Cyrus Innovation, LLC. Licensed under Apache license 2.0.
 */

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalamock.scalatest.MockFactory
import com.cyrusinnovation.computation.util.Log
import org.scalamock.FunctionAdapter2

class ComputationTests extends FlatSpec with Matchers with MockFactory {
  implicit def toFunctionAdapter2[T1, T2, R](f: (T1, T2) => R) = {
    new FunctionAdapter2[T1, T2, R](f)
  }

  "A simple computation" should "apply the Scala expression to get the test entity with the maximum test value" in {
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))

    val maxValueComputation = new SimpleComputation("test.computations",
                                                    "MaximumTestValueComputation",
                                                    "Take the maximum of the values of the testValues map",
                                                    List("scala.collection.mutable.{Map => MutableMap}",
                                                         "scala.collection.mutable.{Set => MutableSet}"),
                                                    """val toTestImports = MutableSet()
                                                       val maxTuple = testValues.maxBy(aTuple => aTuple._2)
                                                       Some(MutableMap(maxTuple))
                                                    """,
                                                    Map("testValues: Map[String, Int]" -> 'testValues),
                                                    'maxTestValue,
                                                    TestSecurityConfiguration,
                                                    stub[Log],
                                                    shouldPropagateExceptions = true
                                                    )

    val newFacts = maxValueComputation.compute(facts)
    newFacts('maxTestValue) should be(Map('b -> 5))
  }

  "A simple computation" should "just log if it fails to compile and exceptions are not propagated" in {
    val logger = stub[Log]
    val testRules = TestRules(logger)

    testRules.simpleComputationWithSyntaxError(shouldPropagate = false)

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
    val testRules = TestRules(logger)

    evaluating {
      testRules.simpleComputationWithSyntaxError(shouldPropagate = true)
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
    val logger = stub[Log]
    val testRules = TestRules(logger)

    val facts: Map[Symbol, Any] = Map('maxTestValue -> Map('unused -> 10))

    val computation = testRules.simpleComputationWithSyntaxError(shouldPropagate = false)

    computation.compute(facts)
    (logger.warn(_:String)).verify("Disabled computation called: test.computations.ExceptionThrowingComputation")
  }

  "On encountering an exception during computation, a simple computation" should "log and propagate exceptions when indicated" in {
    val logger = stub[Log]
    val testRules = TestRules(logger)

    val facts: Map[Symbol, Any] = Map('maxTestValue -> Map('unused -> 3))

    val exceptionThrowingComputation: SimpleComputation = testRules.exceptionThrowingSimpleComputation(shouldPropagate = true)

    evaluating {
      exceptionThrowingComputation.compute(facts)
    } should produce [java.lang.RuntimeException]

    (logger.error(_:String, _:Throwable)).verify{
      (msg: String, t: Throwable) => {
          msg.startsWith("Computation threw exception when processing data: ") &&
          msg.contains(facts.toString)
          t.getClass == classOf[java.lang.RuntimeException] &&
          t.getMessage == "Boom"
      }
    }
  }

  "On encountering an exception during computation, a simple computation" should "log but not not propagate exceptions otherwise" in {
    val logger = stub[Log]
    val testRules = TestRules(logger)

    val facts: Map[Symbol, Any] = Map('maxTestValue -> Map('unused -> 10))

    testRules.exceptionThrowingSimpleComputation(shouldPropagate = false).compute(facts)

    (logger.error(_:String, _:Throwable)).verify{    // Uses implicit conversion above
      (msg: String, t: Throwable) => {
          msg.startsWith("Computation threw exception when processing data: ") &&
          msg.contains(facts.toString)
          t.getClass == classOf[java.lang.RuntimeException] &&
          t.getMessage == "Boom"
      }
    }
  }

  "A simple computation" should "not be able to use classes from packages that aren't whitelisted in the security configuration" in {
    val logger = stub[Log]
    val testRules = TestRules(logger)

    val facts: Map[Symbol, Any] = Map('input -> Map('unused -> 10))

    evaluating {
      testRules.whitelistViolatingComputation.compute(facts)
    } should produce[java.security.AccessControlException]
  }

  "A simple computation" should "not be able to use classes that are blacklisted in the security configuration" in {
    val logger = stub[Log]
    val testRules = TestRules(logger)

    val facts: Map[Symbol, Any] = Map('input -> Map('unused -> 10))

    evaluating {
      testRules.blacklistViolatingComputation.compute(facts)
    } should produce[java.security.AccessControlException]
  }

  "A simple computation" should "not be able to perform actions restricted by the Java security policy" in {
    val logger = stub[Log]
    val testRules = TestRules(logger)

    val facts: Map[Symbol, Any] = Map('input -> Map('unused -> 10))

    evaluating {
      testRules.javaPolicyViolatingComputation.compute(facts)
    } should produce[java.security.AccessControlException]
  }

  "When creating the function body string, the computation" should "construct Scala function code from the given expression" in {
    val expression = "Some(a)"
    val inputMap = Map("a: Int" -> 'foo)
    val outputMap = 'A

    val expectedFunctionString =
      """val a: Int = domainFacts.get('foo).get.asInstanceOf[Int]
        |( { Some(a) } : Option[Any]) match {
        |  case Some(value) => Map('A -> value)
        |  case None => Map()
        |}""".stripMargin

    val actualOutput = SimpleComputation.createFunctionBody(expression, inputMap, outputMap)

    normalizeSpace(actualOutput) should be(normalizeSpace(expectedFunctionString))
  }

  "When creating the function body string, the computation" should "construct Scala function code with multiple values in the input map" in {
    val expression = "Some(a,b)"
    val inputMap = Map("a: Int" -> 'foo, "b: String" -> 'bar)
    val outputMap = 'A

    val expectedFunctionString =
      """val a: Int = domainFacts.get('foo).get.asInstanceOf[Int]
        |val b: String = domainFacts.get('bar).get.asInstanceOf[String]
        |( { Some(a,b) } : Option[Any]) match {
        |  case Some(value) => Map('A -> value)
        |  case None => Map()
        |}""".stripMargin

    val actualOutput = SimpleComputation.createFunctionBody(expression, inputMap, outputMap)

    normalizeSpace(actualOutput) should be(normalizeSpace(expectedFunctionString))
  }

  "When creating the function body string, the computation" should "not throw exceptions with an empty input map" in {
    val expression = "Some(a,b)"
    val inputMap: Map[String, Symbol] = Map()
    val outputMap = 'A

    // Needs initial blank line
    val expectedFunctionString = """
        |( { Some(a,b) } : Option[Any]) match {
        |  case Some(value) => Map('A -> value)
        |  case None => Map()
        |}""".stripMargin

    val actualOutput = SimpleComputation.createFunctionBody(expression, inputMap, outputMap)

    normalizeSpace(actualOutput) should be(normalizeSpace(expectedFunctionString))
  }

  "When creating the function body string, the computation" should "not throw exceptions with a null input map" in {
    val expression = "Some(a,b)"
    val inputMap: Map[String, Symbol] = null
    val outputMap = 'A

    // Needs initial blank line
    val expectedFunctionString = """
        |( { Some(a,b) } : Option[Any]) match {
        |  case Some(value) => Map('A -> value)
        |  case None => Map()
        |}""".stripMargin

    val actualOutput = SimpleComputation.createFunctionBody(expression, inputMap, outputMap)

    normalizeSpace(actualOutput) should be(normalizeSpace(expectedFunctionString))
  }

  "When creating the function body string, the computation" should "not throw exceptions with an empty expression" in {
    val expression = ""
    val inputMap = Map("a: Int" -> 'foo, "b: String" -> 'bar)
    val outputMap = 'A

    val expectedFunctionString =
      """val a: Int = domainFacts.get('foo).get.asInstanceOf[Int]
        |val b: String = domainFacts.get('bar).get.asInstanceOf[String]
        |( { } : Option[Any]) match {
        |  case Some(value) => Map('A -> value)
        |  case None => Map()
        |}""".stripMargin

    val actualOutput = SimpleComputation.createFunctionBody(expression, inputMap, outputMap)

    normalizeSpace(actualOutput) should be(normalizeSpace(expectedFunctionString))
  }

  def normalizeSpace(str: String) = str.replaceAll("""\s+""", " ")
}