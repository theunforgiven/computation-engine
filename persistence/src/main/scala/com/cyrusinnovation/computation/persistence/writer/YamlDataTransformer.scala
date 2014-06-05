package com.cyrusinnovation.computation.persistence.writer
import java.util.{List => JList, Map => JMap, HashMap => JHMap}
import scala.collection.JavaConverters._

import com.cyrusinnovation.computation.persistence.writer.LibraryExtractor._

object YamlDataTransformer {
  def convertNodeToSnakeYamlMaps(nodeContext: Node): JList[JMap[Object, Object]] = {
    val context = nodeContext.asInstanceOf[EntryNode]
    val flattenedLibrary = context.copy(children = List())
    val ver = context.children.head.asInstanceOf[EntryNode]
    val verWithOutKids = ver.copy(children = List())
    (List(flattenedLibrary, verWithOutKids) ++ ver.children).map(extract).asJava
  }

  private def extract(node: Node): JMap[Object, Object] = {
    val map = new JHMap[Object, Object]
    node match {
      case aNode: EntryNode                  => {
        val children = new JHMap[Object, Object]
        map.put(aNode.label, children)
        aNode.attrs.foreach(x => children.put(x._1, x._2))
        aNode.children.map(extract).foreach(x => children.putAll(x))
        map
      }
      case aContextNodeList: NodeListNode => {
        map.put(aContextNodeList.label, aContextNodeList.children.map(extract).asJava)
        map
      }
      case aNodeList: StringListNode               => {
        map.put(aNodeList.label, aNodeList.children.asJava)
        map
      }
      case aMapNode: MapKeyValueNode => {
        aMapNode.children.foreach(x => map.put(x._1, x._2))
        map
      }
    }
  }
}
