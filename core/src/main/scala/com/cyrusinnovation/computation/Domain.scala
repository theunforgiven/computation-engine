package com.cyrusinnovation.computation
/*
 * Copyright 2014 Cyrus Innovation, LLC. Licensed under Apache license 2.0.
 */


/** Contains the facts to be operated on in a computation as well as metadata indicating
 * whether the caller should continue or not (e.g., if the caller is a sequence of
 * computations).
 */
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
