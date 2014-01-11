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

// TODO allow nesting - what if the domain returned by the inner computation returns a continue of false?
// TODO test exception handling on compilation
// TODO test exception handling on application
/* Wrap an inner computation that is part of a series of computations, and abort that series if the
 * inner computation's result satisfies a given condition; i.e. if the facts in the domain returned
 * by the computation satisfy the predicate function passed into the constructor as a string.
 *
 * If this computation fails to compile, it will always abort.
 *
 * @constructor     Instantiate a SequenceAbortingComputation that stops the sequence if the wrapped
 *                  computation satisfies a given condition.
 *
 * @param condition A string that is valid Scala source code for a function of type
 *                  `Map[Symbol, Any] => Boolean`
 *
 * @param inner     The wrapped computation whose returned domain map should contain a value for
 *                  the resultKey, which result satisfies the condition.
 */
import com.cyrusinnovation.computation.util.Log
sealed case class AbortIf(packageName: String,
                          name: String,
                          description: String,
                          imports: List[String],
                          predicateExpression: String,
                          inputMapWithTypes: Map[String, Symbol],
                          inner: Computation,
                          securityConfiguration: SecurityConfiguration,
                          computationEngineLog: Log,
                          shouldPropagateExceptions: Boolean = true) extends SequenceAbortingComputation {

private var enabled = true
private var fullPredicateExpression = AbortIf.createFunctionBody(predicateExpression, inputMapWithTypes, resultKey)

private val predicateFunction: Map[Symbol, Any] => Boolean =
    try {
      EvalPredicateFunctionString(packageName,
                                  imports,
                                  name,
                                  fullPredicateExpression,
                                  securityConfiguration).newInstance
    } catch {
        case t: Throwable => {
          computationEngineLog.error("Computation failed to compile", t)
          enabled = false
          if (shouldPropagateExceptions) throw t
          else (x) => false
        }
    }

  //TODO Log warning and skip computation if compilation fails
  // val disabledComputationWarning = s"Disabled computation called: ${packageName}.${name}"

  def shouldAbort(domain: Domain): Boolean = {
    predicateFunction(domain.facts)
  }
}

object AbortIf {
  def createFunctionBody(predicateExpression: String, inputMap: Map[String, Symbol], resultKey: Symbol) = {

    val inputAssignments  = Computation.createInputMappings(inputMap)

    s"""$inputAssignments
      | $predicateExpression""".stripMargin
  }
}
