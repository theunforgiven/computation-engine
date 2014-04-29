package com.cyrusinnovation.computation.db

import org.joda.time.DateTime
import com.cyrusinnovation.computation._
import com.cyrusinnovation.computation.util.Log

trait AstNode {
  def verifyNoCyclicalReferences(topLevelSpecificationMap: Map[String, TopLevelComputationSpecification], refNodesVisited: Set[Ref]) : Set[Ref] = this match {
    case thisRefNode: Ref => {
      if(refNodesVisited.contains(thisRefNode)) {
        throw new InvalidComputationSpecException("Computation hierarchy may not contain cyclical references")
      } else {
        val nextNodeToVisit = topLevelSpecificationMap(thisRefNode.referencedSpecification)
        nextNodeToVisit.verifyNoCyclicalReferences(topLevelSpecificationMap, (refNodesVisited + thisRefNode))
      }
    }
    case _ => {
      children.foldLeft(refNodesVisited){
        (nodesVisitedSoFar, node) => node.verifyNoCyclicalReferences(topLevelSpecificationMap, nodesVisitedSoFar)
      }
    }
  }

  def children : List[AstNode]
}

case class AstTextNode(text: String) extends AstNode {
  def children = List()
}

case class Library(name: String, versions: Map[String, Version]) extends AstNode {
  def children = versions.values.toList

  def verifyNoCyclicalReferences() : Set[Ref] = {
    versions.values.flatMap(version => version.verifyNoCyclicalReferences()).toSet
  }

  def verifyNoCyclicalReferences(versionNumber: String) : Set[Ref] = {
    versions(versionNumber).verifyNoCyclicalReferences()
  }

  //To verify that there are no cycles, go down each top level branch separately and make sure it isn't traversed more than once.
  override def verifyNoCyclicalReferences(topLevelSpecificationMap: Map[String, TopLevelComputationSpecification], refNodesVisited: Set[Ref]) = {
    topLevelSpecificationMap.values.flatMap(topLevelSpecification => topLevelSpecification.verifyNoCyclicalReferences(topLevelSpecificationMap, Set())).toSet
  }
}

case class Version(versionNumber: String,
                   state: VersionState,
                   commitDate: Option[DateTime],
                   lastEditDate: Option[DateTime],
                   firstTopLevelComputation: TopLevelComputationSpecification,
                   moreTopLevelComputations: TopLevelComputationSpecification*) extends AstNode {

  def children = topLevelSpecifications.values.toList

  def topLevelSpecifications = createSpecificationMap(firstTopLevelComputation :: moreTopLevelComputations.toList)
  
  def createSpecificationMap(specs: List[TopLevelComputationSpecification]) : Map[String, TopLevelComputationSpecification] = {
    specs.foldLeft(Map[String, TopLevelComputationSpecification]()) {
      (mapSoFar, specification) => mapSoFar + (specification.fullyQualifiedName -> specification)
    }
  }

  def verifyNoCyclicalReferences() : Set[Ref] = verifyNoCyclicalReferences(this.topLevelSpecifications, Set())

  //To verify that there are no cycles, go down each top level branch separately and make sure it isn't traversed more than once.
  override def verifyNoCyclicalReferences(topLevelSpecificationMap: Map[String, TopLevelComputationSpecification], refNodesVisited: Set[Ref]) = {
    topLevelSpecificationMap.values.flatMap(topLevelSpecification => topLevelSpecification.verifyNoCyclicalReferences(topLevelSpecificationMap, Set())).toSet
  }
}

object VersionState {
  def fromString(value: String): VersionState = value match {
    case "Editable" => Editable
    case "Committed" => Committed
    case other => throw new RuntimeException(s"No such state: $other")
  }
}

sealed trait VersionState
case object Editable extends VersionState { override def toString = "Editable" }
case object Committed extends VersionState { override def toString = "Committed" }

trait ComputationSpecification extends AstNode {
  private var cachedComputation : Option[Computation] = None
  protected def computation(topLevelSpecs: Map[String, TopLevelComputationSpecification]) : Computation

  def build(topLevelSpecs: Map[String, TopLevelComputationSpecification]) : Computation = cachedComputation match {
    case Some(computation) => computation
    case None => {
      val theComputation = computation(topLevelSpecs)
      cachedComputation = Some(theComputation)
      theComputation
    }
  }
}

trait TopLevelComputationSpecification extends ComputationSpecification {
  val packageValue: String
  val name: String
  def fullyQualifiedName = packageValue + "." + name
  var securityConfigurations : Map[String, SecurityConfiguration] = Map()
  var loggers : Map[String, Log] = Map()

  def securityConfiguration(key: String) : SecurityConfiguration = {
    securityConfigurations(key)
  }
  
  def logger(key: String) : Log = {
    loggers(key)
  }
}

case class SimpleComputationSpecification(
    packageValue: String,
    name: String,
    description: String,
    changedInVersion: String,
    shouldPropagateExceptions: Boolean,
    computationExpression: String,
    imports: Imports,
    input: Inputs,
    resultKey: String,
    logger: String,
    securityConfiguration: String) extends TopLevelComputationSpecification with InnerComputationSpecification {

  def children = List()
  protected def computation(topLevelSpecs: Map[String, TopLevelComputationSpecification]) = new SimpleComputation(
    packageValue,
    name,
    description,
    imports.toList,
    computationExpression,
    input.toMap,
    Symbol(resultKey),
    securityConfiguration(securityConfiguration),
    logger(logger),
    shouldPropagateExceptions)
}

