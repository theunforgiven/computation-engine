package com.cyrusinnovation.computation.persistence.reader

import java.io.{File, FileInputStream, InputStream}
import java.net.URI
import org.yaml.snakeyaml.Yaml
import java.lang.Iterable
import org.joda.time.DateTime
import scala.collection.JavaConversions._
import java.util.{Map => JavaMap, List => JavaList}
import java.text.SimpleDateFormat
import com.cyrusinnovation.computation.specification._
import scala.Some
import scala.Some
import scala.Some
import scala.Some
import scala.Some
import scala.Some
import scala.Some
import scala.Some
import scala.Some
import scala.Some
import scala.Some
import scala.Some
import scala.Some
import scala.Some
import scala.Some
import scala.Some
import com.cyrusinnovation.computation.specification.IterativeComputationSpecification
import com.cyrusinnovation.computation.specification.Version
import com.cyrusinnovation.computation.specification.Library
import com.cyrusinnovation.computation.specification.SequentialComputationSpecification
import com.cyrusinnovation.computation.specification.SimpleComputationSpecification
import com.cyrusinnovation.computation.specification.Imports
import com.cyrusinnovation.computation.specification.Mapping
import com.cyrusinnovation.computation.specification.MappingComputationSpecification
import com.cyrusinnovation.computation.specification.AbortIfNoResultsComputationSpecification
import com.cyrusinnovation.computation.specification.AbortIfHasResultsComputationSpecification
import com.cyrusinnovation.computation.specification.NamedComputationSpecification
import com.cyrusinnovation.computation.specification.AbortIfComputationSpecification
import com.cyrusinnovation.computation.specification.FoldingComputationSpecification
import com.cyrusinnovation.computation.specification.Inputs


object YamlReader {
  def fromFileOnClasspath(resourcePath: String) : YReader = {
    val inputStream: InputStream = getClass.getResourceAsStream(resourcePath)
    fromInputStream(inputStream)
  }

  def fromFile(path: String) : YReader = {
    val inputStream: InputStream = new FileInputStream(new File(path))
    fromInputStream(inputStream)
  }

  def fromFileUri(uri: URI) : YReader = {
    val inputStream: InputStream = new FileInputStream(new File(uri))
    fromInputStream(inputStream)
  }

  def fromInputStream(inputStream: InputStream) : YReader = {
    val snakeYaml = new Yaml()
    val data: Iterable[AnyRef] = snakeYaml.loadAll(inputStream);

    new YamlReader(data)
  }
}


class YamlReader(yamlData: Iterable[AnyRef]) extends YReader {

  val rootNode: YPersistentNode = loadNodes(yamlData.toList)

  protected def attrValue(node: YPersistentNode, key: String): String = {
    node match {
      case YamlPersistentTextBearingNode(label, text) => if(label == key) text else throw new RuntimeException(s"No such attribute ${key} for text-bearing node ${label}, ${text}")
      case internalNode : YamlPersistentInternalNode => internalNode.scalarValuedNodes(key).text
    }
  }

  protected def optionalAttrValue(node: YPersistentNode, key: String): Option[String] = {
    node match {
      case YamlPersistentTextBearingNode(label, text) => if(label == key) Some(text) else None
      case internalNode : YamlPersistentInternalNode => internalNode.scalarValuedNodes.get(key).map(node => node.text)
    }
  }

  protected def children(node: YPersistentNode): List[YPersistentNode] = node match {
    case leaf : YamlPersistentTextBearingNode => List()
    case internalNode : YamlPersistentInternalNode => internalNode.children.values.flatten.toList
  }

  protected def children(node: YPersistentNode, label: String): List[YPersistentNode] = node match {
    case leaf : YamlPersistentTextBearingNode => List()
    case node : YamlPersistentInternalNode => node.children.getOrElse(label, List())
  }

  protected def asTextBearingNode(node: YPersistentNode): YPersistentTextBearingNode = node match {
    case leaf : YamlPersistentTextBearingNode => leaf
  }

