package com.cyrusinnovation.computation

import ClojureConversions._
import clojure.lang.{ISeq, IPersistentMap}

trait CompoundComputation extends Computation {
  def steps : List[Computation]

  def compute(domain: Domain): Domain = {
    steps.foldLeft(domain) {
      (domainSoFar: Domain, step: Computation) => {
        val newDomain = step.compute(domainSoFar)
        if (!newDomain.continue) return newDomain else newDomain
      }
    }
  }
}

class SequentialComputation(val steps: List[Computation]) extends CompoundComputation

class IterativeComputation(val steps: List[Computation], ruleForExtractingSubdomains: Computation, keyForSubdomainSequence: Any, subcomputation: Computation) extends CompoundComputation {

  override def compute(facts: Map[Any, Any]) : Map[Any, Any] = {
    val domain = new Domain(facts, true)
    val subdomains = extractDomainsForSubcomputation(domain)
    val newDomain = applySubcomputations(domain, subdomains)
    val results = compute(newDomain)
    results.facts
  }

  private def extractDomainsForSubcomputation(domain: Domain): List[Domain] = {
    val domainIncludingSubdomains: Domain = ruleForExtractingSubdomains.compute(domain)
    val subdomainSequence = domainIncludingSubdomains.facts.valAt(keyForSubdomainSequence).asInstanceOf[ISeq]
    toList(subdomainSequence)
  }

  private def applySubcomputations(originalDomain: Domain, subdomains: List[Domain]): Domain = {
    subdomains.foldLeft(originalDomain) {
        (domainSoFar: Domain, subdomain: Domain) => {
          val newDomain = subcomputation.compute(subdomain)
          Domain.combine(newDomain.facts, domainSoFar, true) //TODO can't halt iterative computation early. Is that ok?
        }
    }
  }

  private def toList(subdomainSequence: ISeq) : List[Domain] = (subdomainSequence.first, subdomainSequence.next) match {
    case (x, null) => List(Domain(x.asInstanceOf[IPersistentMap], true))
    case (head, tail) => Domain(head.asInstanceOf[IPersistentMap], true) :: toList(tail)
    case _ => List()
  }
}
