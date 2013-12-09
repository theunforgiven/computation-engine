package com.cyrusinnovation.computation

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import clojure.lang._
import ClojureConversions._

class ComputationTests extends FlatSpec with ShouldMatchers{

  "the computation" should "apply the clojure rule to get the test entity with the maximum test value" in {
    val step = new SimpleComputation("test.rules", "maximumTestValueRule", 1,
      """
        (defn transformation ^clojure.lang.IPersistentMap [^clojure.lang.IPersistentMap domain-facts]
            (let [test-values (seq (domain-facts '(:testEntity :testValue)))]
              (if (not (empty? test-values))
                (-> (apply max-key (fn [x] (x 1)) test-values) ((fn [arr] { '(:testEntity :maxTestValue) (apply hash-map (flatten arr)) })))
                {})
          ))
      """,
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

  "an iterative computation" should "apply the clojure rule to get the test entity with the maximum test value" in {
    val step = new SimpleComputation("test.rules", "maximumTestValueRule", 1,
      """
        (defn transformation ^clojure.lang.IPersistentMap [^clojure.lang.IPersistentMap domain-facts]
            (let [test-values (seq (domain-facts '(:testEntity :testValue)))]
              (if (not (empty? test-values))
                (-> (apply max-key (fn [x] (x 1)) test-values) ((fn [arr] { '(:testEntity :maxTestValue) (apply hash-map (flatten arr)) })))
                {})
          ))
      """,
      shouldContinueIfThisComputationApplies = true,
      shouldPropagateExceptions = true
    )
    val steps = List(step)
    val simpleComputation = new SequentialComputation(steps)

    val extractSubdomainsRule = new SimpleComputation("test.rules", "groupTestValuesRule", 1,
      """
        (defn extract-test-value-subdomains ^clojure.lang.IPersistentMap [^clojure.lang.IPersistentMap domain-facts]
          (let [test-values (domain-facts '(:testEntity :testValue))
                groups (domain-facts '(:testEntity :testGroup))
                sequence-of-maps (map (fn [pair] (into {} pair))
                                    (vals (group-by (fn [keyValuePair] (groups (keyValuePair 0))) test-values)))]
            {:subdomains (map (fn [inner-map] {'(:testEntity :testValue) inner-map}) sequence-of-maps) }
          ))
      """,
      shouldContinueIfThisComputationApplies = true,
      shouldPropagateExceptions = true
    )
    val groupRule = new SimpleComputation("test.rules", "groupMaximumTestValuesRule", 1,
      """
        (defn group-max-values ^clojure.lang.IPersistentMap [^clojure.lang.IPersistentMap domain-facts]
          (let [max-values (domain-facts '(:testEntity :maxTestValue))
                groups (domain-facts '(:testEntity :testGroup))
        		    groups-to-max-values (reduce
        										            (fn [so-far key] (assoc so-far (groups key) (max-values key)))
        							                  {}
        										            (keys max-values))]
        	  (assoc domain-facts '(:testGroup :maxTestValue) groups-to-max-values)
        ))
      """,
      shouldContinueIfThisComputationApplies = true,
      shouldPropagateExceptions = true
    )

    val computation = new IterativeComputation(List(groupRule), extractSubdomainsRule, toClojureKeyword("subdomains"), simpleComputation)

    val keyForTestValues = entityValueKeyFor("testEntity", "testValue")
    val innerMapForTestValues = toClojureMap(Map("1" -> 2, "2" -> 5, "3" -> 7, "4" -> 9))
    val keyForTestGroups = entityValueKeyFor("testEntity", "testGroup")
    val innerMapForTestGroups = toClojureMap(Map("1" -> "a", "2" -> "a", "3" -> "b", "4" -> "b"))

    val facts: Map[Any, Any] = Map(keyForTestValues -> innerMapForTestValues, keyForTestGroups -> innerMapForTestGroups)

    val newFacts = computation.compute(facts)

    val resultKey = entityValueKeyFor("testGroup", "maxTestValue")
    val resultsMap = fromClojureMap(newFacts(resultKey).asInstanceOf[IPersistentMap])

    resultsMap should be(Map("a" -> 5, "b" -> 9))
  }
}