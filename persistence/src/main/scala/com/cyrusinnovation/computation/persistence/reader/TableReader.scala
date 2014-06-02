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
case class TableNodeContext(defaultAttributes: Map[String, String])

class TableReader(nodeTable: Map[Long, Map[String, String]], edgeTable: Map[Long, List[Long]]) extends Reader {
  private type NodeContext = TableNodeContext

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

  private def constructChildNodeMap(childNodes: List[Long]): Map[String, List[TableNode]] = {
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

  def unmarshal: Library = unmarshal(rootNode, TableNodeContext(Map.empty)).asInstanceOf[Library]

  def unmarshal(node: TableNode, context: NodeContext) : SyntaxTreeNode = node.label match {
    case "library" => Library(attrValue(node, "name", context), versionMap(node, context))
    case "version" => version(node, context)
    case "computations" => throw new RuntimeException("computations node should not be unmarshaled directly")
    case "simpleComputation" => simpleComputationFactory(node, context)
    case "abortIfComputation" => abortIfComputationFactory(node, context)
    case "namedComputation" => namedComputation(node, context)
    case "abortIfNoResultsComputation" => abortIfNoResultsComputation(node, context)
    case "abortIfHasResultsComputation" => abortIfNoResultsComputation(node, context)
    case "mappingComputation" => mappingComputation(node, context)
    case "iterativeComputation" => iterativeComputation(node, context)
    case "foldingComputation" => foldingComputation(node, context)
    case "sequentialComputation" => sequentialComputation(node, context)
    case "innerComputations" => throw new RuntimeException("innerComputations node should not be unmarshaled directly")
    case "innerComputation" => throw new RuntimeException("innerComputation node should not be unmarshaled directly")
    case "ref" => reference(node)
    case "imports" => imports(node)
    case "inputs" => inputs(node, context)
    case "inputTuple" => singleTuple(node, context)
    case "accumulatorTuple" => singleTuple(node, context)
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

  protected def versionMap(node: TableNode, context: NodeContext) : Map[String, Version] = {
    val versions = children(node, "version")
    versions.foldLeft(Map[String,Version]()) {
      (mapSoFar, versionNode) => {
        val version = unmarshal(versionNode, context).asInstanceOf[Version]
        mapSoFar + (version.versionNumber -> version)
      }
    }
  }

  protected def version(versionNode: TableNode, context: NodeContext) : Version = {
    val computationsNode = children(versionNode, "computations").head
    val topLevelComputations = children(computationsNode)
    val defaultContext = defaults(childOfTypeOpt(versionNode, "defaults"), context)
    Version(attrValue(versionNode, "versionNumber", context),
      versionState(attrValue(versionNode, "state", context)),
      optionalAttrValue(versionNode, "commitDate").map(timeString => dateTime(timeString)),
      optionalAttrValue(versionNode, "lastEditDate").map(timeString => dateTime(timeString)),
      unmarshal(topLevelComputations.head, defaultContext).asInstanceOf[TopLevelComputationSpecification],
      topLevelComputations.tail.map(computationNode => unmarshal(computationNode, defaultContext).asInstanceOf[TopLevelComputationSpecification]):_*
    )
  }

  private def defaults(defaultsNode: Option[TableNode], context: NodeContext): NodeContext = {
    defaultsNode match {
      case Some(nodeWithDefaults: TableNode) =>
        TableNodeContext(nodeWithDefaults.asInstanceOf[InternalTableNode].attributes)
      case None                              => context
    }
  }

  protected def versionState(stateString: String) : VersionState = {
    VersionState.fromString(stateString)
  }

  protected def simpleComputationFactory(node: TableNode, context: NodeContext) : SimpleComputationSpecification = {
    SimpleComputationSpecification(
      attrValue(node, "package", context),
      attrValue(node, "name", context),
      attrValue(node, "description", context),
      attrValue(node, "changedInVersion", context),
      attrValue(node, "shouldPropagateExceptions", context).toBoolean,
      attrValue(node, "computationExpression", context),
      unmarshal(childOfType(node, "imports"), context).asInstanceOf[Imports],
      unmarshal(childOfType(node, "inputs"), context).asInstanceOf[Inputs],
      attrValue(node, "resultKey", context),
      attrValue(node, "logger", context),
      attrValue(node, "securityConfiguration", context)
    )
  }

  protected def abortIfComputationFactory(node: TableNode, context: NodeContext) : AbortIfComputationSpecification = {
    AbortIfComputationSpecification(
      attrValue(node, "package", context),
      attrValue(node, "name", context),
      attrValue(node, "description", context),
      attrValue(node, "changedInVersion", context),
      attrValue(node, "shouldPropagateExceptions", context).toBoolean,
      attrValue(node, "predicateExpression", context),
      extractInnerComputationFrom(childOfType(node, "innerComputation"), context),
      unmarshal(childOfType(node, "imports"), context).asInstanceOf[Imports],
      unmarshal(childOfType(node, "inputs"), context).asInstanceOf[Inputs],
      attrValue(node, "logger", context),
      attrValue(node, "securityConfiguration", context)
    )
  }

  protected def namedComputation(node: TableNode, context: NodeContext) : NamedComputationSpecification = {
    NamedComputationSpecification(
      attrValue(node, "package", context),
      attrValue(node, "name", context),
      attrValue(node, "description", context),
      attrValue(node, "changedInVersion", context),
      unmarshal(child(node), context).asInstanceOf[NamableComputationSpecification]
    )
  }

  protected def abortIfNoResultsComputation(node: TableNode, context: NodeContext) : AbortIfNoResultsComputationSpecification = {
    AbortIfNoResultsComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation"), context)
    )
  }

  protected def abortIfHasResultsComputation(node: TableNode, context: NodeContext) : AbortIfHasResultsComputationSpecification = {
    AbortIfHasResultsComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation"), context)
    )
  }

