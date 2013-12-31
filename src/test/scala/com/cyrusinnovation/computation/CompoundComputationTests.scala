package com.cyrusinnovation.computation

import org.scalatest.{BeforeAndAfterEach, FlatSpec}
import org.scalatest.matchers.ShouldMatchers
import com.cyrusinnovation.computation.util.Log
import org.scalamock.scalatest.MockFactory

class CompoundComputationTests extends FlatSpec with ShouldMatchers with MockFactory with BeforeAndAfterEach {
  var noOpLogger = stub[Log]
  var testRules = TestRules(noOpLogger)

  override def beforeEach {   // Must reset before each test
    noOpLogger = stub[Log]
    testRules = TestRules(noOpLogger)
  }
  
  "A sequential computation" should "chain multiple computations together" in {

    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))

    val sequentialComputation = new SequentialComputation(List(testRules.maxValueComputation, testRules.negationComputation))
    val newFacts = sequentialComputation.compute(facts)

    newFacts('negTestValue) should be(-5)
  }

  "A sequential computation" should "propagate exceptions thrown by inner computation" in {
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5))
    val sequentialComputation = new SequentialComputation(List(testRules.maxValueComputation,
                                                               testRules.exceptionThrowingComputation(shouldPropagate = true)))

    evaluating {
      sequentialComputation.compute(facts)
    } should produce [Exception]
  }

  //TODO Iterative computations
}
