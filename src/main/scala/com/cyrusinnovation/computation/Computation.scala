package com.cyrusinnovation.computation

import clojure.lang._
import ClojureConversions._

case class Domain(facts: IPersistentMap, continue: Boolean)

class Computation(rules: List[Rule]) {

  def compute(facts: Map[Any, Any]) : Map[Any, Any] = {
    val domain = new Domain(facts, true)
    val results = applyRules(domain)
    results.facts
  }

  private def applyRules(domain: Domain): Domain = {
    rules.foldLeft(domain) {
      (domainSoFar: Domain, rule: Rule) => {
        val newDomain = rule(domainSoFar)
        if (!newDomain.continue) return newDomain else newDomain
      }
    }
  }
}


