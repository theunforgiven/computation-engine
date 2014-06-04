package com.cyrusinnovation.computation.persistence.writer

import java.io.{OutputStreamWriter, OutputStream}
import org.yaml.snakeyaml.{DumperOptions, Yaml}
import java.util.{HashMap => JHMap}
import java.util.{Map => JMap}
import collection.JavaConverters._
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import Writer._

object YamlWriter {
  def forOutputStream(outputStream: OutputStream): Writer = {
    val opts = new DumperOptions()
    //When writing yaml: Use indents to denote objects instead of { }
    //This prevents small objects like library and version from being rolled up into one line
    opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
    new YamlWriter(outputStream, new Yaml(opts))
  }
}

class YamlWriter(stream: OutputStream, snakeYaml: Yaml) extends Writer {
  protected override def createNode(label: String, attrs: Map[String, String], children: List[Node]): Node = {
    EntryNode(label, attrs, children)
  }

  protected override def createStringListNode(label: String, children: List[String]): Node = {
    StringListNode(label, children)
  }

  protected override def createNodeListNode(label: String, children: List[Node]): Node = {
    NodeListNode(label, children)
  }

  protected override def createMapNode(label: String, children: Map[String, String]): Node = {
    MapNode(label, children)
  }

  protected override def dateTime(d: DateTime): String = {
    val formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
    formatter.format(d.toDate)
  }

  protected override def persist(nodeContext: Node) {
    val streamWriter = new OutputStreamWriter(stream)
    try {
      val context = nodeContext.asInstanceOf[EntryNode]
      val flattenedLibrary = context.copy(children = List())
      val ver = context.children.head.asInstanceOf[EntryNode]
      val verWithOutKids = ver.copy(children = List())
      val topLevelNodeList = (List(flattenedLibrary, verWithOutKids) ++ ver.children).map(extract).asJava
      snakeYaml.dump(topLevelNodeList, streamWriter)
    } finally {
      streamWriter.close()
    }
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
      case aMapNode: MapNode                 => {
        aMapNode.label match {
          case "" => {
            //A blank label means the children do not have a parent map
            //and should be added directly
            aMapNode.children.foreach(x => map.put(x._1, x._2))
          }
          case _  => map.put(aMapNode.label, aMapNode.children.asJava)
        }
        map
      }
    }
  }
}
