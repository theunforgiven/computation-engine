package com.cyrusinnovation.computation

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

class SequentialComputation(val steps: List[Computation]) extends CompoundComputation {
  def resultKey = steps.last.resultKey
}

