package com.cyrusinnovation.computation.persistence.reader

import java.io.{File, FileInputStream, InputStream}
import java.net.URI
import org.yaml.snakeyaml.Yaml
import java.lang.Iterable
import org.joda.time.DateTime
import scala.collection.JavaConversions._
import java.util.{Map => JavaMap, List => JavaList}
import java.text.SimpleDateFormat


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

  protected def attrValue(node: PersistentNode, key: String): String = {
    node match {
      case YamlPersistentTextBearingNode(label, text) => if(label == key) text else throw new RuntimeException(s"No such attribute ${key} for text-bearing node ${label}, ${text}")
      case internalNode : YamlPersistentInternalNode => internalNode.scalarValuedNodes(key).text
    }
  }

  protected def optionalAttrValue(node: PersistentNode, key: String): Option[String] = {
    node match {
      case YamlPersistentTextBearingNode(label, text) => if(label == key) Some(text) else None
      case internalNode : YamlPersistentInternalNode => internalNode.scalarValuedNodes.get(key).map(node => node.text)
    }
  }

  protected def children(node: PersistentNode): List[PersistentNode] = node match {
    case leaf : YamlPersistentTextBearingNode => List()
    case internalNode : YamlPersistentInternalNode => internalNode.children.values.flatten.toList
  }

  protected def children(node: PersistentNode, label: String): List[PersistentNode] = node match {
    case leaf : YamlPersistentTextBearingNode => List()
    case node : YamlPersistentInternalNode => node.children.getOrElse(label, List())
  }

  protected def asTextBearingNode(node: PersistentNode): PersistentTextBearingNode = node match {
    case leaf : YamlPersistentTextBearingNode => leaf
  }

  protected def dateTime(timeString: String): DateTime = {
    val date = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").parse(timeString)
    new DateTime(date)
  }

  protected def loadNodes(yamlData: Iterable[Any]) : PersistentNode = {
    val yamlSequence = yamlData.toList.head.asInstanceOf[JavaList[JavaMap[Any, Any]]]
    val libraryEntry = removeEntryLabeled("library", yamlSequence)
    val versionEntry = removeEntryLabeled("version", yamlSequence)

    val computationsNode = YamlPersistentInternalNode("computations", Map(), toComputationNodeMap(yamlSequence))
    
    val versionNode = nodeWithAddedChild("version", versionEntry, Map("computations" -> List(computationsNode)))
    val libraryNode = nodeWithAddedChild("library", libraryEntry, Map("version" -> List(versionNode)))

    libraryNode
  }

  def removeEntryLabeled(label: String, yamlSequence: JavaList[JavaMap[Any, Any]]): JavaMap[Any, Any] = {
    val indexOfElementContainingLabel: Int = yamlSequence.indexWhere(element => element.keys.contains(label))
    yamlSequence.remove(indexOfElementContainingLabel)
  }

  def toComputationNodeMap(topLevelJavaMaps: JavaList[JavaMap[Any, Any]]) :  Map[String, List[YamlPersistentNode]] = {
    val computationNodesList : List[YamlPersistentNode] = topLevelJavaMaps.map(element => toPersistentNode(element)).toList
    computationNodesList.foldLeft(Map[String, List[YamlPersistentNode]]()) {
      (mapSoFar, topLevelComputationNode) => {
        mapSoFar.get(topLevelComputationNode.label) match {
          case None => mapSoFar + (topLevelComputationNode.label -> List(topLevelComputationNode))
          case Some(computationList) => mapSoFar + (topLevelComputationNode.label -> (topLevelComputationNode :: computationList))
        }
      }
    }
  }
  
  def nodeWithAddedChild(label: String, topLevelNodeMap: JavaMap[Any, Any], addedChild: Map[String, List[YamlPersistentNode]]) = {
    val nodeWithoutAddedChild = toPersistentNode(topLevelNodeMap).asInstanceOf[YamlPersistentInternalNode]
    new YamlPersistentInternalNode(label, nodeWithoutAddedChild.scalarValuedNodes, nodeWithoutAddedChild.children ++ addedChild)
  }

  // At the top level, we always have maps
  def toPersistentNode(topLevelNodeMap: JavaMap[Any, Any]) : YamlPersistentNode = {
    val yamlNode = toYamlNode(topLevelNodeMap.head._1.toString,
                              topLevelNodeMap.head._2.asInstanceOf[JavaMap[Any, Any]])
    toPersistentNode(yamlNode.asInstanceOf[YamlMapNode])
  }

  def toYamlNode(label: String, yamlMap: JavaMap[Any, Any]) : YamlNode = {
    val subMap = yamlMap.foldLeft(Map[String, YamlNode]()){
      (mapSoFar, keyValuePair) => {
        val innerLabel = keyValuePair._1.toString
        keyValuePair._2 match {
          case map : JavaMap[Any, Any] => mapSoFar + (innerLabel -> toYamlNode(innerLabel, map))
          case list : JavaList[Any] => mapSoFar + (innerLabel -> toYamlNode(innerLabel, list))
          case value if isScalarValue(value) => mapSoFar + (innerLabel -> YamlPersistentTextBearingNode(innerLabel, value.toString))
        }
      }
    }
    YamlMapNode(label, subMap)
  }

  def toYamlNode(label: String, yamlList: JavaList[Any]) : YamlNode = {
    val subNodes = yamlList.map(node => node match {
      case map : JavaMap[Any, Any] => toYamlNode(label, map)
      case list : JavaList[Any] => toYamlNode(label, list)
      case value if isScalarValue(value) => YamlPersistentTextBearingNode(label, value.toString)
    }).toList
    YamlListNode(label, subNodes)
  }

  def isScalarValue(value: Any) : Boolean = {
    value.isInstanceOf[String] ||
      value.isInstanceOf[java.lang.Boolean] ||
      value.isInstanceOf[java.lang.Character] ||
      value.isInstanceOf[java.lang.Integer] ||
      value.isInstanceOf[java.lang.Long] ||
      value.isInstanceOf[java.lang.Double] ||
      value.isInstanceOf[java.lang.Float] ||
      value.isInstanceOf[java.util.Date]
  }

  def toPersistentNode(yamlMapNode: YamlMapNode) : YamlPersistentNode = {
    val subNodes = descendants(yamlMapNode.nodeMap)

    val attributesAndChildNodes = subNodes.partition(stringToNodeList => {
      val nodeList = stringToNodeList._2
      nodeList.size == 1 && nodeList.head.isInstanceOf[YamlPersistentTextBearingNode]
    })

    val attributeMap = attributesAndChildNodes._1.map(labelToListOfOneAttributeValue => {
      val attributeLabel = labelToListOfOneAttributeValue._1
      val attributeNode = labelToListOfOneAttributeValue._2.head.asInstanceOf[YamlPersistentTextBearingNode]
      attributeLabel -> attributeNode
    })
    YamlPersistentInternalNode(yamlMapNode.label, attributeMap, attributesAndChildNodes._2)
  }

  def descendants(yamlNodeMap: Map[String, YamlNode]) : Map[String, List[YamlPersistentNode]] = {
    yamlNodeMap.map(labelWithValue => {
      val label = labelWithValue._1
      val nodes = labelWithValue._2
      nodes match {
        case aTextNode : YamlPersistentTextBearingNode            => label -> List(aTextNode)
        case aList : YamlListNode if label == "imports"           => label -> toImports(aList)
        case aList : YamlListNode if label == "inputs"            => label -> toInputs(aList)
        case aList : YamlListNode if label == "innerComputations" => label -> toInnerComputations(aList)
        case aMap : YamlMapNode if label == "inputTuple"          => label -> List(toMappingNode(aMap))
        case aMap : YamlMapNode if label == "accumulatorTuple"    => label -> List(toMappingNode(aMap))
        case aMap : YamlMapNode if label == "innerComputation"    => label -> List(toInnerComputation(aMap.nodeMap))
        case aMap: YamlMapNode                                    => label -> List(toPersistentNode(aMap))
      }
    }).toMap
  }
  
  def toImports(yamlListNode: YamlListNode) : List[YamlPersistentInternalNode] = {
    val importNodes: List[YamlPersistentTextBearingNode] = yamlListNode.nodes.map {
      yamlNode => YamlPersistentTextBearingNode("import", yamlNode.asInstanceOf[YamlPersistentTextBearingNode].text)
    }
    List(YamlPersistentInternalNode("imports", Map(), Map("import" -> importNodes)))
  }
  
  def toInputs(yamlListNode: YamlListNode) : List[YamlPersistentInternalNode] = {
    val mappingNodes: List[YamlPersistentNode] = yamlListNode.nodes.map {
      yamlNode => toMappingNode(yamlNode.asInstanceOf[YamlMapNode])
    }    
    List(YamlPersistentInternalNode("inputs", Map(), Map("mapping" -> mappingNodes)))
  }

  def toMappingNode(yamlMapNode: YamlMapNode) : YamlPersistentInternalNode = {
    val nodeMapContainingMapping: Map[String, YamlNode] = yamlMapNode.nodeMap //Map is "inputs" or "inputTuple" etc. to a single TextBearingNode with the mapping
    val mappingNode = nodeMapContainingMapping.values.head.asInstanceOf[YamlPersistentTextBearingNode]

    val mappingMap = Map("key" -> YamlPersistentTextBearingNode("key", mappingNode.label),
                         "value" -> YamlPersistentTextBearingNode("value", mappingNode.text))
    YamlPersistentInternalNode("mapping", mappingMap, Map())
  }

  def toInnerComputations(yamlListNode: YamlListNode) : List[YamlPersistentInternalNode] = {
    val innerComputationNodes = yamlListNode.nodes.map(yamlNode => yamlNode match {
      case refNode : YamlPersistentTextBearingNode => toInnerComputation(Map(refNode.label -> refNode))
      case mapNode : YamlMapNode => toInnerComputation(mapNode.nodeMap)
    })
    List(YamlPersistentInternalNode("innerComputations", Map(), Map("innerComputation" -> innerComputationNodes)))
  }

  def toInnerComputation(innerMapping: Map[String,YamlNode]) : YamlPersistentInternalNode = {
    val innerComputationNode = innerMapping.values.head match {
      case refNode : YamlPersistentTextBearingNode => refNode
      case mapNode : YamlMapNode => toPersistentNode(mapNode)
    }
    YamlPersistentInternalNode("innerComputation", Map(), Map(innerComputationNode.label -> List(innerComputationNode)))
  }
}

trait YamlPersistentNode extends PersistentNode
case class YamlPersistentInternalNode(label: String, scalarValuedNodes: Map[String, YamlPersistentTextBearingNode], children: Map[String, List[YamlPersistentNode]]) extends YamlPersistentNode

case class YamlPersistentTextBearingNode(label: String, text: String) extends PersistentTextBearingNode with YamlPersistentNode with YamlNode

trait YamlNode
case class YamlListNode(label: String, nodes: List[YamlNode]) extends YamlNode
case class YamlMapNode(label: String, nodeMap: Map[String, YamlNode]) extends YamlNode

