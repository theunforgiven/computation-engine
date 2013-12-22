package com.cyrusinnovation.computation

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import clojure.lang._
import ClojureConversions._

class ComputationTests extends FlatSpec with ShouldMatchers{

  "the computation" should "apply the clojure computation to get the test entity with the maximum test value" in {
    val step = new SimpleComputation("test.computations", 1, "MaximumTestValueComputation",
      "Take the maximum of the testValue attribute of the testEntity entity",
      "(->> test-values (seq) (apply max-key #(nth %1 1)) (flatten) (apply hash-map))",
      Map("test-values" -> """'(:testEntity :testValue)"""),
      Map("max-keys" -> """'(:testEntity :maxTestValue)"""),
      shouldContinueIfThisComputationApplies = true,
      shouldPropagateExceptions = true
    )
    val steps = List(step)
    val computation = new SequentialComputation(steps)

    val key = entityValueKeyFor("testEntity", "testValue")
    val innerMap = toClojureMap(Map("1" -> 2, "2" -> 5))
    val facts: Map[Any, Any] = Map(key -> innerMap)

    val newFacts = computation.compute(facts)

    val resultKey = entityValueKeyFor("testEntity", "maxTestValue")
    val resultsMap = fromClojureMap(newFacts(resultKey).asInstanceOf[IPersistentMap])

    resultsMap should be(Map("2" -> 5))
  }

  "the computation" should "construct a clojure expression from the given expression and the input and output maps" in {
    val expression = "(->> test-values (seq) (apply max-key #(nth %1 1)) (flatten) (apply hash-map))"
    val inputMap = Map("test-values" -> """'(:testEntity :testValue)""")
    val outputMap = Map("max-keys" -> """'(:testEntity :maxTestValue)""")

    val expectedFunctionString =
      """(fn ^clojure.lang.IPersistentMap [^clojure.lang.IPersistentMap domain-facts]
            (let [test-values (domain-facts '(:testEntity :testValue)) ]
               (if (empty? test-values)
                  {}
                  (let [max-keys (->> test-values (seq) (apply max-key #(nth %1 1)) (flatten) (apply hash-map))]
                    (hash-map '(:testEntity :maxTestValue) max-keys)))))"""

    val actualOutput = SimpleComputation.createFunctionBody(expression, inputMap, outputMap)

    normalizeSpace(actualOutput) should be(normalizeSpace(expectedFunctionString))
  }

  "the computation" should "correctly construct a clojure expression with multiple values in the input map" in {
    val expression = "(identity)"
    val inputMap = Map("a" -> ":foo", "b" -> ":bar")
    val outputMap = Map("result" -> ":A")

    val expectedFunctionString =
      """(fn ^clojure.lang.IPersistentMap [^clojure.lang.IPersistentMap domain-facts]
            (let [a (domain-facts :foo) b (domain-facts :bar) ]
               (if (and (empty? a) (empty? b) )
                  {}
                  (let [result (identity)]
                    (hash-map :A result)))))"""

    val actualOutput = SimpleComputation.createFunctionBody(expression, inputMap, outputMap)

    normalizeSpace(actualOutput) should be(normalizeSpace(expectedFunctionString))
  }

  def normalizeSpace(str: String) = str.replaceAll("""\s+""", " ")
}