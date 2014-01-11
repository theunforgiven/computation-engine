package com.cyrusinnovation.computation

import scala.collection.LinearSeqOptimized

class SequentialComputation(val steps: List[Computation]) extends Computation {
  def resultKey = steps.last.resultKey

  def compute(domain: Domain): Domain = {
    steps.foldLeft(domain) {
      (domainSoFar: Domain, step: Computation) => {
        val newDomain = step.compute(domainSoFar)
        if (newDomain.continue) newDomain else return Domain(newDomain.facts, true)
      }
    }
  }
}

class IterativeComputation[+A,+Repr <: LinearSeqOptimized[A, Repr]](val inner: Computation,
                           inputMapping: (Symbol, Symbol),
                           outputMapping: (Symbol, Symbol)) extends Computation {
  def resultKey = outputMapping._2

  def compute(domain: Domain): Domain = {
    val input: Any = domain.facts.get(inputMapping._1).get
    val reversedResultSequence = computeResultSequence(input, domain)
    val resultSequence = reversedResultSequence.reverse
    Domain.combine(Map(resultKey -> resultSequence), domain)
  }

  def computeResultSequence(input: Any, originalDomain: Domain): List[Any] = {
    val inputSequence = input.asInstanceOf[LinearSeqOptimized[A, Repr]]
    
    inputSequence.foldLeft(List[Any]()) {
      (resultsSoFar: List[Any], value: Any) => {
        val domainWithSingleValueAdded = Domain.combine(Map(inputMapping._2 -> value), originalDomain)
        val innerResults = inner.compute(domainWithSingleValueAdded)
        innerResults.facts.get(outputMapping._1) match {
          case Some(result) => {
            val theResults = result :: resultsSoFar
            if(innerResults.continue) theResults else return theResults
          }
          case None => resultsSoFar
        }
      }
    }
  }
}

