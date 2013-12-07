package com.cyrusinnovation.computation

import clojure.lang.{IPersistentMap, Var}
import ClojureConversions._
import scala.collection.JavaConversions._

class Rule(namespace: String,
            name: String,
            ordering: Int,
            transformationExpression: String,
            shouldContinueIfThisRuleApplies: Boolean = true,
            shouldPropagateExceptions: Boolean = true) {

  private val clojureExpression = s"$transformationExpression"

  /* If you rebuild (clean build) this project in IntelliJ, scala compiles before clojure.
     This results in a compilation failure since Clojail isn't compiled when the scala compiles.
     Workaround: First, comment out the line below that references Clojail, and then uncomment
     the commented line below binding transformationFunction to ??? in order to make both the
     scala and the clojure compile. Then uncomment the Clojail line and recomment the line below. */
  // private val transformationFunction: Var = ???

  // TODO Put in try block and deactivate rule if compilation fails
  // TODO This sandbox isn't particularly safe - allows Compiler etc.
  private val transformationFunction = Clojail.safeEval(clojureExpression).asInstanceOf[Var]

  def apply(domain: Domain) : Domain = {
    // TODO Invoke in sandbox somehow? Right now a function definition can contain println and it works.
    // TODO Put in try block and deactivate rule if invocation fails
    val newFacts = transformationFunction.invoke(domain.facts).asInstanceOf[IPersistentMap]

    val continue = newFacts.count == 0 || shouldContinueIfThisRuleApplies
    combine(newFacts, domain, continue)
  }

  private def combine(newFacts: IPersistentMap, domain: Domain, continue: Boolean) : Domain = {
    val newFactMap: Map[Any, Any] = newFacts // By implicit conversion
    val currentFacts = domain.facts

    val resultingFacts = newFactMap.foldLeft(currentFacts) {
      (factsSoFar, factTypeToNewFactTable) => {
        val factType = factTypeToNewFactTable._1
        val valueTable = factTypeToNewFactTable._2

        if(factsSoFar.containsKey(factType)) combineInnerMaps(factsSoFar, factTypeToNewFactTable)
        else factsSoFar.assoc(factType, valueTable)
      }
    }
    new Domain(resultingFacts, continue)
  }

  def combineInnerMaps(previousFacts: IPersistentMap, newFactTypeToNewFactTable: (Any, Any)): IPersistentMap = {
    val currentInnerMap = previousFacts.valAt(newFactTypeToNewFactTable._1).asInstanceOf[IPersistentMap]
    val newInnerMap = newFactTypeToNewFactTable._2.asInstanceOf[java.util.Map[Any, Any]]

    val combinedValueMap = newInnerMap.foldLeft(currentInnerMap) {
      (resultsSoFar, newKeyValuePair) => resultsSoFar.assoc(newKeyValuePair._1, newKeyValuePair._2)
    }
    previousFacts.assoc(newFactTypeToNewFactTable._1, combinedValueMap)
  }
}

