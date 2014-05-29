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


trait YamlNode {
  def label: String
}

case class YamlListNode(label: String, nodes: List[YamlNode]) extends YamlNode
case class YamlMapNode(label: String, nodeMap: Map[String, YamlNode]) extends YamlNode
case class YamlTextNode(label: String, value: String) extends YamlNode

case class YamlRoot(label: String, library: YamlNode, version: YamlVersionNode) extends YamlNode
case class YamlVersionNode(label: String, version: YamlNode, computations: List[YamlNode]) extends YamlNode


object YamlReader {
  def fromFileOnClasspath(resourcePath: String) : YamlReader = {
    val inputStream: InputStream = getClass.getResourceAsStream(resourcePath)
    fromInputStream(inputStream)
  }

  def fromFile(path: String) : YamlReader = {
    val inputStream: InputStream = new FileInputStream(new File(path))
    fromInputStream(inputStream)
  }

  def fromFileUri(uri: URI) : YamlReader = {
    val inputStream: InputStream = new FileInputStream(new File(uri))
    fromInputStream(inputStream)
  }

  def fromInputStream(inputStream: InputStream) : YamlReader = {
    val snakeYaml = new Yaml()
    val data: Iterable[AnyRef] = snakeYaml.loadAll(inputStream);

    new YamlReader(data)
  }
}

class YamlReader(yamlData: Iterable[AnyRef]) extends AbstractReader {

  lazy val rootNode: YamlRoot = loadNodes(yamlData.toList)

  protected def loadNodes(yamlData: Iterable[Any]): YamlRoot = {
    val yamlSequence = yamlData.toList.head.asInstanceOf[JavaList[JavaMap[Any, Any]]]
    val nodes = yamlSequence.map(x => {
      x.map(x => toYamlNode(x._1.toString, x._2))
    }).flatten.toList

    val versionNode = nodes.find(_.label == "version").get
    val computationNodes = nodes.filterNot(x => x.label == "version" || x.label == "library")
    val version = YamlVersionNode("version", versionNode, computationNodes)

    val libraryNode = nodes.find(_.label == "library").get
    YamlRoot("library", libraryNode, version)
  }

  protected def toYamlNode(label: String, yaml: Any): YamlNode = {
    yaml match {
      case nodeList: JavaList[_] => YamlListNode(label, nodeList.map(toYamlNode(label, _)).toList)
      case node: JavaMap[_, _]   => YamlMapNode(label, node.map(x => (x._1.toString, toYamlNode(x._1.toString, x._2))).toMap)
      case node: Any             => YamlTextNode(label, node.toString)
    }
  }

  def unmarshal: Library = unmarshal(rootNode).asInstanceOf[Library]

  def unmarshal(node: YamlNode) : SyntaxTreeNode = node.label match {
    case "library" => library(node)
    case "version" => version(node)
    case "simpleComputation" => simpleComputationFactory(node)
    case "abortIfComputation" => abortIfComputationFactory(node)
    case "namedComputation" => namedComputation(node)
    case "abortIfNoResultsComputation" => abortIfNoResultsComputation(node)
    case "abortIfHasResultsComputation" => abortIfNoResultsComputation(node)
    case "mappingComputation" => mappingComputation(node)
    case "iterativeComputation" => iterativeComputation(node)
    case "foldingComputation" => foldingComputation(node)
    case "sequentialComputation" => sequentialComputation(node)
    case "innerComputation" => innerComputation(node)
    case "innerComputations" => throw new RuntimeException("innerComputations node should not be unmarshaled directly to AstNode")
    case "ref" => reference(node)
    case "imports" => imports(node)
    case "inputs" => inputs(node)
    case "inputTuple" => singleTuple(node)
    case "accumulatorTuple" => singleTuple(node)
  }

  protected def library(root: YamlNode) = root match {
    case rootNode : YamlRoot => {
      val ver = unmarshal(rootNode.version).asInstanceOf[Version]
      Library(attrValue(rootNode.library, "name"), Map(ver.versionNumber -> ver))
    }
    case _ => throw new RuntimeException("library node must be a YamlRoot node")
  }

  protected def version(versionNode: YamlNode): Version = versionNode match {
    case YamlVersionNode(label, version, topLevelComputations) => {
      Version(attrValue(version, "versionNumber"),
        versionState(attrValue(version, "state")),
        optionalAttrValue(version, "commitDate").map(timeString => dateTime(timeString)),
        optionalAttrValue(version, "lastEditDate").map(timeString => dateTime(timeString)),
        unmarshal(topLevelComputations.head).asInstanceOf[TopLevelComputationSpecification],
        topLevelComputations.tail.map(computationNode => unmarshal(computationNode).asInstanceOf[TopLevelComputationSpecification]):_*
      )
    }
    case _ => throw new RuntimeException("version node must be a YamlVersionNode")
  }

  protected def versionState(stateString: String) : VersionState = {
    VersionState.fromString(stateString)
  }

