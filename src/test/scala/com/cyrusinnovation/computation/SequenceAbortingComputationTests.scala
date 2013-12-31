package com.cyrusinnovation.computation

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.cyrusinnovation.computation.util.Log
import org.scalamock.scalatest.MockFactory

class SequenceAbortingComputationTests extends FlatSpec with ShouldMatchers with MockFactory {
  val noopLogger = mock[Log]
  val testRules = TestRules(noopLogger)
  
  "A sequence aborting computation" should "be able to abort a sequential computation mid-sequence if there is a return value" in {
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))

    val sequentialComputation = new SequentialComputation(List(AbortIfHasResults(testRules.maxValueComputation),
                                                               testRules.exceptionThrowingComputation(false)))
    val newFacts = sequentialComputation.compute(facts)
    newFacts('maxTestValue) should be(Map('b -> 5))
  }

  "A sequence aborting computation" should "be able to abort a sequential computation mid-sequence if there is no return value" in {
    val facts: Map[Symbol, Any] = Map()

    val sequentialComputation = new SequentialComputation(List(AbortIfNoResults(testRules.maxValueComputation),
                                                               testRules.exceptionThrowingComputation(false)))
    val newFacts = sequentialComputation.compute(facts)
    newFacts should be(Map())
  }
}
