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

  val rootNode: YamlRoot = loadNodes(yamlData.toList)

  protected def attrValue(node: YPersistentNode, key: String): String = {
    node match {
      case n: YamlMapNode => {
        n.nodeMap.get(key).head.asInstanceOf[YamlTextNode].value
      }
    }
  }

  protected def optionalAttrValue(node: YPersistentNode, key: String): Option[String] = {
    node match {
      case n: YamlMapNode => {
        n.nodeMap.get(key) match {
          case Some(m: YamlTextNode) => {
            Some(m.value)
          }
          case _ => {
            Option.empty
          }
        }
      }
      case _ => {
        Option.empty
      }
    }
  }

  protected def children(node: YPersistentNode): List[YPersistentNode] = {
    node match {
      case n: YamlListNode => {
        n.nodes
      }
      case n: YamlMapNode  => {
        n.nodeMap.values.toList
      }
    }
  }

  protected def childOfType(node: YPersistentNode, label: String): YPersistentNode = {
    val a = node match {
      case n: YamlMapNode  => List(n.nodeMap.get(label).get)
      case n: YamlListNode => n.nodes.filter(_.label == label)
    }
    a.head
  }

  protected def asTextBearingNode(node: YPersistentNode): YamlTextNode = {
    node.asInstanceOf[YamlTextNode]
  }
  protected def dateTime(timeString: String): DateTime = {
    val date = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").parse(timeString)
    new DateTime(date)
  }

  protected def loadNodes(yamlData: Iterable[Any]): YamlRoot = {
    val yamlSequence = yamlData.toList.head.asInstanceOf[JavaList[JavaMap[Any, Any]]]
    val nodes = yamlSequence.map(x => {
      x.map(x => toYamlNode(x._1.toString, x._2))
    }).flatten.toList

    YamlRoot(nodes.find(_.label == "library").get, nodes.find(_.label == "version").get, nodes.filterNot(x => x.label == "version" || x.label == "library"))
  }

  protected def toYamlNode(label: String, yaml: Any): YamlNode = {
    yaml match {
      case nodeList: JavaList[_] => YamlListNode(label, nodeList.map(toYamlNode(label, _)).toList)
      case node: JavaMap[_, _]   => YamlMapNode(label, node.map(x => (x._1.toString, toYamlNode(x._1.toString, x._2))).toMap)
      case node: Any             => YamlTextNode(label, node.toString)
    }
  }
}

trait YamlNode {
  def label: String
}

case class YamlListNode(label: String, nodes: List[YamlNode]) extends YamlNode
case class YamlMapNode(label: String, nodeMap: Map[String, YamlNode]) extends YamlNode
case class YamlTextNode(label: String, value: String) extends YamlNode

trait YReader {
  type YPersistentNode = YamlNode
  val rootNode: YamlRoot

  def unmarshal: Library = library(rootNode)

  def unmarshal(node: YPersistentNode) : SyntaxTreeNode = node.label match {
    case "library" => throw new RuntimeException("library node should not be unmarshaled directly")
    case "version" => throw new RuntimeException("version node should not be unmarshaled directly")
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

  protected def library(root: YamlRoot) = {
    val ver = version(root.version, root.computations)
    Library(attrValue(root.library, "name"), Map(ver.versionNumber -> ver))
  }

  protected def version(versionNode: YPersistentNode, topLevelComputations: List[YPersistentNode]): Version = {
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
      attrValue(node, "computationExpression"),
      unmarshal(childOfType(node, "imports")).asInstanceOf[Imports],
      unmarshal(childOfType(node, "inputs")).asInstanceOf[Inputs],
      attrValue(node, "resultKey"),
      attrValue(node, "logger"),
      attrValue(node, "securityConfiguration")
    )
  }

  protected def abortIfComputationFactory(node: YPersistentNode) : AbortIfComputationSpecification = {
    AbortIfComputationSpecification(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      attrValue(node, "shouldPropagateExceptions").toBoolean,
      attrValue(node, "predicateExpression"),
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshal(childOfType(node, "imports")).asInstanceOf[Imports],
      unmarshal(childOfType(node, "inputs")).asInstanceOf[Inputs],
      attrValue(node, "logger"),
      attrValue(node, "securityConfiguration")
    )
  }

  protected def namedComputation(node: YPersistentNode) : NamedComputationSpecification = {
    NamedComputationSpecification(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      unmarshal(mapChildren(node)).asInstanceOf[NamableComputationSpecification]
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
      attrValue(node, "resultKey")
    )
  }

  protected def iterativeComputation(node: YPersistentNode) : IterativeComputationSpecification = {
    IterativeComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      attrValue(node, "resultKey")
    )
  }

  protected def foldingComputation(node: YPersistentNode) : FoldingComputationSpecification = {
    FoldingComputationSpecification(
      extractInnerComputationFrom(childOfType(node, "innerComputation")),
      attrValue(node, "initialAccumulatorKey"),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshal(childOfType(node, "accumulatorTuple")).asInstanceOf[Mapping]
    )
  }

  protected def sequentialComputation(node: YPersistentNode) : SequentialComputationSpecification = {
    val innerComputationsNode = childOfType(node, "innerComputations")
    val innerComputations = children(innerComputationsNode).map(extractInnerComputationFrom)

    SequentialComputationSpecification (
      innerComputations.head,
      innerComputations.tail:_*
    )
  }

  protected def reference(node: YPersistentNode) : Ref = {
    new Ref(asTextBearingNode(node).value)
  }

  protected def imports(node: YPersistentNode) : Imports = {
    val importStrings = children(node).map(asTextBearingNode(_).value)
    Imports(importStrings:_*)
  }

  protected def inputs(node: YPersistentNode) : Inputs = {
    val nodes = children(node).map(mapping).toList
    Inputs(nodes.head, nodes.tail:_*)
  }

  protected def mapping(node: YPersistentNode) : Mapping =  {
    val mapping = children(node).map(asTextBearingNode).head
    Mapping(mapping.label, mapping.value)
  }

  protected def singleTuple(node: YPersistentNode) : Mapping = {
    mapping(node)
  }

  protected def extractInnerComputationFrom(innerComputationNode: YPersistentNode) : InnerComputationSpecification = {
    assert(children(innerComputationNode).size == 1)
    val innerComputation = children(innerComputationNode).head
    unmarshal(innerComputation).asInstanceOf[InnerComputationSpecification]
  }

  protected def attrValue(node: YPersistentNode, key: String) : String
  protected def optionalAttrValue(node: YPersistentNode, key: String): Option[String]
  protected def children(node: YPersistentNode) : List[YPersistentNode]
  protected def childOfType(node: YPersistentNode, label: String) : YPersistentNode

  protected def asTextBearingNode(node: YPersistentNode): YamlTextNode
  protected def dateTime(timeString: String): DateTime

  protected def mapChildren(YPersistentNode: YPersistentNode) : YPersistentNode = {
    children(YPersistentNode).find(_.isInstanceOf[YamlMapNode]).get
  }
}

case class YamlRoot(library: YamlNode, version: YamlNode, computations: List[YamlNode])