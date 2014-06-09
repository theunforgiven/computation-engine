package com.cyrusinnovation.computation.persistence.writer
import java.util.{List => JList, Map => JMap, HashMap => JHMap}
import scala.collection.JavaConverters._

import com.cyrusinnovation.computation.persistence.writer.LibraryInspector._

object YamlDataTransformer {
  def convertLibraryNodeToSnakeYamlMaps(libraryNode: Node): JList[JMap[Object, Object]] = {
    val context = libraryNode.asInstanceOf[CompoundNode]
    val flattenedLibrary = context.copy(children = List())
    val version = context.children.head.asInstanceOf[CompoundNode]
    val versionWithOutKids = version.copy(children = List())
    val topLevelYamlNodes = flattenedLibrary :: versionWithOutKids :: version.children
    topLevelYamlNodes.map(toYamlMap).asJava
  }

  private def toYamlMap(node: Node): JMap[Object, Object] = {
    val map = new JHMap[Object, Object]
    node match {
      case aNode: CompoundNode                  => {
        val children = new JHMap[Object, Object]
        map.put(aNode.label, children)
        aNode.attrs.foreach(x => children.put(x._1, x._2))
        aNode.children.map(toYamlMap).foreach(x => children.putAll(x))
        map
      }
      case aNodeList: NodeListNode => {
        map.put(aNodeList.label, aNodeList.children.map(toYamlMap).asJava)
        map
      }
      case aStringList: StringListNode               => {
        map.put(aStringList.label, aStringList.children.asJava)
        map
      }
      case aMapNode: MapKeyValueNode => {
        aMapNode.children.foreach(x => map.put(x._1, x._2))
        map
      }
    }
  }
}
