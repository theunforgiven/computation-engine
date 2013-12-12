package com.cyrusinnovation.computation

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
                        inputMapWithTypes: Map[String, String],
                        outputMapWithoutType: Map[String, String],
                        shouldContinueIfThisComputationApplies: Boolean = true,
                        shouldPropagateExceptions: Boolean = true) extends Computation {

  private val completeExpression = SimpleComputation.createFunctionBody(transformationExpression, inputMapWithTypes, outputMapWithoutType)

  // TODO Put in try block and deactivate rule if compilation fails
  // TODO Test the safety of the sandbox
  private val transformationFunction = EvalCode.with1Arg[Map[Any, Any], Map[Any, Any]]("domainFacts",
                                                                                       "Map[Any, Any]",
                                                                                       completeExpression,
                                                                                       "Map[Any, Any]").newInstance

  def compute(domain: Domain) : Domain = {
    // TODO Test error handling
    try {
      val newFacts: Map[Any, Any] = transformationFunction(domain.facts)
      val continue = newFacts.isEmpty || shouldContinueIfThisComputationApplies
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
        val valWithType = keyValuePair._1
        val domainKey = keyValuePair._2
        val theType = valWithType.split(""":\s*""").last
        soFar + s"""val $valWithType = domainFacts.get($domainKey).get.asInstanceOf[$theType]\n"""
      }
    }

    val emptyChecks = inputMap.values.map((domainKey) => s"domainFacts.get($domainKey).isEmpty")
    val emptyCheckExpression = if(inputMap.keys.size > 1) emptyChecks.mkString(" || ") else emptyChecks.mkString

    val outputVal = outputMap.head._1
    val outputDomainKey = outputMap.head._2

    s"""if($emptyCheckExpression) Map() else {
          $inputMappings
          val $outputVal = $transformationExpression
          Map($outputDomainKey -> $outputVal)
        }"""
  }
}

