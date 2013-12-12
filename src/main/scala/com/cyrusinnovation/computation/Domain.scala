package com.cyrusinnovation.computation

case class Domain(facts: Map[Any, Any], continue: Boolean)

object Domain {
  def combine(newFacts: Map[Any, Any], domain: Domain, continue: Boolean) : Domain = {
    val currentFacts = domain.facts

    val resultingFacts: Map[Any, Any] = newFacts.foldLeft(currentFacts) {
      (factsSoFar: Map[Any, Any], factKeyToFactDataStructure: (Any, Any)) => {
        val factKey = factKeyToFactDataStructure._1
        val values = factKeyToFactDataStructure._2
        factsSoFar + (factKey -> values)
      }
    }
    new Domain(resultingFacts, continue)
  }
}
