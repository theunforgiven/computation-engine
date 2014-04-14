package com.cyrusinnovation.computation
/*
 * Copyright 2014 Cyrus Innovation, LLC. Licensed under Apache license 2.0.
 */

import com.cyrusinnovation.computation.util.Log

/** Operates on a set of facts (contained in a Map[Symbol, Any] to return a result in
 * the same form. Implementers must extend the `compute` method to specify the specific
 * computation, and the `resultKey` method allowing the results to be identified in the
 * returned map.
 */
trait Computation {

/** Takes a set of facts and returns a new set of facts made up of the original set of
 * facts plus the result. This method is generally the one called by clients to execute
 * computations
 *
 * @param facts       A map whose keys the computation will use to identify the values
 *                    to be operated on in the computation.
 * @return            A new map consisting of the original map of facts plus an entry
 *                    whose key is `resultKey` and whose value is the result of the
 *                    computation.
 */
  def compute(facts: Map[Symbol, Any]) : Map[Symbol, Any] = {
    val domain = new Domain(facts, true)
    val results = compute(domain)
    results.facts
  }

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
  def compute(domain: Domain): Domain

/** Returns the symbol that identifies the results of the computation in the domain of facts
 * returned by `compute`. This method must be implemented by classes that mix in this trait.
 */
  def resultKey : Symbol
}

object Computation {
  def createInputMappings(inputMap: Map[String, Symbol]) : String = {
    val inputMappings = if (inputMap == null) Map() else inputMap

    inputMappings.foldLeft("") {
      (soFar, keyValuePair) => {
        val valWithType = keyValuePair._1
        val domainKey = keyValuePair._2
        val theType = valWithType.split( """:\s*""").last
        soFar + s"""val $valWithType = domainFacts.get($domainKey).get.asInstanceOf[$theType]\n"""
      }
    }
  }
}

/** A computation instantiated from a Scala expression passed into the constructor as a string,
 * along with various additional configurations (see constructor params). When the computation's `compute`
 * method is called, the computation will execute against an arbitrary Scala map (a `Map[Any, Any]`)
 * and return a `Map[Any, Any]` containing the results.
 *
 *
 * @constructor                                   Instantiate a SimpleComputation. Compilation of the computation expression
   *                                              occurs in the constructor of the computation.
 * @param packageName                             A java package name for the computation, used to hinder naming collisions.
 *                                                This package will be used as the package for the class compiled from the
 *                                                computation string.
 * @param name                                    A name for the computation. This should follow Java camel case style
 *                                                and contain no spaces, since a class is going to be compiled from it.
 * @param description                             Free text describing the rule.
 * @param imports                                 A list of strings, each of which is a fully qualified class name or
 *                                                otherwise valid Scala identifier/expression that is supplied to an import
 *                                                statement (not including the word "import").
 * @param computationExpression                   A string that is source code for a valid Scala expression, inside curly
 *                                                braces, containing free variables which will be bound by the keys in the
 *                                                input and output maps.
 * @param inputMapWithTypes                       A map whose keys are the free variables in the transformationExpression,
 *                                                with their types, separated by a colon as in a Scala type annotation
 *                                                (space allowed). The values of the map are the keys that will be applied
 *                                                to the incoming domain of facts in order to select the values with which
 *                                                to bind the variables.
 * @param resultKey                               The key that will be used to identify the result of the computation
 *                                                in the outgoing domain of facts.
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
class SimpleComputation(packageName: String,
                        name: String,
                        description: String,
                        imports: List[String],
                        computationExpression: String,
                        inputMapWithTypes: Map[String, Symbol],
                        val resultKey: Symbol,
                        securityConfiguration: SecurityConfiguration,
                        computationEngineLog: Log,
                        shouldPropagateExceptions: Boolean = true) extends Computation {

  private var enabled = true
  private var fullExpression = SimpleComputation.createFunctionBody(computationExpression, inputMapWithTypes, resultKey)

  private val transformationFunction: Map[Symbol, Any] => Map[Symbol, Any] =
    try {
      EvalSimpleComputationString( packageName,
                imports,
                name,
                fullExpression,
                securityConfiguration).newInstance
    } catch {
        case t: Throwable => {
          computationEngineLog.error("Computation failed to compile", t)
          enabled = false
          if (shouldPropagateExceptions) throw t
          else (x) => Map()
        }
    }

  val disabledComputationWarning = s"Disabled computation called: ${packageName}.${name}"

/** Takes a domain of facts and returns a new domain of facts made up of the original set of
 * facts plus the result. Implements `compute` on the `Computation` trait. This method will
 * propagate exceptions or not depending on whether the `shouldPropagateExceptions` constructor
 * parameter is set.
 */
  def compute(domain: Domain) : Domain = {
    if(enabled) {
      try {
        computationEngineLog.debug(s"${packageName}.${name}: Input: ${domain.facts}")
        val newFacts: Map[Symbol, Any] = transformationFunction(domain.facts)
        computationEngineLog.debug(s"${packageName}.${name}: Results: ${newFacts}")
        Domain.combine(newFacts, domain)
      }
      catch {
        case t: Throwable => {
          computationEngineLog.error(s"Computation threw exception when processing data: ${domain.facts}", t)
          if(shouldPropagateExceptions) throw t else domain
        }
      }
    } else {
      computationEngineLog.warn(disabledComputationWarning)
      domain
    }
  }
}

object SimpleComputation {

  def createFunctionBody(computationExpression: String, inputMap: Map[String, Symbol], resultKey: Symbol) = {

    val inputAssignments  = Computation.createInputMappings(inputMap)

    s"""$inputAssignments
      | ( { $computationExpression } : Option[Any]) match {
      |   case Some(value) => Map($resultKey -> value)
      |   case None => Map()
      |}""".stripMargin
  }
}

