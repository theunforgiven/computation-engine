package com.cyrusinnovation.computation.db.schema

case class Library(versions: Version*)

case class Version(
  computations: Computations,
  commitDate: Option[javax.xml.datatype.XMLGregorianCalendar] = None,
  lastEditDate: Option[javax.xml.datatype.XMLGregorianCalendar] = None,
  state: VersionState,
  versionNumber: String)


object VersionState {
  def fromString(value: String, scope: scala.xml.NamespaceBinding): VersionState = value match {
    case "Editable" => Editable
    case "Committed" => Committed
  }
}

trait VersionState
case object Editable extends VersionState { override def toString = "Editable" }
case object Committed extends VersionState { override def toString = "Committed" }


case class Computations(topLevelComputation: TopLevelComputationType*)

trait TopLevelComputationType

case class SimpleComputation(
  computationExpression: String,
  imports: Imports,
  input: Inputs,
  resultKey: String,
  logger: String,
  securityConfiguration: String,
  packageValue: String,
  name: String,
  description: String,
  changedInVersion: String,
  shouldPropagateExceptions: Boolean) extends TopLevelComputationType with InnerComputationType

case class AbortIfComputation(
  predicateExpression: String,
  innerComputation: InnerComputationType,
  imports: Imports,
  input: Inputs,
  logger: String,
  securityConfiguration: String,
  packageValue: String,
  name: String,
  description: String,
  changedInVersion: String,
  shouldPropagateExceptions: Boolean) extends TopLevelComputationType with InnerComputationType

case class NamedComputation(
  namableComputation: NamableComputation,
  packageValue: String,
  name: String,
  description: String,
  changedInVersion: String) extends TopLevelComputationType with InnerComputationType

case class Imports(importSequence: ImportSequence*)
case class ImportSequence(importValue: String)

case class Inputs(inputTypeSequence: InputMapping*)
case class InputMapping(key: String, value: String)

trait NamableComputation

case class SimpleAbortComputationType(innerComputation: InnerComputationType) extends NamableComputation with InnerComputationType

case class SimpleAggregateComputationType(
  innerComputation: InnerComputationType,
  input: Inputs,
  resultKey: String) extends NamableComputation with InnerComputationType

case class SequentialComputation(innerComputations: InnerComputations) extends NamableComputation with InnerComputationType

case class InnerComputations(innerComputationSequence: InnerComputationType*)
trait InnerComputationType

case class Ref(referencedComputation: String) extends InnerComputationType
