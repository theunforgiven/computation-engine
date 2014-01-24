package com.cyrusinnovation.computation

/* A computation intended to wrap a computation that is part of a series of computations, and stop the
 * remainder of the sequence from being executed if the results fail a test. This trait is implemented
 * by two case classes, `AbortIfNoResults` and `AbortIfHasResults`, each of which takes the inner computation
 * as an argument to its constructor. For more complex abort conditions, create a SimpleComputation to
 * perform the test on the results of a previous computation, and wrap it in one of the two conditions.
 */
trait AbortingComputation extends Computation {
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
 * @constructor     Instantiate an AbortingComputation that stops the sequence if the wrapped
 *                  computation does not return a result.
 *
 * @param inner     The wrapped computation whose returned domain map should contain
 *                  the resultKey for that computation, if the sequence is to continue.
 */
sealed case class AbortIfNoResults(inner: Computation) extends AbortingComputation {
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
 * @constructor     Instantiate an AbortingComputation that stops the sequence if the wrapped
 *                  computation returns a result.
 *
 * @param inner     The wrapped computation whose returned domain map should not contain
 *                  the resultKey for that computation, if the sequence is to continue.
 */
sealed case class AbortIfHasResults(inner: Computation) extends AbortingComputation {
  def shouldAbort(domain: Domain): Boolean = {
    domain.facts.get(inner.resultKey) match {
      case Some(result) => true
      case None => false
    }
  }
}

/* Wrap an inner computation that is part of a series of computations, and abort that series if the
 * inner computation's result satisfies a given condition; i.e. if the facts in the domain returned
 * by the computation satisfy the predicate function passed into the constructor as a string. (A
 * series of computations can be either a SequentialComputation or an IterativeComputation.)
 *
 * If this computation fails to compile or throws an exception during computation, it will always abort
 * the inner series of computations, whether or not exceptions are propagated.
 *
 * @constructor     Instantiate an AbortingComputation that stops the sequence if the wrapped
 *                  computation satisfies a given condition. Compilation of the predicate expression
 *                  occurs in the constructor of the computation.
 *
 * @param packageName                             A java package name for the computation, used to hinder naming collisions.
 *                                                This package will be used as the package for the class compiled from the
 *                                                computation string.
 * @param name                                    A name for the computation. This should follow Java camel case style
 *                                                and contain no spaces, since a class is going to be compiled from it.
 * @param description                             Free text describing the rule.
 * @param imports                                 A list of strings, each of which is a fully qualified class name or
 *                                                otherwise valid Scala identifier/expression that is supplied to an import
 *                                                statement (not including the word "import").
 * @param predicateExpression                     A string that is valid Scala source code for an expression returning a
 *                                                Boolean, containing free variables which will be bound by the keys in the
 *                                                input map.
 * @param inputMapWithTypes                       A map whose keys are the free variables in the transformationExpression,
 *                                                with their types, separated by a colon as in a Scala type annotation
 *                                                (space allowed). The values of the map are the keys that will be applied
 *                                                to the incoming domain of facts in order to select the values with which
 *                                                to bind the variables.
 * @param inner                                   The wrapped computation whose returned domain map should contain values
 *                                                that satisfy the predicate expression.
 * @param securityConfiguration                   An instance of the SecurityConfiguration trait indicating what packages
 *                                                are safe to load, what classes in those packages are unsafe to load, and
 *                                                where the Java security policy file for the current security manager is.
 * @param computationEngineLog                    An instance of `com.cyrusinnovation.computation.util.Log`. A convenience
 *                                                case class `com.cyrusinnovation.computation.util.ComputationEngineLog`
 *                                                extends this trait and wraps an slf4j log passed to its constructor.
 * @param shouldPropagateExceptions               If a computation fails to compile or if it throws an exception
 *                                                on application, it can throw an exception up the stack, or simply
 *                                                log and return the domain it was passed.
 */
import com.cyrusinnovation.computation.util.Log
// TODO allow nesting - what if the domain returned by the inner computation returns a continue of false?
// TODO test exception handling on application
sealed case class AbortIf(packageName: String,
                          name: String,
                          description: String,
                          imports: List[String],
                          predicateExpression: String,
                          inputMapWithTypes: Map[String, Symbol],
                          inner: Computation,
                          securityConfiguration: SecurityConfiguration,
                          computationEngineLog: Log,
                          shouldPropagateExceptions: Boolean = true) extends AbortingComputation {

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
          else (x) => true
        }
    }

  val disabledComputationWarning = s"Defective computation called: ${packageName}.${name}"

  override def compute(domain: Domain): Domain = {
    if(enabled) {
      try {
        super.compute(domain)
      }
      catch {
        case t: Throwable => {
          computationEngineLog.error(s"AbortIf threw exception when processing data: ${domain.facts}", t)
          if(shouldPropagateExceptions) throw t else domain
        }
      }
    } else {
      computationEngineLog.warn(disabledComputationWarning)
      new Domain(domain.facts, false)
    }
  }

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
