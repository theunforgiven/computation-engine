package com.cyrusinnovation.computation

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.cyrusinnovation.computation.util.Log
import org.scalamock.scalatest.MockFactory

class CompoundComputationTests extends FlatSpec with ShouldMatchers with MockFactory {

  "A sequential computation" should "chain multiple computations together" in {
    val testRules = TestRules(stub[Log])    //Can't stub inside a BeforeEach
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))

    val sequentialComputation = new SequentialComputation(List(testRules.maxValueComputation, testRules.negationComputation))
    val newFacts = sequentialComputation.compute(facts)

    newFacts('negTestValue) should be(-5)
  }

  "A sequential computation" should "propagate exceptions thrown by inner computation" in {
    val testRules = TestRules(stub[Log])
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))
    val sequentialComputation = new SequentialComputation(List(testRules.maxValueComputation,
                                                               testRules.exceptionThrowingComputation(shouldPropagate = true)))

    evaluating {
      sequentialComputation.compute(facts)
    } should produce [java.lang.RuntimeException]
  }

  //TODO Iterative computations
}
