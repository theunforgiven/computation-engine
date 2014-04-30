package com.cyrusinnovation.computation.specification

import org.joda.time.DateTime
import com.cyrusinnovation.computation._
import com.cyrusinnovation.computation.util.Log

case class Library(name: String, versions: Map[String, Version]) extends SyntaxTreeNode {
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
                   moreTopLevelComputations: TopLevelComputationSpecification*) extends SyntaxTreeNode {

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

trait ComputationSpecification extends SyntaxTreeNode

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
}

trait InnerComputationSpecification extends ComputationSpecification

case class Imports(importSequence: String*) extends SyntaxTreeNode {
  def children = List()
}

case class Inputs(firstInputMapping: Mapping, moreInputMappings: Mapping*) extends SyntaxTreeNode {
  def inputMappings = firstInputMapping :: moreInputMappings.toList
  def children = inputMappings
}

case class Mapping(key: String, value: String) extends SyntaxTreeNode {
  def children = List()
}

case class NamedComputationSpecification(
    packageValue: String,
    name: String,
    description: String,
    changedInVersion: String,
    specForNamableComputation: NamableComputationSpecification) extends TopLevelComputationSpecification with InnerComputationSpecification {

  def children = List(specForNamableComputation)
}

trait NamableComputationSpecification extends ComputationSpecification

trait SimpleAbortComputationSpecification extends NamableComputationSpecification with InnerComputationSpecification {
  val inner: InnerComputationSpecification
  def children = List(inner)
}
case class AbortIfNoResultsComputationSpecification(inner: InnerComputationSpecification) extends SimpleAbortComputationSpecification

case class AbortIfHasResultsComputationSpecification(inner: InnerComputationSpecification) extends SimpleAbortComputationSpecification

trait SimpleAggregateComputationSpecification extends NamableComputationSpecification with InnerComputationSpecification {
  val innerSpecification: InnerComputationSpecification
  val inputTuple: Mapping
  val resultKey: String
  def children = List(inputTuple, innerSpecification)
}

case class IterativeComputationSpecification(
    innerSpecification: InnerComputationSpecification,
    inputTuple: Mapping,
    resultKey: String) extends SimpleAggregateComputationSpecification

case class MappingComputationSpecification(
    innerSpecification: InnerComputationSpecification,
    inputTuple: Mapping,
    resultKey: String) extends SimpleAggregateComputationSpecification

case class FoldingComputationSpecification(
    innerSpecification: InnerComputationSpecification,
    initialAccumulatorKey: String,
    inputTuple: Mapping,
    accumulatorTuple: Mapping) extends NamableComputationSpecification with InnerComputationSpecification {
  
  def children = List(inputTuple, accumulatorTuple, innerSpecification)
}

case class SequentialComputationSpecification(
  firstInnerComputation: InnerComputationSpecification, 
  moreInnerComputations: InnerComputationSpecification*) extends NamableComputationSpecification with InnerComputationSpecification {
  
  def innerSpecs = firstInnerComputation :: moreInnerComputations.toList
  def children = innerSpecs
}

// Ref is not a case class because we don't want multiple instances referring to the same thing to be equal.
// This allows us to determine if we've already visited a Ref node when validating the syntax tree for cyclical references.
class Ref(val referencedSpecification: String) extends InnerComputationSpecification {
  def children = List()
  override def equals(other : Any) : Boolean = other match {
    case that : Ref => that eq this
    case _ => false
  }
}

case class InvalidComputationSpecException(message: String) extends Exception(message)