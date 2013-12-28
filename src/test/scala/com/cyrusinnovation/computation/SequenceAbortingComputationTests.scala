package com.cyrusinnovation.computation

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class SequenceAbortingComputationTests extends FlatSpec with ShouldMatchers {

  "A sequence aborting computation" should "be able to abort a sequential computation mid-sequence if there is a return value" in {
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))

    val sequentialComputation = new SequentialComputation(List(AbortIfHasResults(TestRules.maxValueComputation),
                                                               TestRules.exceptionThrowingComputation(false)))
    val newFacts = sequentialComputation.compute(facts)
    newFacts('maxTestValue) should be(Map('b -> 5))
  }

  "A sequence aborting computation" should "be able to abort a sequential computation mid-sequence if there is no return value" in {
    val facts: Map[Symbol, Any] = Map()

    val sequentialComputation = new SequentialComputation(List(AbortIfNoResults(TestRules.maxValueComputation),
                                                               TestRules.exceptionThrowingComputation(false)))
    val newFacts = sequentialComputation.compute(facts)
    newFacts should be(Map())
  }
}
