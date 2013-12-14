package com.cyrusinnovation.computation

trait CompoundComputation extends Computation {
  def steps : List[Computation]

  def compute(domain: Domain): Domain = {
    steps.foldLeft(domain) {
      (domainSoFar: Domain, step: Computation) => {
        val newDomain = step.compute(domainSoFar)
        // TODO Log when not continuing
        if (!newDomain.continue) return newDomain else newDomain
      }
    }
  }
}

class SequentialComputation(val steps: List[Computation]) extends CompoundComputation

