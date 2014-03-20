package com.cyrusinnovation.computation
/*
 * Copyright 2014 Cyrus Innovation, LLC. Licensed under Apache license 2.0.
 */

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import com.cyrusinnovation.computation.util.Log
import org.scalamock.scalatest.MockFactory
import org.scalamock.FunctionAdapter2

class AbortingComputationTests extends FlatSpec with Matchers with MockFactory {

  "An aborting computation" should "be able to abort a sequential computation mid-sequence if there is a return value" in {
    val testRules = TestRules(stub[Log])    //Can't stub inside a BeforeEach

    val domain = Domain(Map('testValues -> Map('a -> 2, 'b -> 5)), continue = true)

    val sequentialComputation = new SequentialComputation(List(AbortIfHasResults(testRules.maxValueComputation),
                                                               testRules.exceptionThrowingSimpleComputation(false)))
    val newDomain = sequentialComputation.compute(domain)
    newDomain.facts('maxTestValue) should be(Map('b -> 5))
    newDomain.continue should be(true)  // Domain coming out of a sequence that is aborted should not have continue set to false
  }

  "An aborting computation" should "be able to abort a sequential computation mid-sequence if there is no return value" in {
    val testRules = TestRules(stub[Log])

    val domain = Domain(Map('testValues -> Map('a -> 2, 'b -> 5)), continue = true)

    val sequentialComputation = new SequentialComputation(List(AbortIfNoResults(testRules.noResultsComputation),
                                                               testRules.exceptionThrowingSimpleComputation(false)))
    val newDomain = sequentialComputation.compute(domain)
    newDomain.facts should be(Map('testValues -> Map('a -> 2, 'b -> 5)))   // SequentialComputation combines results of each computation with previous domain
    newDomain.continue should be(true)
  }

  "An aborting computation" should "be able to abort a sequential computation mid-sequence based on a specific condition" in {
    val stubLogger = stub[Log]
    val testRules = TestRules(stubLogger)

    val domain = Domain(Map('testValues -> Map('a -> 2, 'b -> 5)), continue = true)

    val sequentialComputation = new SequentialComputation(List(AbortIf("test.computations",
                                                                       "AbortIfContainsMapWithDesiredEntry",
                                                                       "See if the value is a map with one key 'b and value 5",
                                                                       List("scala.collection.mutable.{Map => MutableMap}"),
                                                                       """x == MutableMap('b -> 5)""",
                                                                       Map("x: MutableMap[Symbol, Int]" -> testRules.maxValueComputation.resultKey),
                                                                       testRules.maxValueComputation,
                                                                       TestSecurityConfiguration,
                                                                       stubLogger,
                                                                       shouldPropagateExceptions = true),
                                                               testRules.exceptionThrowingSimpleComputation(true)))

    val newDomain = sequentialComputation.compute(domain) //Should not throw exception
    newDomain.continue should be(true)
  }

  implicit def toFunctionAdapter2[T1, T2, R](f: (T1, T2) => R) = {
    new FunctionAdapter2[T1, T2, R](f)
  }
  
  "An AbortIf" should "log and throw an exception if it fails to compile and exceptions are propagated" in {
    val logger = stub[Log]
    val testRules = TestRules(logger)

    evaluating {
      testRules.abortIfComputationWithSyntaxError(shouldPropagate = true)
    } should produce[com.googlecode.scalascriptengine.CompilationError]

    (logger.error(_:String, _:Throwable)).verify {    // Uses implicit conversion above
      (msg: String, t: Throwable) => {
          msg == "Computation failed to compile" &&
          t.getClass == classOf[com.googlecode.scalascriptengine.CompilationError] &&
          t.getMessage.startsWith("2 error(s) occured")
      }
    }
  }

  "An AbortIf" should "log but not throw an exception if it fails to compile and exceptions are not propagated" in {
    val logger = stub[Log]
    val testRules = TestRules(logger)

    testRules.abortIfComputationWithSyntaxError(shouldPropagate = false)

    (logger.error(_:String, _:Throwable)).verify {    // Uses implicit conversion above
      (msg: String, t: Throwable) => {
          msg == "Computation failed to compile" &&
          t.getClass == classOf[com.googlecode.scalascriptengine.CompilationError] &&
          t.getMessage.startsWith("2 error(s) occured")
      }
    }
  }

  "An AbortIf" should "always abort before calling any inner computation if it was not compiled successfully" in {
    val logger = stub[Log]
    val testRules = TestRules(logger)

    val facts: Map[Symbol, Any] = Map('maxTestValue -> Map('unused -> 10))

    val computation = testRules.abortIfComputationWithSyntaxError(shouldPropagate = false)

    computation.compute(facts)
    (logger.warn(_:String)).verify("Defective computation called: test.computations.AbortIfWithSyntaxError")
  }
  
  
  "On encountering an exception during computation, a simple computation" should "log and propagate exceptions when indicated" in {
    val logger = stub[Log]
    val testRules = TestRules(logger)

    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))

    val exceptionThrowingComputation = testRules.exceptionThrowingAbortIf(shouldPropagate = true)

    evaluating {
      exceptionThrowingComputation.compute(facts)
    } should produce [java.lang.RuntimeException]

    (logger.error(_:String, _:Throwable)).verify{
      (msg: String, t: Throwable) => {
          msg.startsWith("AbortIf threw exception when processing data: ") &&
          msg.contains(facts.toString)
          t.getClass == classOf[java.lang.RuntimeException] &&
          t.getMessage == "Boom"
      }
    }
  }

  "On encountering an exception during computation, a simple computation" should "log and abort the series but not not propagate exceptions otherwise" in {
    val logger = stub[Log]
    val testRules = TestRules(logger)

    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))

    testRules.exceptionThrowingAbortIf(shouldPropagate = false).compute(facts)

    (logger.error(_:String, _:Throwable)).verify{    // Uses implicit conversion above
      (msg: String, t: Throwable) => {
          msg.startsWith("AbortIf threw exception when processing data: ") &&
          msg.contains(facts.toString)
          t.getClass == classOf[java.lang.RuntimeException] &&
          t.getMessage == "Boom"
      }
    }
  }
}
