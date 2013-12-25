package com.cyrusinnovation.computation

trait Computation {

  def compute(facts: Map[Symbol, Any]) : Map[Symbol, Any] = {
    val domain = new Domain(facts, true)
    val results = compute(domain)
    results.facts
  }

  def compute(domain: Domain): Domain
}

/* A computation instantiated from a Scala expression passed into the constructor as a string,
 * along with various additional configurations (see constructor params). When the computation's `compute`
 * method is called, the computation will execute against an arbitrary Scala map (a `Map[Any, Any]`)
 * and return a `Map[Any, Any]` containing the results.
 *
 *
 * @constructor Instantiate a SimpleComputation. Compilation of the computation expression occurs in the constructor
 *              of the computation.
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
 * @param computationExpression                   A string that is a valid Scala expression, inside curly braces,
 *                                                containing free variables which will be bound by the keys in the input
 *                                                and output maps.
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
 * @param shouldComtinueIfThisComputationApplies  Indicates whether a sequence of computations containing this
 *                                                computation should stop if this rule returns a nonempty map.
 * @param shouldPropagateExceptions               If a computation fails to compile or if it throws an exception
 *                                                on application, it can throw an exception up the stack, or simply
 *                                                log and return the domain it was passed.
 */
// TODO For testing - create a constructor that allows passing in a compiled object instead of a code string?
class SimpleComputation(packageName: String,
                        name: String,
                        description: String,
                        imports: List[String],
                        computationExpression: String,
                        inputMapWithTypes: Map[String, Symbol],
                        resultKey: Symbol,
                        securityConfiguration: SecurityConfiguration,
                        shouldContinueIfThisComputationApplies: Boolean = true, //TODO Fix this parameter
                        shouldPropagateExceptions: Boolean = true) extends Computation {

  private val fullExpression = SimpleComputation.createFunctionBody(computationExpression, inputMapWithTypes, resultKey)

  // TODO Put in try block and deactivate rule if compilation fails
  // TODO Test the safety of the sandbox
  private val transformationFunction = EvalCode(packageName,
                                                imports,
                                                name,
                                                fullExpression,
                                                securityConfiguration).newInstance

  def compute(domain: Domain) : Domain = {
    // TODO Test error handling
    try {
      val newFacts: Map[Symbol, Any] = transformationFunction(domain.facts)
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

  def createFunctionBody(computationExpression: String, inputMap: Map[String, Symbol], resultKey: Symbol) = {
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

    s"""if($emptyCheckExpression) Map() else {
          $inputMappings
          ($computationExpression : Option[Any]) match {
            case Some(value) => Map($resultKey -> value)
            case None => Map()
          }
        }"""
  }
}

