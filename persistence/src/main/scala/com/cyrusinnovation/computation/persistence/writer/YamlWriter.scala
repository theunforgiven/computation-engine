package com.cyrusinnovation.computation.persistence.writer

import java.io.{OutputStreamWriter, OutputStream}
import org.yaml.snakeyaml.{DumperOptions, Yaml}
import java.util.{HashMap => JHMap}
import java.util.{Map => JMap, List => JList}
import collection.JavaConverters._
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import Writer._
import java.util

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
  protected override def dateTime(d: DateTime): String = {
    val formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
    formatter.format(d.toDate)
  }

  protected override def persist(nodeContext: Node) {
    val streamWriter = new OutputStreamWriter(stream)
    try {
      snakeYaml.dump(convertNodeToSnakeYamlMaps(nodeContext), streamWriter)
    } finally {
      streamWriter.close()
    }
  }

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
