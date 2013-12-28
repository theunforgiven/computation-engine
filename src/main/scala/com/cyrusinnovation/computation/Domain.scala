package com.cyrusinnovation.computation

case class Domain(facts: Map[Symbol, Any], continue: Boolean)

object Domain {
  def combine(newFacts: Map[Symbol, Any], originalDomain: Domain) : Domain = {
    val previousFacts = originalDomain.facts

    val resultingFacts: Map[Symbol, Any] = newFacts.foldLeft(previousFacts) {
      (factsSoFar: Map[Symbol, Any], factKeyToFactDataStructure: (Symbol, Any)) => {
        val factKey = factKeyToFactDataStructure._1
        val values = factKeyToFactDataStructure._2
        factsSoFar + (factKey -> values)
      }
    }
    new Domain(resultingFacts, originalDomain.continue)
  }
}
