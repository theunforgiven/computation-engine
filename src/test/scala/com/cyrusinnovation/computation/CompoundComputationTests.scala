package com.cyrusinnovation.computation

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class CompoundComputationTests extends FlatSpec with ShouldMatchers{

  "A sequential computation" should "chain multiple computations together" in {

    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))

    val sequentialComputation = new SequentialComputation(List(TestRules.maxValueComputation, TestRules.negationComputation))
    val newFacts = sequentialComputation.compute(facts)

    newFacts('negTestValue) should be(-5)
  }

  "A sequential computation" should "propagate exceptions when specified" in {
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))
    val sequentialComputation = new SequentialComputation(List(TestRules.maxValueComputation,
                                                               TestRules.exceptionThrowingComputation(true)))

    evaluating {
      sequentialComputation.compute(facts)
    } should produce [Exception]
  }

  "A sequential computation" should "not propagate exceptions otherwise" in {
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))
    val sequentialComputation = new SequentialComputation(List(TestRules.maxValueComputation,
                                                               TestRules.exceptionThrowingComputation(false)))

    sequentialComputation.compute(facts)
  }

    //TODO Iterative and branching computations
}