  protected def mappingComputation(node: TableNode, context: NodeContext) : MappingComputationSpecification = {
    MappingComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation"), context),
      unmarshal(childOfType(node, "inputTuple"), context).asInstanceOf[Mapping],
      attrValue(node, "resultKey", context)
    )
  }

  protected def iterativeComputation(node: TableNode, context: NodeContext) : IterativeComputationSpecification = {
    IterativeComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation"), context),
      unmarshal(childOfType(node, "inputTuple"), context).asInstanceOf[Mapping],
      attrValue(node, "resultKey", context)
    )
  }

  protected def foldingComputation(node: TableNode, context: NodeContext) : FoldingComputationSpecification = {
    FoldingComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation"), context),
      attrValue(node, "initialAccumulatorKey", context),
      unmarshal(childOfType(node, "inputTuple"), context).asInstanceOf[Mapping],
      unmarshal(childOfType(node, "accumulatorTuple"), context).asInstanceOf[Mapping]
    )
  }

  protected def sequentialComputation(node: TableNode, context: NodeContext) : SequentialComputationSpecification = {
    val innerComputationsNode = childOfType(node, "innerComputations")
    val innerComputations = children(innerComputationsNode).map(x => extractInnerComputationFrom(x, context))

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

  protected def inputs(node: TableNode, context: NodeContext) : Inputs = {
    val nodes: List[Mapping] = children(node, "mapping").map(x => unmarshal(x, context).asInstanceOf[Mapping])
    Inputs(nodes.head, nodes.tail:_*)
  }

  protected def mapping(node: TableNode) : Mapping =  {
    Mapping(
      unmarshalToString(childOfType(node, "key")),
      unmarshalToString(childOfType(node, "value"))
    )
  }

  protected def singleTuple(node: TableNode, context: NodeContext) : Mapping = {
    unmarshal(childOfType(node, "mapping"), context).asInstanceOf[Mapping]
  }

  protected def extractInnerComputationFrom(innerComputationNode: TableNode, context: NodeContext) : InnerComputationSpecification = {
    assert(children(innerComputationNode).size == 1)
    val innerComputation = children(innerComputationNode).head
    unmarshal(innerComputation, context).asInstanceOf[InnerComputationSpecification]
  }

  protected def attrValue(node: TableNode, key: String, context: NodeContext): String = {
    node.asInstanceOf[InternalTableNode].attributes.get(key) match {
      case Some(value: String) => value
      case None => context.defaultAttributes.get(key) match {
        case Some(value: String) => value
        case None => throw new RuntimeException(s"Required attribute value $key could not be found.")
      }
    }
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

  protected def childOfType(DbPersistentNode: TableNode, label: String): TableNode = {
    childOfTypeOpt(DbPersistentNode, label) match {
      case Some(n: TableNode) => n
      case None               => throw new RuntimeException(s"Required element $label could not be found.")
    }
  }

  protected def childOfTypeOpt(DbPersistentNode: TableNode, label: String): Option[TableNode] = {
    children(DbPersistentNode, label).headOption
  }

}