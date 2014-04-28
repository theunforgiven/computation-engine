package com.cyrusinnovation.computation.db

import org.joda.time.DateTime

trait AstNode {
  def verifyNoCyclicalReferences(topLevelFactoryMap: Map[String, TopLevelComputationFactory], refNodesVisited: Set[Ref]) : Set[Ref] = this match {
    case thisRefNode: Ref => {
      if(refNodesVisited.contains(thisRefNode)) {
        throw new InvalidComputationSpecException("Computation hierarchy may not contain cyclical references")
      } else {
        val nextNodeToVisit = topLevelFactoryMap(thisRefNode.referencedFactoryName)
        nextNodeToVisit.verifyNoCyclicalReferences(topLevelFactoryMap, (refNodesVisited + thisRefNode))
      }
    }
    case _ => {
      children.foldLeft(refNodesVisited){
        (nodesVisitedSoFar, node) => node.verifyNoCyclicalReferences(topLevelFactoryMap, nodesVisitedSoFar)
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
  override def verifyNoCyclicalReferences(topLevelFactoryMap: Map[String, TopLevelComputationFactory], refNodesVisited: Set[Ref]) = {
    topLevelFactoryMap.values.flatMap(topLevelFactory => topLevelFactory.verifyNoCyclicalReferences(topLevelFactoryMap, Set())).toSet
  }
}

case class Version(versionNumber: String,
                   state: VersionState,
                   commitDate: Option[DateTime],
                   lastEditDate: Option[DateTime],
                   firstTopLevelComputation: TopLevelComputationFactory,
                   moreTopLevelComputations: TopLevelComputationFactory*) extends AstNode {

  def children = topLevelFactories.values.toList

  def topLevelFactories = createFactoryMap(firstTopLevelComputation :: moreTopLevelComputations.toList)
  
  def createFactoryMap(factories: List[TopLevelComputationFactory]) : Map[String, TopLevelComputationFactory] = {
    factories.foldLeft(Map[String, TopLevelComputationFactory]()) {
      (mapSoFar, factory) => mapSoFar + (factory.fullyQualifiedName -> factory)
    }
  }

  def verifyNoCyclicalReferences() : Set[Ref] = verifyNoCyclicalReferences(this.topLevelFactories, Set())

  //To verify that there are no cycles, go down each top level branch separately and make sure it isn't traversed more than once.
  override def verifyNoCyclicalReferences(topLevelFactoryMap: Map[String, TopLevelComputationFactory], refNodesVisited: Set[Ref]) = {
    topLevelFactoryMap.values.flatMap(topLevelFactory => topLevelFactory.verifyNoCyclicalReferences(topLevelFactoryMap, Set())).toSet
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

trait TopLevelComputationFactory extends AstNode {
  val packageValue: String
  val name: String
  def fullyQualifiedName = packageValue + "." + name
}

case class SimpleComputationFactory(
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
    securityConfiguration: String) extends TopLevelComputationFactory with InnerComputationFactory {

  def children = List()
}

case class AbortIfComputationFactory(
    packageValue: String,
    name: String,
    description: String,
    changedInVersion: String,
    shouldPropagateExceptions: Boolean,
    predicateExpression: String,
    innerFactory: InnerComputationFactory,
    imports: Imports,
    input: Inputs,
    logger: String,
    securityConfiguration: String) extends TopLevelComputationFactory with InnerComputationFactory {

  def children = List(imports, input, innerFactory)
}

trait InnerComputationFactory extends AstNode

case class Imports(importSequence: String*) extends AstNode {
  def children = List()
}

case class Inputs(firstInputMapping: Mapping, moreInputMappings: Mapping*) extends AstNode {
  def inputMappings = firstInputMapping :: moreInputMappings.toList
  def children = inputMappings
}

case class Mapping(key: String, value: String) extends AstNode {
  def children = List()
}

case class NamedComputationFactory(
    packageValue: String,
    name: String,
    description: String,
    changedInVersion: String,
    factoryForNamableComputation: NamableComputationFactory) extends TopLevelComputationFactory with InnerComputationFactory {

  def children = List(factoryForNamableComputation)
}

trait NamableComputationFactory extends AstNode

trait SimpleAbortComputationFactory extends NamableComputationFactory with InnerComputationFactory {
  val inner: InnerComputationFactory
  def children = List(inner)
}
case class AbortIfNoResultsComputationFactory(inner: InnerComputationFactory) extends SimpleAbortComputationFactory
case class AbortIfHasResultsComputationFactory(inner: InnerComputationFactory) extends SimpleAbortComputationFactory

trait SimpleAggregateComputationFactory extends NamableComputationFactory with InnerComputationFactory {
  val innerFactory: InnerComputationFactory
  val inputTuple: Mapping
  val resultKey: String
  def children = List(inputTuple, innerFactory)
}

case class IterativeComputationFactory(
  innerFactory: InnerComputationFactory,
  inputTuple: Mapping,
  resultKey: String) extends SimpleAggregateComputationFactory

case class MappingComputationFactory(
  innerFactory: InnerComputationFactory,
  inputTuple: Mapping,
  resultKey: String) extends SimpleAggregateComputationFactory

case class FoldingComputationFactory(
    innerComputation: InnerComputationFactory,
    initialAccumulatorKey: String,
    inputTuple: Mapping,
    accumulatorTuple: Mapping) extends NamableComputationFactory with InnerComputationFactory {
  def children = List(inputTuple, accumulatorTuple, innerComputation)
}

case class SequentialComputationFactory(
  firstInnerComputation: InnerComputationFactory, 
  moreInnerComputations: InnerComputationFactory*) extends NamableComputationFactory with InnerComputationFactory {
  
  def innerFactories = firstInnerComputation :: moreInnerComputations.toList
  def children = innerFactories
}

// Ref is not a case class because we don't want multiple instances referring to the same thing to be equal.
// This allows us to determine if we've already visited a Ref node when validating the syntax tree for cyclical references.
class Ref(val referencedFactoryName: String) extends InnerComputationFactory {
  def children = List()
  override def equals(other : Any) : Boolean = other match {
    case that : Ref => that eq this
    case _ => false
  }
}

case class InvalidComputationSpecException(message: String) extends Exception(message)