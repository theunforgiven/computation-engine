package com.cyrusinnovation.computation.persistence.reader

import java.io.{File, FileInputStream, InputStream}
import java.net.URI
import org.yaml.snakeyaml.{Yaml, external}
import java.lang.Iterable
import org.joda.time.DateTime
import scala.collection.JavaConversions._
import java.util


object YamlReader {
  def fromFileOnClasspath(resourcePath: String) : Reader = {
    val inputStream: InputStream = getClass.getResourceAsStream(resourcePath)
    fromInputStream(inputStream)
  }

  def fromFile(path: String) : Reader = {
    val inputStream: InputStream = new FileInputStream(new File(path))
    fromInputStream(inputStream)
  }

  def fromFileUri(uri: URI) : Reader = {
    val inputStream: InputStream = new FileInputStream(new File(uri))
    fromInputStream(inputStream)
  }

  def fromInputStream(inputStream: InputStream) : Reader = {
    val snakeYaml = new Yaml()
    val data: Iterable[AnyRef] = snakeYaml.loadAll(inputStream);

    new YamlReader(data)
  }
}

class YamlReader(yamlData: Iterable[AnyRef]) extends Reader {

  val rootNode: PersistentNode = loadNodes(yamlData.toList)

  protected def attrValue(node: PersistentNode, key: String): String = ???

  protected def optionalAttrValue(node: PersistentNode, key: String): Option[String] = ???

  protected def children(node: PersistentNode): List[PersistentNode] = ???

  protected def children(node: PersistentNode, label: String): List[PersistentNode] = ???

  protected def asTextBearingNode(node: PersistentNode): PersistentTextBearingNode = ???

  protected def dateTime(timeString: String): DateTime = ???

  protected def loadNodes(yamlData: Iterable[AnyRef]) : PersistentNode = {
    val yamlSequence = yamlData.toList.head.asInstanceOf[java.util.List[java.util.Map[String, AnyRef]]]
    val libraryEntry = removeEntryLabeled("library", yamlSequence)
    val versionEntry = removeEntryLabeled("version", yamlSequence)

    putVersionUnderLibrary(libraryEntry, versionEntry)
    putComputationsUnderVersion(versionEntry, yamlSequence.toList)

    constructNodeFromNodeMap(libraryEntry)
  }

  def removeEntryLabeled(label: String, yamlSequence: java.util.List[java.util.Map[String, AnyRef]]): java.util.Map[String, AnyRef] = {
    val indexOfElementContainingLabel: Int = yamlSequence.indexWhere(element => element.keys.contains(label))
    yamlSequence.remove(indexOfElementContainingLabel)
  }

  private def putVersionUnderLibrary(library: java.util.Map[String, AnyRef],
                                     version: java.util.Map[String, AnyRef]) : Unit = {
    library.put("version", version)
  }

  private def putComputationsUnderVersion(version: java.util.Map[String, AnyRef],
                                          yamlData: List[AnyRef]) : Unit = {
    version.put("computations", yamlData)
  }

  private def constructNodeFromNodeMap(nodeMap: java.util.Map[String, AnyRef]) : YamlPersistentNode = {
    val attributeMap = nodeMap.foldLeft(Map[String, String]()) {
      (mapSoFar, mapEntry) => if(mapEntry._2.isInstanceOf[String]) {
        mapSoFar + (mapEntry._1 -> mapEntry._2.asInstanceOf[String])
        nodeMap.remove(mapEntry._1)
        mapSoFar
      } else { mapSoFar }
    }
    if(nodeMap.isEmpty && attributeMap.contains("text")) {
      YamlPersistentTextBearingNode(attributeMap("label"), attributeMap("text"))
    } else {
      YamlPersistentInternalNode(attributeMap("label"), attributeMap, childNodesFor(nodeMap))
    }
  }
  
  private def childNodesFor(nodeMap: java.util.Map[String, AnyRef]) : Map[String, List[YamlPersistentNode]] = {
    val childMap = nodeMap.asInstanceOf[Map[String, List[Map[String, AnyRef]]]]
    childMap.map {
      childLabelToListOfChildren => childLabelToListOfChildren._1 -> childLabelToListOfChildren._2.map(child => constructNodeFromNodeMap(child))
    }
  }
}

trait YamlPersistentNode extends PersistentNode
case class YamlPersistentInternalNode(label: String, attributes: Map[String, String], children: Map[String, List[YamlPersistentNode]]) extends YamlPersistentNode
case class YamlPersistentTextBearingNode(label: String, text: String) extends PersistentTextBearingNode with YamlPersistentNode
