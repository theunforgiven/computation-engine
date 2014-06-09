package com.cyrusinnovation.computation.persistence.writer

import com.cyrusinnovation.computation.specification._
import org.joda.time.DateTime
import LibraryInspector._

object LibraryInspector {
  sealed abstract class Node

  sealed case class CompoundNode(label: String, attrs: Map[String, String], children: List[Node]) extends Node

  sealed case class StringListNode(label: String, children: List[String]) extends Node

  sealed case class NodeListNode(label: String, children: List[Node]) extends Node

  sealed case class MapKeyValueNode(children: Map[String, String]) extends Node
}

trait LibraryInspector {
  def marshal(node: SyntaxTreeNode): Node = {
    node match {
      case node: Library                            => library(node)
      case node: Version                            => version(node)
      case node: SimpleComputationSpecification     => simpleComputationSpec(node)
      case node: AbortIfComputationSpecification    => abortIfComputationSpec(node)
      case node: NamedComputationSpecification      => namedComputationSpec(node)
      case node: SequentialComputationSpecification => sequentialComputationSpec(node)
      case node: MappingComputationSpecification    => mappingComputationSpec(node)
      case node: FoldingComputationSpecification    => foldingComputationSpec(node)
      case node: Ref                                => ref(node)
      case node: Mapping                            => mapping(node)
      case node: Inputs                             => inputs(node)
      case node: Imports                            => imports(node)
    }
  }

  private def library(library: Library) = {
    val versions = library.versions.map(x => marshal(x._2)).toList
    createCompoundNode("library", Map("name" -> library.name), versions)
  }

  protected def version(version: Version) = {
    val lastEditDate = version.lastEditDate.map(dateTime).getOrElse(null)
    val kids = List(marshal(version.firstTopLevelComputation)) ++ version.moreTopLevelComputations.map(marshal(_))
    createCompoundNode("version", Map("versionNumber" -> version.versionNumber, "state" -> version.state.toString, "lastEditDate" -> lastEditDate), kids)
  }

  private def simpleComputationSpec(computation: SimpleComputationSpecification) = {
    val attrs = Map("package" -> computation.packageValue,
      "name" -> computation.name,
      "description" -> computation.description,
      "changedInVersion" -> computation.changedInVersion,
      "shouldPropagateExceptions" -> computation.shouldPropagateExceptions.toString,
      "computationExpression" -> computation.computationExpression,
      "resultKey" -> computation.resultKey,
      "logger" -> computation.logger,
      "securityConfiguration" -> computation.securityConfiguration)
    createCompoundNode("simpleComputation", attrs, List(marshal(computation.imports), marshal(computation.input)))
  }

  private def abortIfComputationSpec(computation: AbortIfComputationSpecification) = {
    val attrs = Map("package" -> computation.packageValue,
      "name" -> computation.name,
      "description" -> computation.description,
      "changedInVersion" -> computation.changedInVersion,
      "logger" -> computation.logger,
      "securityConfiguration" -> computation.securityConfiguration,
      "shouldPropagateExceptions" -> computation.shouldPropagateExceptions.toString,
      "predicateExpression" -> computation.predicateExpression)

    val inners = List(marshal(computation.innerSpecification))
    val innerComputation = createCompoundNode("innerComputation", Map(), inners)
    createCompoundNode("abortIfComputation", attrs, List(innerComputation, marshal(computation.imports), marshal(computation.input)))
  }

  private def namedComputationSpec(computation: NamedComputationSpecification) = {
    val attrs = Map("package" -> computation.packageValue,
      "name" -> computation.name,
      "description" -> computation.description,
      "changedInVersion" -> computation.changedInVersion)
    createCompoundNode("namedComputation", attrs, List(marshal(computation.specForNamableComputation)))
  }

  protected def sequentialComputationSpec(computation: SequentialComputationSpecification) = {
    val inners = computation.innerSpecs.map(marshal(_))
    val ictx = createNodeListNode("innerComputations", inners)
    createCompoundNode("sequentialComputation", Map.empty, List(ictx))
  }

  private def mappingComputationSpec(computation: MappingComputationSpecification) = {
    val attrs = Map("resultKey" -> computation.resultKey)
    val inputTupeCtx = tuple("inputTuple", computation.inputTuple)
    val innerCompCtx = createCompoundNode("innerComputation", Map(), List(marshal(computation.innerSpecification)))
    createCompoundNode("mappingComputation", attrs, List(inputTupeCtx, innerCompCtx))
  }

  private def foldingComputationSpec(computation: FoldingComputationSpecification) = {
    val attrs = Map("initialAccumulatorKey" -> computation.initialAccumulatorKey)
    val inputTupelCtx = tuple("inputTuple", computation.inputTuple)
    val accumulatorTupleCtx = tuple("accumulatorTuple", computation.accumulatorTuple)
    val innerCompCtx = createCompoundNode("innerComputation", Map(), List(marshal(computation.innerSpecification)))
    createCompoundNode("foldingComputation", attrs, List(inputTupelCtx, accumulatorTupleCtx, innerCompCtx))
  }

  private def tuple(label: String, mapping: Mapping): Node = {
    createCompoundNode(label, Map.empty, List(marshal(mapping)))
  }

  private def ref(ref: Ref) = {
    createMapNode(Map("ref" -> ref.referencedSpecification))
  }

  private def inputs(inputs: Inputs) = {
    val map = inputs.inputMappings.map(x => marshal(x)).toList
    createNodeListNode("inputs", map)
  }

  protected def mapping(mapping: Mapping) = {
    createMapNode(Map(mapping.key -> mapping.value))
  }

  protected def imports(imports: Imports) = {
    val s = imports.importSequence
    createStringListNode("imports", s.toList)
  }

  protected def createCompoundNode(label: String, attrs: Map[String, String], children: List[Node]): Node = {
    CompoundNode(label, attrs, children)
  }

  protected def createStringListNode(label: String, children: List[String]): Node = {
    StringListNode(label, children)
  }

  protected def createMapNode(children: Map[String, String]): Node = {
    MapKeyValueNode(children)
  }

  protected def createNodeListNode(label: String, children: List[Node]): Node = {
    NodeListNode(label, children)
  }

  protected def dateTime(d: DateTime): String
}
