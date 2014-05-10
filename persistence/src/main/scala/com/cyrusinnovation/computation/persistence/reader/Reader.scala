package com.cyrusinnovation.computation.persistence.reader

import org.joda.time.DateTime
import com.cyrusinnovation.computation.specification._
import com.cyrusinnovation.computation.specification.Inputs
import com.cyrusinnovation.computation.specification.Imports
import com.cyrusinnovation.computation.specification.Mapping
import com.cyrusinnovation.computation.specification.MappingComputationSpecification
import com.cyrusinnovation.computation.specification.AbortIfNoResultsComputationSpecification
import com.cyrusinnovation.computation.specification.IterativeComputationSpecification
import com.cyrusinnovation.computation.specification.AbortIfHasResultsComputationSpecification
import com.cyrusinnovation.computation.specification.NamedComputationSpecification
import com.cyrusinnovation.computation.specification.AbortIfComputationSpecification
import com.cyrusinnovation.computation.specification.Version
import com.cyrusinnovation.computation.specification.Library
import com.cyrusinnovation.computation.specification.SequentialComputationSpecification
import com.cyrusinnovation.computation.specification.FoldingComputationSpecification
import com.cyrusinnovation.computation.specification.SimpleComputationSpecification

trait PersistentNode {
  def label : String
}

trait PersistentTextBearingNode extends PersistentNode {
  val text : String
}

trait Reader {
  val rootNode : PersistentNode
  def unmarshal: Library = unmarshal(rootNode).asInstanceOf[Library]

  def unmarshal(node: PersistentNode) : SyntaxTreeNode = node.label match {
    case "library" => Library(attrValue(node, "name"), versionMap(node))
    case "version" => version(node)
    case "computations" => throw new RuntimeException("computations node should not be unmarshaled directly")
    case "simpleComputation" => simpleComputationFactory(node)
    case "abortIfComputation" => abortIfComputationFactory(node)
    case "namedComputation" => namedComputation(node)
    case "abortIfNoResultsComputation" => abortIfNoResultsComputation(node)
    case "abortIfHasResultsComputation" => abortIfNoResultsComputation(node)
    case "mappingComputation" => mappingComputation(node)
    case "iterativeComputation" => iterativeComputation(node)
    case "foldingComputation" => foldingComputation(node)
    case "sequentialComputation" => sequentialComputation(node)
    case "innerComputations" => throw new RuntimeException("innerComputations node should not be unmarshaled directly")
    case "innerComputation" => throw new RuntimeException("innerComputation node should not be unmarshaled directly")
    case "ref" => reference(node)
    case "imports" => imports(node)
    case "inputs" => inputs(node)
    case "inputTuple" => singleTuple(node)
    case "accumulatorTuple" => singleTuple(node)
    case "mapping" => mapping(node)
    case "key" => throw new RuntimeException("key node should not be unmarshaled to AstNode")
    case "value" => throw new RuntimeException("value node should not be unmarshaled to AstNode")
    case "initialAccumulatorKey" => throw new RuntimeException("initialAccumulatorKey node should not be unmarshaled to AstNode")
    case "resultKey" => throw new RuntimeException("resultKey node should not be unmarshaled to AstNode")
    case "computationExpression" => throw new RuntimeException("computationExpression node should not be unmarshaled to AstNode")
    case "predicateExpression" => throw new RuntimeException("predicateExpression node should not be unmarshaled to AstNode")
    case "logger" => throw new RuntimeException("logger node should not be unmarshaled to AstNode")
    case "securityConfiguration" => throw new RuntimeException("securityConfiguration node should not be unmarshaled to AstNode")
  }

  def versionMap(node: PersistentNode) : Map[String, Version] = {
    val versions = children(node, "version")
    versions.foldLeft(Map[String,Version]()) {
      (mapSoFar, versionNode) => {
        val version = unmarshal(versionNode).asInstanceOf[Version]
        mapSoFar + (version.versionNumber -> version)
      }
    }
  }

  def version(versionNode: PersistentNode) : Version = {
    val computationsNode = children(versionNode, "computations").head
    val topLevelComputations = children(computationsNode)
    Version(attrValue(versionNode, "versionNumber"),
            versionState(attrValue(versionNode, "state")),
            optionalAttrValue(versionNode, "commitDate").map(timeString => dateTime(timeString)),
            optionalAttrValue(versionNode, "lastEditDate").map(timeString => dateTime(timeString)),
            unmarshal(topLevelComputations.head).asInstanceOf[TopLevelComputationSpecification],
            topLevelComputations.tail.map(computationNode => unmarshal(computationNode).asInstanceOf[TopLevelComputationSpecification]):_*
    )
  }

  protected def versionState(stateString: String) : VersionState = {
    VersionState.fromString(stateString)
  }

