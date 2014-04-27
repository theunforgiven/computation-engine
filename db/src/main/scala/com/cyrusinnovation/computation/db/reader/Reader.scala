package com.cyrusinnovation.computation.db.reader

import com.cyrusinnovation.computation.db._
import com.cyrusinnovation.computation.db.Imports
import com.cyrusinnovation.computation.db.Mapping
import com.cyrusinnovation.computation.db.AbortIfHasResultsComputationFactory
import com.cyrusinnovation.computation.db.NamedComputationFactory
import com.cyrusinnovation.computation.db.MappingComputationFactory
import com.cyrusinnovation.computation.db.Ref
import com.cyrusinnovation.computation.db.AbortIfComputationFactory
import com.cyrusinnovation.computation.db.AbortIfNoResultsComputationFactory
import com.cyrusinnovation.computation.db.SimpleComputationFactory
import com.cyrusinnovation.computation.db.IterativeComputationFactory
import com.cyrusinnovation.computation.db.Version
import com.cyrusinnovation.computation.db.Library
import com.cyrusinnovation.computation.db.SequentialComputationFactory
import com.cyrusinnovation.computation.db.FoldingComputationFactory
import com.cyrusinnovation.computation.db.Inputs
import org.joda.time.DateTime

trait PersistentNode {
  def label : String
}

trait PersistentTextBearingNode extends PersistentNode {
  val text : String
}

trait Reader {
  val rootNode : PersistentNode
  def unmarshal: Library = unmarshal(rootNode).asInstanceOf[Library]

  protected def unmarshal(node: PersistentNode) : AstNode = node.label match {
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
    case "input" => input(node)
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

  protected def versionMap(node: PersistentNode) : Map[String, Version] = {
    val versions = children(node, "version")
    versions.foldLeft(Map[String,Version]()) {
      (mapSoFar, versionNode) => {
        val version = unmarshal(versionNode).asInstanceOf[Version]
        mapSoFar + (version.versionNumber -> version)
      }
    }
  }

  protected def version(versionNode: PersistentNode) : Version = {
    val computationsNode = children(versionNode, "computations").head
    val topLevelComputations = allChildren(computationsNode)
    Version(attrValue(versionNode, "versionNumber"),
            versionState(attrValue(versionNode, "state")),
            optionalAttrValue(versionNode, "commitDate").map(timeString => dateTime(timeString)),
            optionalAttrValue(versionNode, "lastEditDate").map(timeString => dateTime(timeString)),
            unmarshal(topLevelComputations.head).asInstanceOf[TopLevelComputationFactory],
            topLevelComputations.tail.map(computationNode => unmarshal(computationNode).asInstanceOf[TopLevelComputationFactory]):_*
    )
  }

  protected def versionState(stateString: String) : VersionState = {
    VersionState.fromString(stateString)
  }

  protected def simpleComputationFactory(node: PersistentNode) : SimpleComputationFactory = {
    SimpleComputationFactory(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      attrValue(node, "shouldPropagateExceptions").toBoolean,
      unmarshalToString(childOfType(node, "computationExpression")),
      unmarshal(childOfType(node, "imports")).asInstanceOf[Imports],
      unmarshal(childOfType(node, "input")).asInstanceOf[Inputs],
      unmarshalToString(childOfType(node, "resultKey")),
      unmarshalToString(childOfType(node, "logger")),
      unmarshalToString(childOfType(node, "securityConfiguration"))
    )
  }

  protected def abortIfComputationFactory(node: PersistentNode) : AbortIfComputationFactory = {
    AbortIfComputationFactory(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      attrValue(node, "shouldPropagateExceptions").toBoolean,
      unmarshalToString(childOfType(node, "predicateExpression")),
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshal(childOfType(node, "imports")).asInstanceOf[Imports],
      unmarshal(childOfType(node, "input")).asInstanceOf[Inputs],
      unmarshalToString(childOfType(node, "logger")),
      unmarshalToString(childOfType(node, "securityConfiguration"))
    )
  }

  protected def namedComputation(node: PersistentNode) : NamedComputationFactory = {
    NamedComputationFactory(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      unmarshal(child(node)).asInstanceOf[NamableComputationFactory]
    )
  }

  protected def abortIfNoResultsComputation(node: PersistentNode) : AbortIfNoResultsComputationFactory = {
    AbortIfNoResultsComputationFactory(
      extractInnerComputationFrom(childOfType(node, "innerComputation"))
    )
  }

  protected def abortIfHasResultsComputation(node: PersistentNode) : AbortIfHasResultsComputationFactory = {
    AbortIfHasResultsComputationFactory(
      extractInnerComputationFrom(childOfType(node, "innerComputation"))
    )
  }

  protected def mappingComputation(node: PersistentNode) : MappingComputationFactory = {
    MappingComputationFactory(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshalToString(childOfType(node, "resultKey"))
    )
  }

  protected def iterativeComputation(node: PersistentNode) : IterativeComputationFactory = {
    IterativeComputationFactory(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshalToString(childOfType(node, "resultKey"))
    )
  }

  protected def foldingComputation(node: PersistentNode) : FoldingComputationFactory = {
    FoldingComputationFactory(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshalToString(childOfType(node, "initialAccumulatorKey")),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshal(childOfType(node, "accumulatorTuple")).asInstanceOf[Mapping]
    )
  }

  protected def sequentialComputation(node: PersistentNode) : SequentialComputationFactory = {
    val innerComputationsNode = childOfType(node, "innerComputations")
    val innerComputations = allChildren(innerComputationsNode).map(x => extractInnerComputationFrom(x))

    SequentialComputationFactory (
      innerComputations.head,
      innerComputations.tail:_*
    )
  }

  protected def reference(node: PersistentNode) : Ref = {
    Ref(unmarshalToString(node))
  }

  protected def imports(node: PersistentNode) : Imports = {
    val importStrings = children(node, "import").map(x => unmarshalToString(x))
    Imports(importStrings:_*)
  }

  protected def input(node: PersistentNode) : Inputs = {
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

  protected def extractInnerComputationFrom(innerComputationNode: PersistentNode) : InnerComputationFactory = {
    assert(allChildren(innerComputationNode).size == 1)
    val innerComputation = allChildren(innerComputationNode).head
    unmarshal(innerComputation).asInstanceOf[InnerComputationFactory]
  }

  protected def attrValue(node: PersistentNode, key: String) : String
  protected def optionalAttrValue(node: PersistentNode, key: String): Option[String]
  protected def children(node: PersistentNode) : List[PersistentNode]
  protected def children(node: PersistentNode, label: String) : List[PersistentNode]
  protected def allChildren(node: PersistentNode) : List[PersistentNode]
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
