package com.cyrusinnovation.computation
/*
 * Copyright 2014 Cyrus Innovation, LLC. Licensed under Apache license 2.0.
 */

import scala.collection.{MapLike, LinearSeqOptimized}

/** A single computation created from a series of computations. The sequence of computations
  * aborts if the result of an inner computation (usually, an AbortingComputation) indicates
  * that it should not continue.
  *
  * This type of computation will always propagate exceptions up the stack.
  *
  * @constructor     Instantiate a SequentialComputation from a list of computations.
  * @param steps     A list of computations that make up the sequence.
  *
  */
class SequentialComputation(val steps: List[Computation]) extends Computation {
  def resultKey = steps.last.resultKey

  /** Takes a domain of facts and returns a new domain of facts made up of the original set of
    * facts plus the result. This method specifies the details of the computation and must be
    * implemented by classes that mix in this trait.
    *
    * @param domain      A `Domain` containing the facts to be operated on as well as
    *                    additional metadata.
    * @return            A new domain consisting of the original domain of facts plus
    *                    an entry whose key is `resultKey` and whose value is the result
    *                    of the computation. The metadata of the domain may also be different
    *                    from the metadata in the input domain.
    */
  def compute(domain: Domain): Domain = {
    steps.foldLeft(domain) {
      (domainSoFar: Domain, step: Computation) => {
        val newDomain = step.compute(domainSoFar)
        if (newDomain.continue) newDomain else return Domain(newDomain.facts, true)
      }
    }
  }
}

/** A computation that applies another computation serially to a sequence of values (i.e., a Scala
  * LinearSeqOptimized). The value sequence is passed to the computation as an entry in the domain
  * of facts. The result is a separate sequence of values returned as an entry in the output domain.
  * The series of computations can be terminated early, and only applied to part of the sequence of
  * values, if the inner computation (usually, an AbortingComputation) indicates that it should
  * not continue.
  *
  * For each value in the sequence to be iterated over, the iterative computation creates a distinct
  * domain of facts. This domain is the original domain with the addition of a key-value pair containing
  * the single value. (The key is the key required by the inner computation to identify its
  * input data.)
  *
  * This type of computation will always propagate exceptions up the stack.
  *
  * @constructor           Instantiate an IterativeComputation from another computation, indicating the
  *                        keys in the data map required to carry out the computation and the key that
  *                        will designate the results.
  * @param inner           The computation that will be applied to each element in the value sequence.
  * @param inputMapping    A tuple whose first element is the key that designates the sequence of
  *                        values in the domain, and whose second element designates the key
  *                        required to pass a single value to the inner computation.
  * @param resultKey       The key that will designate the result of the computation in the outgoing
  *                        domain of facts. The result takes the form of a list of the results of
  *                        each inner computation).
  *
  */
