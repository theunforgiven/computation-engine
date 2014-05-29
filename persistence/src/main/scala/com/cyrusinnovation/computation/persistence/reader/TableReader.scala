package com.cyrusinnovation.computation.persistence.reader

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import scala.Some
import com.cyrusinnovation.computation.specification._

trait TableNode  {
  def label : String
}

trait TableTextNode extends TableNode {
  def text : String
}

case class InternalTableNode(id: Long, label: String, attributes: Map[String, String], children: Map[String, List[TableNode]]) extends TableNode
case class TextBearingTableNode(id: Long, label: String, text: String) extends TableTextNode

class TableReader(nodeTable: Map[Long, Map[String, String]], edgeTable: Map[Long, List[Long]]) extends Reader {
  val rootNode: TableNode = constructNodeFromNodeId(findLibraryNodeId)
  def findLibraryNodeId: Long = {
    nodeTable.filter(nodeIdToAttributeMap => nodeIdToAttributeMap._2("label") == "library").keys.head //There should be only one
  }

  private def constructNodeFromNodeId(nodeId: Long) : TableNode = {
    val attributesForThisNode = nodeTable(nodeId)
    val labelForThisNode = attributesForThisNode("label")
    val childNodeMap = childNodeMapFor(nodeId)

    attributesForThisNode.get("text") match {
      case Some(text) => TextBearingTableNode(nodeId, labelForThisNode, text)
      case None => InternalTableNode(nodeId, labelForThisNode, attributesForThisNode, childNodeMap)
    }
  }

  private def childNodeMapFor(nodeId: Long): Map[String, List[TableNode]] = {
    edgeTable.get(nodeId) match {
      case None => Map()
      case Some(childNodes) => constructChildNodeMap(childNodes)
    }
  }

  def constructChildNodeMap(childNodes: List[Long]): Map[String, List[TableNode]] = {
    val childNodesWithLabels = childNodes.map(nodeId => (nodeId, nodeTable(nodeId)("label")))

    val initialAccumulator: Map[String, List[TableNode]] = Map()

    val labelToReversedNodeList = childNodesWithLabels.foldLeft(initialAccumulator) {
      (mapSoFar, nodeIdWithLabel) => {
        mapSoFar.get(nodeIdWithLabel._2) match {
          case None => mapSoFar + (nodeIdWithLabel._2 -> List(constructNodeFromNodeId(nodeIdWithLabel._1)))
          case Some(nodesWithSameLabel) => mapSoFar + (nodeIdWithLabel._2 -> (constructNodeFromNodeId(nodeIdWithLabel._1) :: nodesWithSameLabel))
        }
      }
    }

    labelToReversedNodeList.map(keyValuePair => {
      keyValuePair._1 -> keyValuePair._2.reverse
    })
  }

  def unmarshal: Library = unmarshal(rootNode).asInstanceOf[Library]

  def unmarshal(node: TableNode) : SyntaxTreeNode = node.label match {
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

  def versionMap(node: TableNode) : Map[String, Version] = {
    val versions = children(node, "version")
    versions.foldLeft(Map[String,Version]()) {
      (mapSoFar, versionNode) => {
        val version = unmarshal(versionNode).asInstanceOf[Version]
        mapSoFar + (version.versionNumber -> version)
      }
    }
  }

  def version(versionNode: TableNode) : Version = {
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

  protected def simpleComputationFactory(node: TableNode) : SimpleComputationSpecification = {
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

  protected def abortIfComputationFactory(node: TableNode) : AbortIfComputationSpecification = {
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

  protected def namedComputation(node: TableNode) : NamedComputationSpecification = {
    NamedComputationSpecification(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      unmarshal(child(node)).asInstanceOf[NamableComputationSpecification]
    )
  }

  protected def abortIfNoResultsComputation(node: TableNode) : AbortIfNoResultsComputationSpecification = {
    AbortIfNoResultsComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation"))
    )
  }

  protected def abortIfHasResultsComputation(node: TableNode) : AbortIfHasResultsComputationSpecification = {
    AbortIfHasResultsComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation"))
    )
  }

  protected def mappingComputation(node: TableNode) : MappingComputationSpecification = {
    MappingComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshalToString(childOfType(node, "resultKey"))
    )
  }

  protected def iterativeComputation(node: TableNode) : IterativeComputationSpecification = {
    IterativeComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshalToString(childOfType(node, "resultKey"))
    )
  }

  protected def foldingComputation(node: TableNode) : FoldingComputationSpecification = {
    FoldingComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshalToString(childOfType(node, "initialAccumulatorKey")),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshal(childOfType(node, "accumulatorTuple")).asInstanceOf[Mapping]
    )
  }

  protected def sequentialComputation(node: TableNode) : SequentialComputationSpecification = {
    val innerComputationsNode = childOfType(node, "innerComputations")
    val innerComputations = children(innerComputationsNode).map(x => extractInnerComputationFrom(x))

    SequentialComputationSpecification (
      innerComputations.head,
      innerComputations.tail:_*
    )
  }

  protected def reference(node: TableNode) : Ref = {
    new Ref(unmarshalToString(node))
  }

  protected def imports(node: TableNode) : Imports = {
    val importStrings = children(node, "import").map(x => unmarshalToString(x))
    Imports(importStrings:_*)
  }

  protected def inputs(node: TableNode) : Inputs = {
    val nodes: List[Mapping] = children(node, "mapping").map(x => unmarshal(x).asInstanceOf[Mapping])
    Inputs(nodes.head, nodes.tail:_*)
  }

  protected def mapping(node: TableNode) : Mapping =  {
    Mapping(
      unmarshalToString(childOfType(node, "key")),
      unmarshalToString(childOfType(node, "value"))
    )
  }

  protected def singleTuple(node: TableNode) : Mapping = {
    unmarshal(childOfType(node, "mapping")).asInstanceOf[Mapping]
  }

  protected def extractInnerComputationFrom(innerComputationNode: TableNode) : InnerComputationSpecification = {
    assert(children(innerComputationNode).size == 1)
    val innerComputation = children(innerComputationNode).head
    unmarshal(innerComputation).asInstanceOf[InnerComputationSpecification]
  }

  protected def attrValue(node: TableNode, key: String): String = {
    node.asInstanceOf[InternalTableNode].attributes(key)
  }

  protected def optionalAttrValue(node: TableNode, key: String): Option[String] = {
    node.asInstanceOf[InternalTableNode].attributes.get(key)
  }

  protected def children(node: TableNode): List[TableNode] = {
    node.asInstanceOf[InternalTableNode].children.values.flatten.toList
  }

  protected def children(node: TableNode, label: String): List[TableNode] = {
    node.asInstanceOf[InternalTableNode].children.get(label) match {
      case Some(list) => list
      case None => List()
    }
  }

  protected def asTextBearingNode(node: TableNode): TableTextNode = {
    node.asInstanceOf[TextBearingTableNode]
  }

  protected def dateTime(timeString: String): DateTime = {
    val formatter = ISODateTimeFormat.dateTimeParser()
    formatter.parseDateTime(timeString)
  }

  protected def unmarshalToString(DbPersistentNode: TableNode) : String = {
    asTextBearingNode(DbPersistentNode).text
  }

  protected def child(DbPersistentNode: TableNode) : TableNode = {
    children(DbPersistentNode).head
  }

  protected def childOfType(DbPersistentNode: TableNode, label: String) : TableNode = {
    children(DbPersistentNode, label).head
  }
}