  protected def simpleComputationFactory(node: PersistentNode) : SimpleComputationSpecification = {
    SimpleComputationSpecification(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      attrValue(node, "shouldPropagateExceptions").toBoolean,
      unmarshalToString(childOfType(node, "computationExpression")),
      unmarshal(childOfType(node, "imports")).asInstanceOf[Imports],
      unmarshal(childOfType(node, "inputs")).asInstanceOf[Inputs],
      unmarshalToString(childOfType(node, "resultKey")),
      unmarshalToString(childOfType(node, "logger")),
      unmarshalToString(childOfType(node, "securityConfiguration"))
    )
  }

  protected def abortIfComputationFactory(node: PersistentNode) : AbortIfComputationSpecification = {
    AbortIfComputationSpecification(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      attrValue(node, "shouldPropagateExceptions").toBoolean,
      unmarshalToString(childOfType(node, "predicateExpression")),
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshal(childOfType(node, "imports")).asInstanceOf[Imports],
      unmarshal(childOfType(node, "inputs")).asInstanceOf[Inputs],
      unmarshalToString(childOfType(node, "logger")),
      unmarshalToString(childOfType(node, "securityConfiguration"))
    )
  }

  protected def namedComputation(node: PersistentNode) : NamedComputationSpecification = {
    NamedComputationSpecification(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      unmarshal(child(node)).asInstanceOf[NamableComputationSpecification]
    )
  }

  protected def abortIfNoResultsComputation(node: PersistentNode) : AbortIfNoResultsComputationSpecification = {
    AbortIfNoResultsComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation"))
    )
  }

  protected def abortIfHasResultsComputation(node: PersistentNode) : AbortIfHasResultsComputationSpecification = {
    AbortIfHasResultsComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation"))
    )
  }

  protected def mappingComputation(node: PersistentNode) : MappingComputationSpecification = {
    MappingComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshalToString(childOfType(node, "resultKey"))
    )
  }

  protected def iterativeComputation(node: PersistentNode) : IterativeComputationSpecification = {
    IterativeComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshalToString(childOfType(node, "resultKey"))
    )
  }

  protected def foldingComputation(node: PersistentNode) : FoldingComputationSpecification = {
    FoldingComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshalToString(childOfType(node, "initialAccumulatorKey")),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshal(childOfType(node, "accumulatorTuple")).asInstanceOf[Mapping]
    )
  }

  protected def sequentialComputation(node: PersistentNode) : SequentialComputationSpecification = {
    val innerComputationsNode = childOfType(node, "innerComputations")
    val innerComputations = children(innerComputationsNode).map(x => extractInnerComputationFrom(x))

    SequentialComputationSpecification (
      innerComputations.head,
      innerComputations.tail:_*
    )
  }

  protected def reference(node: PersistentNode) : Ref = {
    new Ref(unmarshalToString(node))
  }

  protected def imports(node: PersistentNode) : Imports = {
    val importStrings = children(node, "import").map(x => unmarshalToString(x))
    Imports(importStrings:_*)
  }

  protected def inputs(node: PersistentNode) : Inputs = {
    val nodes: List[Mapping] = children(node, "mapping").map(x => unmarshal(x).asInstanceOf[Mapping])
    Inputs(nodes.head, nodes.tail:_*)
  }

  protected def mapping(node: PersistentNode) : Mapping =  {
    Mapping(
      unmarshalToString(childOfType(node, "key")),
      unmarshalToString(childOfType(node, "value"))
    )
  }

  protected def singleTuple(node: PersistentNode) : Mapping = {
    unmarshal(childOfType(node, "mapping")).asInstanceOf[Mapping]
  }

  protected def extractInnerComputationFrom(innerComputationNode: PersistentNode) : InnerComputationSpecification = {
    assert(children(innerComputationNode).size == 1)
    val innerComputation = children(innerComputationNode).head
    unmarshal(innerComputation).asInstanceOf[InnerComputationSpecification]
  }

  protected def attrValue(node: PersistentNode, key: String) : String
  protected def optionalAttrValue(node: PersistentNode, key: String): Option[String]
  protected def children(node: PersistentNode) : List[PersistentNode]
  protected def children(node: PersistentNode, label: String) : List[PersistentNode]
  protected def asTextBearingNode(node: PersistentNode) : PersistentTextBearingNode
  protected def dateTime(timeString: String): DateTime

  protected def unmarshalToString(persistentNode: PersistentNode) : String = {
    asTextBearingNode(persistentNode).text
  }

  protected def child(persistentNode: PersistentNode) : PersistentNode = {
    children(persistentNode).head
  }

  protected def childOfType(persistentNode: PersistentNode, label: String) : PersistentNode = {
    children(persistentNode, label).head
  }
}

