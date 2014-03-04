package com.cyrusinnovation.computation

import scala.collection.{MapLike, LinearSeqOptimized}

// TODO Scaladoc
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

// TODO Scaladoc
class IterativeComputation[+A, +SeqType <: LinearSeqOptimized[A, SeqType]](val inner: Computation,
                           inputMapping: (Symbol, Symbol),
                           val resultKey: Symbol) extends Computation {

  def compute(domain: Domain): Domain = {
    val input: Any = domain.facts.get(inputMapping._1).get
    val reversedResultSequence = computeResultSequence(input, domain)
    val resultSequence = reversedResultSequence.reverse
    Domain.combine(Map(resultKey -> resultSequence), domain)
  }

  def computeResultSequence(input: Any, originalDomain: Domain): List[Any] = {
    val inputSequence = input.asInstanceOf[LinearSeqOptimized[A, SeqType]]
    
    inputSequence.foldLeft(List[Any]()) {
      (resultsSoFar: List[Any], value: Any) => {
        val domainWithSingleValueAdded = Domain.combine(Map(inputMapping._2 -> value), originalDomain)
        val innerResults = inner.compute(domainWithSingleValueAdded)
        innerResults.facts.get(inner.resultKey) match {
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

// TODO Scaladoc, Main documentation
// TODO Make abortable
class MappingComputation[A, +B, +MapType <: MapLike[A, B, MapType] with Map[A, B]](val inner: Computation,
                           inputMapping: (Symbol, Symbol),
                           val resultKey: Symbol) extends Computation {

  def compute(domain: Domain): Domain = {
    val input: Any = domain.facts.get(inputMapping._1).get
    val resultMap = computeResultSequence(input, domain)
    Domain.combine(Map(resultKey -> resultMap), domain)
  }

  def computeResultSequence(input: Any, originalDomain: Domain): Map[A, Any] = {
    val inputSequence = input.asInstanceOf[MapLike[A, B, MapType]]

    inputSequence.foldLeft(Map[A, Any]()) {
      (resultsSoFar: Map[A, Any], keyValueTuple: (A, B)) => {
        val domainWithSingleValueAdded = Domain.combine(Map(inputMapping._2 -> keyValueTuple._2), originalDomain)

        val innerResults = inner.compute(domainWithSingleValueAdded)
        innerResults.facts.get(inner.resultKey) match {
          case Some(result) => {
            val theResults = resultsSoFar + (keyValueTuple._1 -> result)
            theResults
            //if(innerResults.continue) theResults else return theResults
          }
          case None => resultsSoFar
        }
      }
    }
  }
}
