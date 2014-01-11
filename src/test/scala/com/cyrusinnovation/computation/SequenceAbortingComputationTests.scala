package com.cyrusinnovation.computation

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.cyrusinnovation.computation.util.Log
import org.scalamock.scalatest.MockFactory

class SequenceAbortingComputationTests extends FlatSpec with ShouldMatchers with MockFactory {

  "A sequence aborting computation" should "be able to abort a sequential computation mid-sequence if there is a return value" in {
    val testRules = TestRules(stub[Log])    //Can't stub inside a BeforeEach

    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))
    val domain = Domain(facts, true)

    val sequentialComputation = new SequentialComputation(List(AbortIfHasResults(testRules.maxValueComputation),
                                                               testRules.exceptionThrowingComputation(false)))
    val newDomain = sequentialComputation.compute(domain)
    newDomain.facts('maxTestValue) should be(Map('b -> 5))
    newDomain.continue should be(true)  // Domain coming out of a sequence that is aborted should not have continue set to false
  }

  "A sequence aborting computation" should "be able to abort a sequential computation mid-sequence if there is no return value" in {
    val testRules = TestRules(stub[Log])

    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))
    val domain = Domain(facts, true)

    val sequentialComputation = new SequentialComputation(List(AbortIfNoResults(testRules.noResultsComputation),
                                                               testRules.exceptionThrowingComputation(false)))
    val newDomain = sequentialComputation.compute(domain)
    newDomain.facts should be(Map('testValues -> Map('a -> 2, 'b -> 5)))   // SequentialComputation combines results of each computation with previous domain
    newDomain.continue should be(true)
  }

  "A sequence aborting computation" should "be able to abort a sequential computation mid-sequence based on a specific condition" in {
    val stubLogger = stub[Log]
    val testRules = TestRules(stubLogger)

    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))
    val domain = Domain(facts, true)

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
                                                               testRules.exceptionThrowingComputation(true)))

    val newDomain = sequentialComputation.compute(domain) //Should not throw exception
    newDomain.continue should be(true)
  }
}
