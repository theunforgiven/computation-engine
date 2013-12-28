package com.cyrusinnovation.computation

/* A computation intended to wrap a computation that is part of a series of computations, and stop the
 * remainder of the sequence from being executed if the results fail a test. This trait is implemented
 * by two case classes, `AbortIfNoResults` and `AbortIfHasResults`, each of which takes the inner computation
 * as an argument to its constructor. For more complex abort conditions, create a SimpleComputation to
 * perform the test on the results of a previous computation, and wrap it in one of the two conditions.
 */
trait SequenceAbortingComputation extends Computation {
  def compute(domain: Domain): Domain = {
    val newDomain: Domain = inner.compute(domain)
    val continue = ! shouldAbort(newDomain)
    new Domain(newDomain.facts, continue)
  }

  def inner : Computation
  def shouldAbort(domain: Domain) : Boolean
  def resultKey = inner.resultKey
}

/* Wrap an inner computation that is part of a series of computations, and abort that series if the
 * inner computation does not return a result; i.e. if the resultKey specified for that computation
 * is not found in the domain of facts returned from the computation.
 *
 * @constructor     Instantiate a SequenceAbortingComputation that stops the sequence if the wrapped
 *                  computation does not return a result.
 *
 * @param inner     The wrapped computation whose returned domain map should contain
 *                  the resultKey for that computation, if the sequence is to continue.
 */
sealed case class AbortIfNoResults(inner: Computation) extends SequenceAbortingComputation {
  def shouldAbort(domain: Domain): Boolean = {
    domain.facts.get(inner.resultKey) match {
      case Some(result) => false
      case None => true
    }
  }
}

/* Wrap an inner computation that is part of a series of computations, and abort that series if the
 * inner computation returns a result; i.e. if the resultKey specified for that computation
 * is found in the domain of facts returned from the computation.
 *
 * @constructor     Instantiate a SequenceAbortingComputation that stops the sequence if the wrapped
 *                  computation returns a result.
 *
 * @param inner     The wrapped computation whose returned domain map should not contain
 *                  the resultKey for that computation, if the sequence is to continue.
 */
sealed case class AbortIfHasResults(inner: Computation) extends SequenceAbortingComputation {
  def shouldAbort(domain: Domain): Boolean = {
    domain.facts.get(inner.resultKey) match {
      case Some(result) => true
      case None => false
    }
  }
}

//
