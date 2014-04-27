package com.cyrusinnovation.computation.db.reader

import scala.xml.{Elem, Node, NodeSeq}
import com.cyrusinnovation.computation.db._
import javax.xml.bind.DatatypeConverter
import com.cyrusinnovation.computation.db.Version
import com.cyrusinnovation.computation.db.Library
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

class XmlPersistentNode(val xmlNode: Node) extends PersistentNode {
  def label = xmlNode.label
}

class XmlPersistentTextBearingNode(override val xmlNode: Node) extends XmlPersistentNode(xmlNode) with PersistentTextBearingNode {
  val text = xmlNode.text
}

class XmlReader(root: Elem) extends Reader {
  val rootNode = new XmlPersistentNode(root)

  def attrValue(persistentNode: PersistentNode, key: String) : String = {
    toXmlNode(persistentNode).attribute(key).get.text
  }

  def optionalAttrValue(persistentNode: PersistentNode, key: String): Option[String] = {
    toXmlNode(persistentNode).attribute(key).map(x => x.text)
  }

  def children(persistentNode: PersistentNode) : List[PersistentNode] = {
    val nodeSeq = toXmlNode(persistentNode) \ "_"
    toPersistentNodeList(nodeSeq)
  }

  def children(persistentNode: PersistentNode, label: String) : List[PersistentNode] = {
    val nodeSeq = toXmlNode(persistentNode) \ label
    toPersistentNodeList(nodeSeq)
  }

  def allChildren(persistentNode: PersistentNode) : List[PersistentNode] = {
    val nodeSeq = toXmlNode(persistentNode) \ "_"
    toPersistentNodeList(nodeSeq)
  }

  def asTextBearingNode(persistentNode: PersistentNode) : PersistentTextBearingNode = {
    val xmlNode = toXmlNode(persistentNode)
    new XmlPersistentTextBearingNode(xmlNode)
  }
  
  def dateTime(timeString: String): DateTime = {
    val formatter = ISODateTimeFormat.dateTimeParser()
    formatter.parseDateTime(timeString)
  }

  private def toXmlNode(persistentNode: PersistentNode) = persistentNode.asInstanceOf[XmlPersistentNode].xmlNode

  private def toPersistentNodeList(nodeSeq: NodeSeq) : List[PersistentNode] = {
    nodeSeq.map(node => new XmlPersistentNode(node)).toList
  }
}