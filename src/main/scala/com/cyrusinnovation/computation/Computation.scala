package com.cyrusinnovation.computation

trait Computation {

  def compute(facts: Map[Any, Any]) : Map[Any, Any] = {
    val domain = new Domain(facts, true)
    val results = compute(domain)
    results.facts
  }

  def compute(domain: Domain): Domain
}

/*
  name: A name for the computation. This should follow Java camel case style and contain no spaces.
  description: Free text describing the rule.
  transformationExpression: A string that is a valid Scala expression, inside curly braces, containing
                            free variables which will be bound by the keys in the input and output maps.
  inputMapWithTypes: A map whose keys are the free variables in the transformationExpression, with their
                     types, separated by a colon as in a Scala type annotation (space allowed). The values
                     of the map are the keys that will be applied to the incoming domain of facts in order
                     to select the values with which to bind the variables.
  outputMapWithoutType: A map with a single entry, whose key is the name of the free variable in the
                        transformationExpression that will carry the value to be returned from the computation.
                        The value is the key that will be used to identify the returned value in the outgoing
                        domain of facts.
  shouldComtinueIfThisComputationApplies: Indicates whether a sequence of computations containing this computation
                                          should stop if this rule returns a nonempty map.
  shouldPropagateExceptions: If a computation fails to compile or if it throws an exception on application, it
                             can throw an exception up the stack, or simply log and return the domain it was passed.

 */
class SimpleComputation(name: String,
                        description: String,
                        transformationExpression: String,
                        inputMapWithTypes: Map[String, String],
                        outputMapWithoutType: Map[String, String],
                        shouldContinueIfThisComputationApplies: Boolean = true,
                        shouldPropagateExceptions: Boolean = true) extends Computation {

  private val completeExpression = SimpleComputation.createFunctionBody(transformationExpression, inputMapWithTypes, outputMapWithoutType)

  // TODO Put in try block and deactivate rule if compilation fails
  // TODO Test the safety of the sandbox
  private val transformationFunction = EvalCode.with1Arg[Map[Any, Any], Map[Any, Any]](name,
                                                                                       "domainFacts",
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
          val $outputVal: Option[Any] = $transformationExpression
          $outputVal match {
            case Some(value) => Map($outputDomainKey -> value)
            case None => Map()
          }
        }"""
  }
}