  protected def dateTime(timeString: String): DateTime = {
    val date = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").parse(timeString)
    new DateTime(date)
  }

  protected def loadNodes(yamlData: Iterable[Any]) : YPersistentNode = {
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
    val computationNodesList : List[YamlPersistentNode] = topLevelJavaMaps.map(element => toYPersistentNode(element)).toList
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
    val nodeWithoutAddedChild = toYPersistentNode(topLevelNodeMap).asInstanceOf[YamlPersistentInternalNode]
    new YamlPersistentInternalNode(label, nodeWithoutAddedChild.scalarValuedNodes, nodeWithoutAddedChild.children ++ addedChild)
  }

  // At the top level, we always have maps
  def toYPersistentNode(topLevelNodeMap: JavaMap[Any, Any]) : YamlPersistentNode = {
    val yamlNode = toYamlNode(topLevelNodeMap.head._1.toString,
                              topLevelNodeMap.head._2.asInstanceOf[JavaMap[Any, Any]])
    toYPersistentNode(yamlNode.asInstanceOf[YamlMapNode])
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

  def toYPersistentNode(yamlMapNode: YamlMapNode) : YamlPersistentNode = {
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
        case aMap: YamlMapNode                                    => label -> List(toYPersistentNode(aMap))
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
      case mapNode : YamlMapNode => toYPersistentNode(mapNode)
    }
    YamlPersistentInternalNode("innerComputation", Map(), Map(innerComputationNode.label -> List(innerComputationNode)))
  }
}

trait YamlPersistentNode extends YPersistentNode
case class YamlPersistentInternalNode(label: String, scalarValuedNodes: Map[String, YamlPersistentTextBearingNode], children: Map[String, List[YamlPersistentNode]]) extends YamlPersistentNode

case class YamlPersistentTextBearingNode(label: String, text: String) extends YPersistentTextBearingNode with YamlPersistentNode with YamlNode

trait YamlNode
case class YamlListNode(label: String, nodes: List[YamlNode]) extends YamlNode
case class YamlMapNode(label: String, nodeMap: Map[String, YamlNode]) extends YamlNode

trait YPersistentNode {
  def label : String
}

trait YPersistentTextBearingNode extends YPersistentNode {
  val text : String
}

trait YReader {
  val rootNode : YPersistentNode
  def unmarshal: Library = unmarshal(rootNode).asInstanceOf[Library]

  def unmarshal(node: YPersistentNode) : SyntaxTreeNode = node.label match {
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

  def versionMap(node: YPersistentNode) : Map[String, Version] = {
    val versions = children(node, "version")
    versions.foldLeft(Map[String,Version]()) {
      (mapSoFar, versionNode) => {
        val version = unmarshal(versionNode).asInstanceOf[Version]
        mapSoFar + (version.versionNumber -> version)
      }
    }
  }

  def version(versionNode: YPersistentNode) : Version = {
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

  protected def simpleComputationFactory(node: YPersistentNode) : SimpleComputationSpecification = {
    SimpleComputationSpecification(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      attrValue(node, "shouldPropagateExceptions").toBoolean,
      unmarshalChildToString(node, "computationExpression"),
      unmarshal(childOfType(node, "imports")).asInstanceOf[Imports],
      unmarshal(childOfType(node, "inputs")).asInstanceOf[Inputs],
      unmarshalChildToString(node, "resultKey"),
      unmarshalChildToString(node, "logger"),
      unmarshalChildToString(node, "securityConfiguration")
    )
  }

  protected def abortIfComputationFactory(node: YPersistentNode) : AbortIfComputationSpecification = {
    AbortIfComputationSpecification(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      attrValue(node, "shouldPropagateExceptions").toBoolean,
      unmarshalChildToString(node, "predicateExpression"),
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshal(childOfType(node, "imports")).asInstanceOf[Imports],
      unmarshal(childOfType(node, "inputs")).asInstanceOf[Inputs],
      unmarshalChildToString(node, "logger"),
      unmarshalChildToString(node, "securityConfiguration")
    )
  }

  protected def namedComputation(node: YPersistentNode) : NamedComputationSpecification = {
    NamedComputationSpecification(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      unmarshal(child(node)).asInstanceOf[NamableComputationSpecification]
    )
  }

  protected def abortIfNoResultsComputation(node: YPersistentNode) : AbortIfNoResultsComputationSpecification = {
    AbortIfNoResultsComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation"))
    )
  }

  protected def abortIfHasResultsComputation(node: YPersistentNode) : AbortIfHasResultsComputationSpecification = {
    AbortIfHasResultsComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation"))
    )
  }

