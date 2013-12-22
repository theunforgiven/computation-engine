package com.cyrusinnovation.computation

import clojure.lang.{IFn, IPersistentMap}
import ClojureConversions._

trait Computation {

  def compute(facts: Map[Any, Any]) : Map[Any, Any] = {
    val domain = new Domain(facts, true)
    val results = compute(domain)
    results.facts
  }

  def compute(domain: Domain): Domain
}

class SimpleComputation(namespace: String,
                        ordering: Int,
                        name: String,
                        description: String,
                        transformationExpression: String,
                        inputMap: Map[String, String],
                        outputMap: Map[String, String],
                        shouldContinueIfThisComputationApplies: Boolean = true,
                        shouldPropagateExceptions: Boolean = true) extends Computation {

  private val completeExpression = SimpleComputation.createFunctionBody(transformationExpression, inputMap, outputMap)

  /* If you rebuild (clean build) this project in IntelliJ, Scala compiles before Clojure.
     This results in a compilation failure since Clojail isn't compiled when the scala compiles.
     Workaround: First, comment out the line below that references Clojail, and then uncomment
     the commented line below binding transformationFunction to ??? in order to make both the
     scala and the clojure compile. Then uncomment the Clojail line and recomment the line below. */
  //private val transformationFunction: IFn = ???

  // TODO Put in try block and deactivate rule if compilation fails
  // TODO Test the safety of the sandbox
  private val transformationFunction = Clojail.safeEval(completeExpression).asInstanceOf[IFn]

  def compute(domain: Domain) : Domain = {
    // TODO Test error handling
    try {
      val newFacts = transformationFunction.invoke(domain.facts).asInstanceOf[IPersistentMap]
      val continue = newFacts.count == 0 || shouldContinueIfThisComputationApplies
      Domain.combine(newFacts, domain, continue)
    }
    catch {
      case e: Throwable => if(shouldPropagateExceptions) throw e else {
        // TODO Figure out some kind of logging
        System.err.println(e.getMessage)
        e.printStackTrace(System.err)
        domain
      }
    }
  }
}

object SimpleComputation {
  def createFunctionBody(transformationExpression: String, inputMap: Map[String, String], outputMap: Map[String, String]) = {
    val inputMappings  = inputMap.foldLeft("") {
      (soFar, keyValuePair) => {
        val binding = keyValuePair._1
        val domainKey = keyValuePair._2
        soFar + s"""$binding (domain-facts $domainKey) """
      }
    }
    val emptyChecks = inputMap.keys.foldLeft("") {
      (soFar: String, binding: String) => {
        soFar + s"""(empty? $binding) """
      }
    }
    val emptyCheckExpression = if(inputMap.keys.size > 1) s"(and $emptyChecks)" else emptyChecks

    val outputBinding = outputMap.head._1
    val outputDomainKey = outputMap.head._2

    s"""(fn ^clojure.lang.IPersistentMap [^clojure.lang.IPersistentMap domain-facts]
          (let [$inputMappings]
           (if $emptyCheckExpression
            {}
            (let [$outputBinding $transformationExpression]
              (hash-map $outputDomainKey $outputBinding)))))"""
  }
}

