package com.cyrusinnovation.computation

import ClojureConversions._
import clojure.lang.{ISeq, IPersistentMap}

trait Computation {
  def rules : List[Rule]

  def compute(facts: Map[Any, Any]) : Map[Any, Any] = {
    val domain = new Domain(facts, true)
    val results = compute(domain)
    results.facts
  }

  def compute(domain: Domain): Domain = {
    rules.foldLeft(domain) {
      (domainSoFar: Domain, rule: Rule) => {
        val newDomain = rule(domainSoFar)
        if (!newDomain.continue) return newDomain else newDomain
      }
    }
  }
}

class SimpleComputation(val rules: List[Rule]) extends Computation

class IterativeComputation(val rules: List[Rule], ruleForExtractingSubdomains: Rule, keyForSubdomainSequence: Any, subcomputation: Computation) extends Computation {

  override def compute(facts: Map[Any, Any]) : Map[Any, Any] = {
    val domain = new Domain(facts, true)
    val subdomains = extractDomainsForSubcomputation(domain)
    val newDomain = applySubcomputations(domain, subdomains)
    val results = compute(newDomain)
    results.facts
  }

  private def extractDomainsForSubcomputation(domain: Domain): List[Domain] = {
    val domainIncludingSubdomains: Domain = ruleForExtractingSubdomains(domain)
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
