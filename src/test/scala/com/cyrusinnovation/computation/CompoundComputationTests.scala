package com.cyrusinnovation.computation

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.cyrusinnovation.computation.util.Log
import org.scalamock.scalatest.MockFactory

class CompoundComputationTests extends FlatSpec with ShouldMatchers with MockFactory {
  val noopLogger = mock[Log]
  val testRules = TestRules(noopLogger)
  
  "A sequential computation" should "chain multiple computations together" in {

    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))

    val sequentialComputation = new SequentialComputation(List(testRules.maxValueComputation, testRules.negationComputation))
    val newFacts = sequentialComputation.compute(facts)

    newFacts('negTestValue) should be(-5)
  }

  "A sequential computation" should "propagate exceptions when specified" in {
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))
    val sequentialComputation = new SequentialComputation(List(testRules.maxValueComputation,
                                                               testRules.exceptionThrowingComputation(true)))

    evaluating {
      sequentialComputation.compute(facts)
    } should produce [Exception]
  }

  "A sequential computation" should "not propagate exceptions otherwise" in {
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))
    val sequentialComputation = new SequentialComputation(List(testRules.maxValueComputation,
                                                               testRules.exceptionThrowingComputation(false)))

    sequentialComputation.compute(facts)
  }
    //TODO Iterative and branching computations
}