class IterativeComputation[+A, +SeqType <: LinearSeqOptimized[A, SeqType]](val inner: Computation,
                           inputMapping: (Symbol, Symbol),
                           val resultKey: Symbol) extends Computation {

  /** Takes a domain of facts and returns a new domain of facts made up of the original set of
    * facts plus the result. This method specifies the details of the computation and must be
    * implemented by classes that mix in this trait.
    *
    * @param domain      A `Domain` containing the facts to be operated on as well as
    *                    additional metadata.
    * @return            A new domain consisting of the original domain of facts plus
    *                    an entry whose key is `resultKey` and whose value is the result
    *                    of the computation. The metadata of the domain may also be different
    *                    from the metadata in the input domain.
    */
  def compute(domain: Domain): Domain = {
    val input: Any = domain.facts.get(inputMapping._1).get
    val reversedResultSequence = computeResultSequence(input, domain)
    val resultSequence = reversedResultSequence.reverse
    Domain.combine(Map(resultKey -> resultSequence), domain)
  }

  private def computeResultSequence(input: Any, originalDomain: Domain): List[Any] = {
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

/** A computation that applies another computation to a each value of a map (i.e., a Scala MapLike).
  * This map  is passed to the computation as an entry in the domain of facts. The result is a
  * separate map of values returned as an entry in the output domain, whose keys are the keys of
  * the original map, and whose values are the results of the inner computation applied to the
  * corresponding values. The series of computations can be terminated early, and only applied
  * to part of the map, if the inner computation (usually, an AbortingComputation) indicates that
  * it should not continue.
  *
  * For each key-value pair in the map, the MappingComputation creates a distinct domain of facts.
  * This domain is the original domain with the addition of an entry containing the value from the
  * key-value pair. (The entry's key is the key required by the inner computation to identify its
  * input data.)
  *
  * This type of computation will always propagate exceptions up the stack.
  *
  * @constructor           Instantiate a MappingComputation from another computation, indicating the
  *                        keys in the domain required to carry out the computation and the key that
  *                        will designate the results.
  * @param inner           The computation that will be applied to each value in the map.
  * @param inputMapping    A tuple whose first element is the key that designates in the domain the
  *                        value map to be iterated over, and whose second element designates the key
  *                        required to pass a single value to the inner computation.
  * @param resultKey       The key that will designate the result of the computation in the outgoing
  *                        domain of facts. The result takes the form of a Map of the results of each
  *                        inner computation, mapped from the keys of the original value map.
  */
class MappingComputation[A, +B, +MapType <: MapLike[A, B, MapType] with Map[A, B]](val inner: Computation,
                           inputMapping: (Symbol, Symbol),
                           val resultKey: Symbol) extends Computation {

  /** Takes a domain of facts and returns a new domain of facts made up of the original set of
    * facts plus the result. This method specifies the details of the computation and must be
    * implemented by classes that mix in this trait.
    *
    * @param domain      A `Domain` containing the facts to be operated on as well as
    *                    additional metadata.
    * @return            A new domain consisting of the original domain of facts plus
    *                    an entry whose key is `resultKey` and whose value is the result
    *                    of the computation. The metadata of the domain may also be different
    *                    from the metadata in the input domain.
    */
  def compute(domain: Domain): Domain = {
    val input: Any = domain.facts.get(inputMapping._1).get
    val resultMap = computeResultSequence(input, domain)
    Domain.combine(Map(resultKey -> resultMap), domain)
  }

  private def computeResultSequence(input: Any, originalDomain: Domain): Map[A, Any] = {
    val inputSequence = input.asInstanceOf[MapLike[A, B, MapType]]

    inputSequence.foldLeft(Map[A, Any]()) {
      (resultsSoFar: Map[A, Any], keyValueTuple: (A, B)) => {
        val domainWithSingleValueAdded = Domain.combine(Map(inputMapping._2 -> keyValueTuple._2), originalDomain)

        val innerResults = inner.compute(domainWithSingleValueAdded)
        innerResults.facts.get(inner.resultKey) match {
          case Some(result) => {
            val theResults = resultsSoFar + (keyValueTuple._1 -> result)
            if(innerResults.continue) theResults else return theResults
          }
          case None => resultsSoFar
        }
      }
    }
  }
}

/** A computation that applies another computation serially to a sequence of values (i.e., a Scala
  * LinearSeqOptimized), accumulating the result in a single value. The input sequence of values
  * is passed to the computation as an entry in the domain of facts, as is the initial value of the
  * accumulator. The result is a distinct values returned as an entry in the output domain.
  * The series of computations can be terminated early, and only applied to part of the sequence of
  * values, if the inner computation (usually, an AbortingComputation) indicates that it should
  * not continue.
  *
  * For each value in the sequence to be iterated over, the folding computation creates a distinct
  * domain of facts. This domain is the original domain with the addition of two key-value pairs,
  * one containing the single value, and the other containing the accumulated result up to that point.
  * (The keys are the keys required by the inner computation to identify the input data; note that
  * the inner computation must depend on at least these two parameters.)
  *
  * This type of computation will always propagate exceptions up the stack.
  *
  * @constructor                  Instantiate a FoldingComputation from another computation, indicating the
  *                               keys in the data map required to carry out the computation.
  * @param initialAccumulatorKey  The key that designates the initial value of the accumulator, to be passed
  *                               to the inner computation the first time it is called.
  * @param inputMapping           A tuple whose first element is the key that designates the sequence of
  *                               values in the domain, and whose second element designates the key
  *                               required to pass a single value to the inner computation.
  * @param accumulatorMapping     A tuple whose first element is the key that designates the accumulated
  *                               value in the domain, and whose second element designates the key
  *                               required to pass the accumulated value to the inner computation. The
  *                               first element also designates the result of the computation in the outgoing
  *                               domain of facts.
  * @param inner                  The computation that will be applied to each element in the value sequence
  *                               along with the accumulated result so far.
  */
class FoldingComputation[+A, +SeqType <: LinearSeqOptimized[A, SeqType]] (initialAccumulatorKey: Symbol,
                                                                          inputMapping: (Symbol, Symbol),
                                                                          accumulatorMapping: (Symbol, Symbol),
                                                                          inner: Computation)
                                                              extends Computation {

  /** Takes a domain of facts and returns a new domain of facts made up of the original set of
    * facts plus the result. This method specifies the details of the computation and must be
    * implemented by classes that mix in this trait.
    *
    * @param domain      A `Domain` containing the facts to be operated on as well as
    *                    additional metadata.
    * @return            A new domain consisting of the original domain of facts plus
    *                    an entry whose key is `resultKey` and whose value is the result
    *                    of the computation. The metadata of the domain may also be different
    *                    from the metadata in the input domain.
    */
  def compute(domain: Domain): Domain = {
    val inputSequence = domain.facts(inputMapping._1)
    val initialAccumulator = domain.facts(initialAccumulatorKey)
    val accumulatedResult = computeAccumulatedResult(initialAccumulator, inputSequence, domain)

    Domain.combine(Map(accumulatorMapping._1 -> accumulatedResult), domain)
  }


  private def computeAccumulatedResult(initialAccumulator: Any, inputSequence: Any, domain: Domain): Any = {
    val valuesToFoldOver = inputSequence.asInstanceOf[LinearSeqOptimized[A, SeqType]]
    valuesToFoldOver.foldLeft(initialAccumulator) {
      (resultsSoFar, value) => {
        val domainWithSingleValueAdded = Domain.combine(Map(inputMapping._2 -> value, accumulatorMapping._2 -> resultsSoFar), domain)
        val innerResults = inner.compute(domainWithSingleValueAdded)
        val result = innerResults.facts(inner.resultKey)
        if (innerResults.continue) result else return result
      }
    }
  }

  /** Returns the symbol that identifies the results of the computation in the domain of facts
    * returned by `compute`. This method must be implemented by classes that mix in this trait.
    */
  def resultKey: Symbol = accumulatorMapping._1
}
