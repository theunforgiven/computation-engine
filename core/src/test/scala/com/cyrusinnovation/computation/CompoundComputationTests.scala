package com.cyrusinnovation.computation
/*
 * Copyright 2014 Cyrus Innovation, LLC. Licensed under Apache license 2.0.
 */

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import com.cyrusinnovation.computation.util.Log
import org.scalamock.scalatest.MockFactory

class CompoundComputationTests extends FlatSpec with Matchers with MockFactory {

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
                                                               testRules.exceptionThrowingSimpleComputation(shouldPropagate = true)))

    evaluating {
      sequentialComputation.compute(facts)
    } should produce [java.lang.RuntimeException]
  }

  "An iterative computation" should "perform an inner computation once for each element in a specified sequence" in {
    val testRules = TestRules(stub[Log])    //Can't stub inside a BeforeEach
    val facts: Map[Symbol, Any] = Map('testValues -> List(2, 5, 7, 9))

    val iterativeComputation = new IterativeComputation(testRules.simpleNegationComputation,
                                                        ('testValues -> 'testValue),
                                                        'negatives)
    val newFacts = iterativeComputation.compute(facts)

    newFacts('negatives) should be(List(-2, -5, -7, -9))
  }

  "An iterative computation" should "be able to be aborted given an arbitrary condition" in {
    val stubLogger = stub[Log]
    val testRules = TestRules(stubLogger)
    val facts: Map[Symbol, Any] = Map('testValues -> List(2, 5, 7, 9))
    val domain = Domain(facts, true)

    val abortingComputation = AbortIf("test.computations",
                                      "AbortIfContainsMapWithDesiredEntry",
                                      "See if the value is -5",
                                      List(),
                                      "x == -5",
                                      Map("x: Int" -> testRules.simpleNegationComputation.resultKey),
                                      testRules.simpleNegationComputation,
                                      TestSecurityConfiguration,
                                      stubLogger,
                                      shouldPropagateExceptions = true)

    val iterativeComputation = new IterativeComputation(abortingComputation,
                                                        ('testValues -> 'testValue),
                                                        'negatives
    )
    val newDomain = iterativeComputation.compute(domain)

    newDomain.facts('negatives) should be(List(-2, -5))
    newDomain.continue should be(true)
  }

  "A mapping computation" should "return a map with the keys from the original map mapped to the results of applying the inner computation to the values" in {
    val testRules = TestRules(stub[Log])    //Can't stub inside a BeforeEach
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5, 'c -> 7, 'd -> 9))

    val iterativeComputation = new MappingComputation(testRules.simpleNegationComputation,
                                                      ('testValues -> 'testValue),
                                                      'negatives)

    val newFacts = iterativeComputation.compute(facts)

    newFacts('negatives) should be(Map('a -> -2, 'b -> -5, 'c -> -7, 'd -> -9))
  }
  
  

  "A mapping computation" should "be able to be aborted given an arbitrary condition" in {
    val stubLogger = stub[Log]
    val testRules = TestRules(stubLogger)
    val facts: Map[Symbol, Any] = Map('testValues -> Map('a -> 2, 'b -> 5, 'c -> 7, 'd -> 9))
    val domain = Domain(facts, true)

    val abortingComputation = AbortIf("test.computations",
                                      "AbortIfContainsMapWithDesiredEntry",
                                      "See if the value is -5",
                                      List(),
                                      "x == -5",
                                      Map("x: Int" -> testRules.simpleNegationComputation.resultKey),
                                      testRules.simpleNegationComputation,
                                      TestSecurityConfiguration,
                                      stubLogger,
                                      shouldPropagateExceptions = true)

    val mappingComputation = new MappingComputation(abortingComputation,
                                                    ('testValues -> 'testValue),
                                                    'negatives
    )
    val newDomain = mappingComputation.compute(domain)

    newDomain.facts('negatives) should be(Map('a -> -2, 'b -> -5))
    newDomain.continue should be(true)
  }
}
