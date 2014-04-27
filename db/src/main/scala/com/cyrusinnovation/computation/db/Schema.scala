package com.cyrusinnovation.computation.db

import org.joda.time.DateTime

trait AstNode
case class AstTextNode(text: String) extends AstNode

case class Library(name: String, versions: Map[String, Version]) extends AstNode

case class Version(versionNumber: String,
                   state: VersionState,
                   commitDate: Option[DateTime],
                   lastEditDate: Option[DateTime],
                   firstTopLevelComputation: TopLevelComputationFactory,
                   moreTopLevelComputations: TopLevelComputationFactory*) extends AstNode {
  
  def topLevelFactories = createFactoryMap(firstTopLevelComputation :: moreTopLevelComputations.toList)
  
  def createFactoryMap(factories: List[TopLevelComputationFactory]) : Map[String, TopLevelComputationFactory] = {
    factories.foldLeft(Map[String, TopLevelComputationFactory]()) {
      (mapSoFar, factory) => mapSoFar + (factory.fullyQualifiedName -> factory)
    }
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
  securityConfiguration: String) extends TopLevelComputationFactory with InnerComputationFactory

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
  securityConfiguration: String) extends TopLevelComputationFactory with InnerComputationFactory

trait InnerComputationFactory extends AstNode

case class Imports(importSequence: String*) extends AstNode

case class Inputs(firstInputMapping: Mapping, moreInputMappings: Mapping*) extends AstNode {
  def inputMappings = firstInputMapping :: moreInputMappings.toList
}

case class Mapping(key: String, value: String) extends AstNode

case class NamedComputationFactory(
  packageValue: String,
  name: String,
  description: String,
  changedInVersion: String,
  factoryForNamableComputation: NamableComputationFactory) extends TopLevelComputationFactory with InnerComputationFactory

trait NamableComputationFactory

trait SimpleAbortComputationFactory extends NamableComputationFactory with InnerComputationFactory {
  val inner: InnerComputationFactory
}
case class AbortIfNoResultsComputationFactory(inner: InnerComputationFactory) extends SimpleAbortComputationFactory
case class AbortIfHasResultsComputationFactory(inner: InnerComputationFactory) extends SimpleAbortComputationFactory

trait SimpleAggregateComputationFactory extends NamableComputationFactory with InnerComputationFactory {
  val innerFactory: InnerComputationFactory
  val inputTuple: Mapping
  val resultKey: String
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
  accumulatorTuple: Mapping) extends NamableComputationFactory with InnerComputationFactory

case class SequentialComputationFactory(
  firstInnerComputation: InnerComputationFactory, 
  moreInnerComputations: InnerComputationFactory*) extends NamableComputationFactory with InnerComputationFactory {
  
  def innerFactories = firstInnerComputation :: moreInnerComputations.toList
}

case class Ref(referencedFactoryName: String) extends InnerComputationFactory