case class AbortIfComputationSpecification(
    packageValue: String,
    name: String,
    description: String,
    changedInVersion: String,
    shouldPropagateExceptions: Boolean,
    predicateExpression: String,
    innerSpecification: InnerComputationSpecification,
    imports: Imports,
    input: Inputs,
    logger: String,
    securityConfiguration: String) extends TopLevelComputationSpecification with InnerComputationSpecification {

  def children = List(imports, input, innerSpecification)
  
  protected def computation(topLevelSpecs: Map[String, TopLevelComputationSpecification]) = AbortIf(
    packageValue,
    name,
    description,
    imports.toList,
    predicateExpression,
    input.toMap,
    innerSpecification.build(topLevelSpecs),
    securityConfiguration(securityConfiguration),
    logger(logger),
    shouldPropagateExceptions)
}

trait InnerComputationSpecification extends ComputationSpecification

case class Imports(importSequence: String*) extends AstNode {
  def children = List()
  def toList = importSequence.toList
}

case class Inputs(firstInputMapping: Mapping, moreInputMappings: Mapping*) extends AstNode {
  def inputMappings = firstInputMapping :: moreInputMappings.toList
  def children = inputMappings
  def toMap = inputMappings.foldLeft(Map[String, Symbol]()) {
    (mapSoFar, mapping) => mapSoFar + mapping.stringSymbolTuple
  }
}

case class Mapping(key: String, value: String) extends AstNode {
  def children = List()
  def stringSymbolTuple = (key, Symbol(value))
  def symbolTuple = (Symbol(key), Symbol(value))
}

case class NamedComputationSpecification(
    packageValue: String,
    name: String,
    description: String,
    changedInVersion: String,
    specForNamableComputation: NamableComputationSpecification) extends TopLevelComputationSpecification with InnerComputationSpecification {

  def children = List(specForNamableComputation)
  
  protected def computation(topLevelSpecs: Map[String, TopLevelComputationSpecification]) = specForNamableComputation.build(topLevelSpecs)
}

trait NamableComputationSpecification extends ComputationSpecification

trait SimpleAbortComputationSpecification extends NamableComputationSpecification with InnerComputationSpecification {
  val inner: InnerComputationSpecification
  def children = List(inner)
}
case class AbortIfNoResultsComputationSpecification(inner: InnerComputationSpecification) extends SimpleAbortComputationSpecification {
  protected def computation(topLevelSpecs: Map[String, TopLevelComputationSpecification]) = AbortIfNoResults(inner.build(topLevelSpecs))
}
case class AbortIfHasResultsComputationSpecification(inner: InnerComputationSpecification) extends SimpleAbortComputationSpecification {
  protected def computation(topLevelSpecs: Map[String, TopLevelComputationSpecification]) = AbortIfHasResults(inner.build(topLevelSpecs))
}

trait SimpleAggregateComputationSpecification extends NamableComputationSpecification with InnerComputationSpecification {
  val innerSpecification: InnerComputationSpecification
  val inputTuple: Mapping
  val resultKey: String
  def children = List(inputTuple, innerSpecification)
}

case class IterativeComputationSpecification(
    innerSpecification: InnerComputationSpecification,
    inputTuple: Mapping,
    resultKey: String) extends SimpleAggregateComputationSpecification {

  protected def computation(topLevelSpecs: Map[String, TopLevelComputationSpecification]) = new IterativeComputation(
    innerSpecification.build(topLevelSpecs),
    inputTuple.symbolTuple,
    Symbol(resultKey))
}

case class MappingComputationSpecification(
    innerSpecification: InnerComputationSpecification,
    inputTuple: Mapping,
    resultKey: String) extends SimpleAggregateComputationSpecification{
    
  protected def computation(topLevelSpecs: Map[String, TopLevelComputationSpecification]) = new MappingComputation(
    innerSpecification.build(topLevelSpecs),
    inputTuple.symbolTuple,
    Symbol(resultKey))
}

case class FoldingComputationSpecification(
    innerSpecification: InnerComputationSpecification,
    initialAccumulatorKey: String,
    inputTuple: Mapping,
    accumulatorTuple: Mapping) extends NamableComputationSpecification with InnerComputationSpecification {
  
  def children = List(inputTuple, accumulatorTuple, innerSpecification)
    
  protected def computation(topLevelSpecs: Map[String, TopLevelComputationSpecification]) = new FoldingComputation(
    Symbol(initialAccumulatorKey),
    inputTuple.symbolTuple,
    accumulatorTuple.symbolTuple,
    innerSpecification.build(topLevelSpecs))
}

case class SequentialComputationSpecification(
  firstInnerComputation: InnerComputationSpecification, 
  moreInnerComputations: InnerComputationSpecification*) extends NamableComputationSpecification with InnerComputationSpecification {
  
  def innerSpecs = firstInnerComputation :: moreInnerComputations.toList
  def children = innerSpecs

  protected def computation(topLevelSpecs: Map[String, TopLevelComputationSpecification]) = new SequentialComputation(
    innerSpecs.map(specification => specification.build(topLevelSpecs)))
}

// Ref is not a case class because we don't want multiple instances referring to the same thing to be equal.
// This allows us to determine if we've already visited a Ref node when validating the syntax tree for cyclical references.
class Ref(val referencedSpecification: String) extends InnerComputationSpecification {
  def children = List()
  override def equals(other : Any) : Boolean = other match {
    case that : Ref => that eq this
    case _ => false
  }

  protected def computation(topLevelSpecs: Map[String, TopLevelComputationSpecification]) = {
    topLevelSpecs(referencedSpecification).build(topLevelSpecs)
  }
}

case class InvalidComputationSpecException(message: String) extends Exception(message)