  protected def simpleComputationFactory(node: YamlNode) : SimpleComputationSpecification = {
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

  protected def abortIfComputationFactory(node: YamlNode) : AbortIfComputationSpecification = {
    AbortIfComputationSpecification(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      attrValue(node, "shouldPropagateExceptions").toBoolean,
      attrValue(node, "predicateExpression"),
      unmarshal(childOfType(node, "innerComputation")).asInstanceOf[InnerComputationSpecification],
      unmarshal(childOfType(node, "imports")).asInstanceOf[Imports],
      unmarshal(childOfType(node, "inputs")).asInstanceOf[Inputs],
      attrValue(node, "logger"),
      attrValue(node, "securityConfiguration")
    )
  }

  protected def innerComputation(innerComputationNode: YamlNode) : InnerComputationSpecification = {
    assert(children(innerComputationNode).size == 1)
    val innerComputation = children(innerComputationNode).head
    unmarshal(innerComputation).asInstanceOf[InnerComputationSpecification]
  }

  protected def namedComputation(node: YamlNode) : NamedComputationSpecification = {
    val childMapNode = children(node).find(_.isInstanceOf[YamlMapNode]).get
    NamedComputationSpecification(
      attrValue(node, "package"),
      attrValue(node, "name"),
      attrValue(node, "description"),
      attrValue(node, "changedInVersion"),
      unmarshal(childMapNode).asInstanceOf[NamableComputationSpecification]
    )
  }

  protected def abortIfNoResultsComputation(node: YamlNode) : AbortIfNoResultsComputationSpecification = {
    AbortIfNoResultsComputationSpecification(
      unmarshal(childOfType(node, "innerComputation")).asInstanceOf[InnerComputationSpecification]
    )
  }

  protected def abortIfHasResultsComputation(node: YamlNode) : AbortIfHasResultsComputationSpecification = {
    AbortIfHasResultsComputationSpecification(
      unmarshal(childOfType(node, "innerComputation")).asInstanceOf[InnerComputationSpecification]
    )
  }

  protected def mappingComputation(node: YamlNode) : MappingComputationSpecification = {
    MappingComputationSpecification(
      unmarshal(childOfType(node, "innerComputation")).asInstanceOf[InnerComputationSpecification],
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      attrValue(node, "resultKey")
    )
  }

  protected def iterativeComputation(node: YamlNode) : IterativeComputationSpecification = {
    IterativeComputationSpecification(
      unmarshal(childOfType(node, "innerComputation")).asInstanceOf[InnerComputationSpecification],
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      attrValue(node, "resultKey")
    )
  }

  protected def foldingComputation(node: YamlNode) : FoldingComputationSpecification = {
    FoldingComputationSpecification(
      unmarshal(childOfType(node, "innerComputation")).asInstanceOf[InnerComputationSpecification],
      attrValue(node, "initialAccumulatorKey"),
      unmarshal(childOfType(node, "inputTuple")).asInstanceOf[Mapping],
      unmarshal(childOfType(node, "accumulatorTuple")).asInstanceOf[Mapping]
    )
  }

  protected def sequentialComputation(node: YamlNode) : SequentialComputationSpecification = {
    val innerComputationsNode = childOfType(node, "innerComputations")
    val innerComputations = children(innerComputationsNode).map(innerComputation)

    SequentialComputationSpecification (
      innerComputations.head,
      innerComputations.tail:_*
    )
  }

  protected def reference(node: YamlNode) : Ref = {
    new Ref(asTextBearingNode(node).value)
  }

  protected def imports(node: YamlNode) : Imports = {
    val importStrings = children(node).map(asTextBearingNode(_).value)
    Imports(importStrings:_*)
  }

  protected def inputs(node: YamlNode) : Inputs = {
    val nodes = children(node).map(singleTuple).toList
    Inputs(nodes.head, nodes.tail:_*)
  }

  protected def singleTuple(node: YamlNode) : Mapping = {
    val mapping = children(node).map(asTextBearingNode).head
    Mapping(mapping.label, mapping.value)
  }


  protected def attrValue(node: YamlNode, key: String): String = {
    node match {
      case n: YamlMapNode => {
        n.nodeMap.get(key).head.asInstanceOf[YamlTextNode].value
      }
    }
  }

  protected def optionalAttrValue(node: YamlNode, key: String): Option[String] = {
    node match {
      case n: YamlMapNode => {
        n.nodeMap.get(key) match {
          case Some(m: YamlTextNode) => Some(m.value)
          case _ => None
        }
      }
      case _ => None
    }
  }

  protected def children(node: YamlNode): List[YamlNode] = {
    node match {
      case n: YamlListNode => n.nodes
      case n: YamlMapNode  =>  n.nodeMap.values.toList
    }
  }

  protected def childOfType(node: YamlNode, label: String): YamlNode = {
    val a = node match {
      case n: YamlMapNode  => List(n.nodeMap.get(label).get)
      case n: YamlListNode => n.nodes.filter(_.label == label)
    }
    a.head
  }

  protected def asTextBearingNode(node: YamlNode): YamlTextNode = {
    node.asInstanceOf[YamlTextNode]
  }

  protected def dateTime(timeString: String): DateTime = {
    val date = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").parse(timeString)
    new DateTime(date)
  }
}