  protected def mappingComputation(node: YPersistentNode) : MappingComputationSpecification = {
    MappingComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshalChildToString(node, "resultKey")
    )
  }

  protected def iterativeComputation(node: YPersistentNode) : IterativeComputationSpecification = {
    IterativeComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshalChildToString(node, "resultKey")
    )
  }

  protected def foldingComputation(node: YPersistentNode) : FoldingComputationSpecification = {
    FoldingComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshalChildToString(node, "initialAccumulatorKey"),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshal(childOfType(node, "accumulatorTuple")).asInstanceOf[Mapping]
    )
  }

  protected def sequentialComputation(node: YPersistentNode) : SequentialComputationSpecification = {
    val innerComputationsNode = childOfType(node, "innerComputations")
    val innerComputations = children(innerComputationsNode).map(x => extractInnerComputationFrom(x))

    SequentialComputationSpecification (
      innerComputations.head,
      innerComputations.tail:_*
    )
  }

  protected def reference(node: YPersistentNode) : Ref = {
    new Ref(unmarshalToString(node))
  }

  protected def imports(node: YPersistentNode) : Imports = {
    val importStrings = children(node, "import").map(x => unmarshalToString(x))
    Imports(importStrings:_*)
  }

  protected def inputs(node: YPersistentNode) : Inputs = {
    val nodes: List[Mapping] = children(node, "mapping").map(x => unmarshal(x).asInstanceOf[Mapping])
    Inputs(nodes.head, nodes.tail:_*)
  }

  protected def mapping(node: YPersistentNode) : Mapping =  {
    Mapping(
      unmarshalChildToString(node, "key"),
      unmarshalChildToString(node, "value")
    )
  }

  protected def singleTuple(node: YPersistentNode) : Mapping = {
    unmarshal(childOfType(node, "mapping")).asInstanceOf[Mapping]
  }

  protected def extractInnerComputationFrom(innerComputationNode: YPersistentNode) : InnerComputationSpecification = {
    assert(children(innerComputationNode).size == 1)
    val innerComputation = children(innerComputationNode).head
    unmarshal(innerComputation).asInstanceOf[InnerComputationSpecification]
  }

  protected def attrValue(node: YPersistentNode, key: String) : String
  protected def optionalAttrValue(node: YPersistentNode, key: String): Option[String]
  protected def children(node: YPersistentNode) : List[YPersistentNode]
  protected def children(node: YPersistentNode, label: String) : List[YPersistentNode]
  protected def asTextBearingNode(node: YPersistentNode) : YPersistentTextBearingNode
  protected def dateTime(timeString: String): DateTime

  //TODO This is hackery. Make this more consistent.
  protected def unmarshalToString(YPersistentNode: YPersistentNode) : String = {
    asTextBearingNode(YPersistentNode).text
  }

  //TODO This is hackery. Make this more consistent.
  protected def unmarshalChildToString(node: YPersistentNode, key: String) : String = {
    optionalAttrValue(node, key) match {
      case Some(value) => value
      case None => asTextBearingNode(childOfType(node, key)).text
    }
  }

  protected def child(YPersistentNode: YPersistentNode) : YPersistentNode = {
    children(YPersistentNode).head
  }

  protected def childOfType(YPersistentNode: YPersistentNode, label: String) : YPersistentNode = {
    children(YPersistentNode, label).head
  }
}

