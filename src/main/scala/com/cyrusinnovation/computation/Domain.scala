package com.cyrusinnovation.computation

import clojure.lang.IPersistentMap
import ClojureConversions._
import scala.collection.JavaConversions._

case class Domain(facts: IPersistentMap, continue: Boolean)

object Domain {
  def combine(newFacts: IPersistentMap, domain: Domain, continue: Boolean) : Domain = {
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
