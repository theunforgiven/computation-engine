package com.cyrusinnovation.computation.persistence.reader

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

trait DbPersistentNode extends PersistentNode
case class DbPersistentInternalNode(id: Long, label: String, attributes: Map[String, String], children: Map[String, List[DbPersistentNode]]) extends DbPersistentNode
case class DbPersistentTextBearingNode(id: Long, label: String, text: String) extends PersistentTextBearingNode with DbPersistentNode

class TableReader(nodeTable: Map[Long, Map[String, String]], edgeTable: Map[Long, List[Long]]) extends Reader {

  val rootNode: PersistentNode = constructNodeFromNodeId(findLibraryNodeId)

  protected def attrValue(node: PersistentNode, key: String): String = {
    node.asInstanceOf[DbPersistentInternalNode].attributes(key)
  }
  protected def attrValues(node: PersistentNode): Map[String, String] = {
    throw new NotImplementedError()
  }

  protected def optionalAttrValue(node: PersistentNode, key: String): Option[String] = {
    node.asInstanceOf[DbPersistentInternalNode].attributes.get(key)
  }

  protected def children(node: PersistentNode): List[PersistentNode] = {
    node.asInstanceOf[DbPersistentInternalNode].children.values.flatten.toList
  }

  protected def children(node: PersistentNode, label: String): List[PersistentNode] = {
    node.asInstanceOf[DbPersistentInternalNode].children.get(label) match {
      case Some(list) => list
      case None => List()
    }
  }

  protected def asTextBearingNode(node: PersistentNode): PersistentTextBearingNode = {
    node.asInstanceOf[DbPersistentTextBearingNode]
  }

  protected def dateTime(timeString: String): DateTime = {
    val formatter = ISODateTimeFormat.dateTimeParser()
    formatter.parseDateTime(timeString)
  }
  
  def findLibraryNodeId: Long = {
    nodeTable.filter(nodeIdToAttributeMap => nodeIdToAttributeMap._2("label") == "library").keys.head //There should be only one
  }
  
  private def constructNodeFromNodeId(nodeId: Long) : DbPersistentNode = {
    val attributesForThisNode = nodeTable(nodeId)
    val labelForThisNode = attributesForThisNode("label")
    val childNodeMap = childNodeMapFor(nodeId)

    attributesForThisNode.get("text") match {
      case Some(text) => DbPersistentTextBearingNode(nodeId, labelForThisNode, text)
      case None => DbPersistentInternalNode(nodeId, labelForThisNode, attributesForThisNode, childNodeMap)
    }
  }

  private def childNodeMapFor(nodeId: Long): Map[String, List[DbPersistentNode]] = {
    edgeTable.get(nodeId) match {
      case None => Map()
      case Some(childNodes) => constructChildNodeMap(childNodes)
    }
  }

  def constructChildNodeMap(childNodes: List[Long]): Map[String, List[DbPersistentNode]] = {
    val childNodesWithLabels = childNodes.map(nodeId => (nodeId, nodeTable(nodeId)("label")))

    val initialAccumulator: Map[String, List[DbPersistentNode]] = Map()

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
}