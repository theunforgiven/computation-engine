package com.cyrusinnovation.computation

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import clojure.lang._
import ClojureConversions._

class ComputationTests extends FlatSpec with ShouldMatchers{

  "the computation" should "apply the clojure rule to get the test entity with the maximum test value" in {
    val rule = new Rule("test.rules", "maximumTestValueRule", 1,
      """
        (defn transformation ^clojure.lang.IPersistentMap [^clojure.lang.IPersistentMap domain-facts]
            (let [test-values (seq (domain-facts '(:testEntity :testValue)))]
              (if (not (empty? test-values))
                (-> (apply max-key (fn [x] (x 1)) test-values) ((fn [arr] { '(:testEntity :maxTestValue) (apply hash-map (flatten arr)) })))
                {})
          ))
      """,
      shouldContinueIfThisRuleApplies = true,
      shouldPropagateExceptions = true
    )
    val rules = List(rule)

    val key = entityValueKeyFor("testEntity", "testValue")
    val innerMap = toClojureMap(Map("1" -> 2, "2" -> 5))
    val facts: Map[Any, Any] = Map(key -> innerMap)

    val computation = new Computation(rules)
    val newFacts = computation.compute(facts)

    val resultKey = entityValueKeyFor("testEntity", "maxTestValue")
    val resultsMap = fromClojureMap(newFacts(resultKey).asInstanceOf[IPersistentMap])

    resultsMap should be(Map("2" -> 5))
  }
}