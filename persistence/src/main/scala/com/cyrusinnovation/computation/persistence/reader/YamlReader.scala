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

class YamlReader(yamlData: Iterable[AnyRef]) extends Reader {
  private type NodeContext = Map[String, String]

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

  def unmarshal: Library = unmarshal(rootNode, Map.empty).asInstanceOf[Library]

  def unmarshal(node: YamlNode, context: NodeContext): SyntaxTreeNode = node.label match {
    case "library"                      => library(node, context)
    case "version"                      => version(node, context)
    case "simpleComputation"            => simpleComputationFactory(node, context)
    case "abortIfComputation"           => abortIfComputationFactory(node, context)
    case "namedComputation"             => namedComputation(node, context)
    case "abortIfNoResultsComputation"  => abortIfNoResultsComputation(node, context)
    case "abortIfHasResultsComputation" => abortIfNoResultsComputation(node, context)
    case "mappingComputation"           => mappingComputation(node, context)
    case "iterativeComputation"         => iterativeComputation(node, context)
    case "foldingComputation"           => foldingComputation(node, context)
    case "sequentialComputation"        => sequentialComputation(node, context)
    case "innerComputation"             => innerComputation(node, context)
    case "innerComputations"            => throw new RuntimeException("innerComputations node should not be unmarshaled directly to AstNode")
    case "ref"                          => reference(node)
    case "imports"                      => imports(node)
    case "inputs"                       => inputs(node)
    case "inputTuple"                   => singleTuple(node)
    case "accumulatorTuple"             => singleTuple(node)
  }

  protected def library(root: YamlNode, context: NodeContext) = root match {
    case rootNode : YamlRoot => {
      val ver = unmarshal(rootNode.version, context).asInstanceOf[Version]
      Library(attrValue(rootNode.library, "name", context), Map(ver.versionNumber -> ver))
    }
    case _ => throw new RuntimeException("library node must be a YamlRoot node")
  }

  private def extractDefaults(version: YamlNode, exclusions: List[String]) = {
    attrs(version).filterNot(x => exclusions.contains(x.label)).map(x => (x.label, x.value)).toMap
  }

  protected def version(versionNode: YamlNode, context: NodeContext): Version = versionNode match {
    case YamlVersionNode(label, version, topLevelComputations) => {
      val defaultsContext = extractDefaults(version, List("versionNumber", "state", "commitDate", "lastEditDate"))
      Version(attrValue(version, "versionNumber", context),
        versionState(attrValue(version, "state", context)),
        optionalAttrValue(version, "commitDate").map(timeString => dateTime(timeString)),
        optionalAttrValue(version, "lastEditDate").map(timeString => dateTime(timeString)),
        unmarshal(topLevelComputations.head, defaultsContext).asInstanceOf[TopLevelComputationSpecification],
        topLevelComputations.tail.map(computationNode => unmarshal(computationNode, defaultsContext).asInstanceOf[TopLevelComputationSpecification]): _*
      )
    }
    case _ => throw new RuntimeException("version node must be a YamlVersionNode")
  }

  protected def versionState(stateString: String) : VersionState = {
    VersionState.fromString(stateString)
  }

  protected def simpleComputationFactory(node: YamlNode, context: NodeContext): SimpleComputationSpecification = {
    SimpleComputationSpecification(
      attrValue(node, "package", context),
      attrValue(node, "name", context),
      attrValue(node, "description", context),
      attrValue(node, "changedInVersion", context),
      attrValue(node, "shouldPropagateExceptions", context).toBoolean,
      attrValue(node, "computationExpression", context),
      unmarshal(childOfType(node, "imports"), context).asInstanceOf[Imports],
      unmarshal(childOfType(node, "inputs"), context).asInstanceOf[Inputs],
      attrValue(node, "resultKey", context),
      attrValue(node, "logger", context),
      attrValue(node, "securityConfiguration", context)
    )
  }

  protected def abortIfComputationFactory(node: YamlNode, context: NodeContext): AbortIfComputationSpecification = {
    AbortIfComputationSpecification(
      attrValue(node, "package", context),
      attrValue(node, "name", context),
      attrValue(node, "description", context),
      attrValue(node, "changedInVersion", context),
      attrValue(node, "shouldPropagateExceptions", context).toBoolean,
      attrValue(node, "predicateExpression", context),
      unmarshal(childOfType(node, "innerComputation"), context).asInstanceOf[InnerComputationSpecification],
      unmarshal(childOfType(node, "imports"), context).asInstanceOf[Imports],
      unmarshal(childOfType(node, "inputs"), context).asInstanceOf[Inputs],
      attrValue(node, "logger", context),
      attrValue(node, "securityConfiguration", context)
    )
  }

  protected def innerComputation(innerComputationNode: YamlNode, context: NodeContext): InnerComputationSpecification = {
    assert(children(innerComputationNode).size == 1)
    val innerComputation = children(innerComputationNode).head
    unmarshal(innerComputation, context).asInstanceOf[InnerComputationSpecification]
  }

  protected def namedComputation(node: YamlNode, context: NodeContext): NamedComputationSpecification = {
    val childMapNode = children(node).find(_.isInstanceOf[YamlMapNode]).get
    NamedComputationSpecification(
      attrValue(node, "package", context),
      attrValue(node, "name", context),
      attrValue(node, "description", context),
      attrValue(node, "changedInVersion", context),
      unmarshal(childMapNode, context).asInstanceOf[NamableComputationSpecification]
    )
  }

  protected def abortIfNoResultsComputation(node: YamlNode, context: NodeContext): AbortIfNoResultsComputationSpecification = {
    AbortIfNoResultsComputationSpecification(
      unmarshal(childOfType(node, "innerComputation"), context).asInstanceOf[InnerComputationSpecification]
    )
  }

  protected def abortIfHasResultsComputation(node: YamlNode, context: NodeContext): AbortIfHasResultsComputationSpecification = {
    AbortIfHasResultsComputationSpecification(
      unmarshal(childOfType(node, "innerComputation"), context).asInstanceOf[InnerComputationSpecification]
    )
  }

  protected def mappingComputation(node: YamlNode, context: NodeContext): MappingComputationSpecification = {
    MappingComputationSpecification(
      unmarshal(childOfType(node, "innerComputation"), context).asInstanceOf[InnerComputationSpecification],
      unmarshal(childOfType(node, "inputTuple"), context).asInstanceOf[Mapping],
      attrValue(node, "resultKey", context)
    )
  }

  protected def iterativeComputation(node: YamlNode, context: NodeContext): IterativeComputationSpecification = {
    IterativeComputationSpecification(
      unmarshal(childOfType(node, "innerComputation"), context).asInstanceOf[InnerComputationSpecification],
      unmarshal(childOfType(node, "inputTuple"), context).asInstanceOf[Mapping],
      attrValue(node, "resultKey", context)
    )
  }

  protected def foldingComputation(node: YamlNode, context: NodeContext): FoldingComputationSpecification = {
    FoldingComputationSpecification(
      unmarshal(childOfType(node, "innerComputation"), context).asInstanceOf[InnerComputationSpecification],
      attrValue(node, "initialAccumulatorKey", context),
      unmarshal(childOfType(node, "inputTuple"), context).asInstanceOf[Mapping],
      unmarshal(childOfType(node, "accumulatorTuple"), context).asInstanceOf[Mapping]
    )
  }

  protected def sequentialComputation(node: YamlNode, context: NodeContext): SequentialComputationSpecification = {
    val innerComputationsNode = childOfType(node, "innerComputations")
    val innerComputations = children(innerComputationsNode).map(innerComputation(_, context))

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


  protected def attrValue(node: YamlNode, key: String, context: NodeContext): String = {
    val mapNode = node match { case n: YamlMapNode => n.nodeMap.get(key) }
    val mapAttrValue = mapNode.map(_.asInstanceOf[YamlTextNode].value)
    mapAttrValue.orElse(context.get(key)) match {
      case Some(attributeValue: String) => attributeValue
      case None                         => throw new RuntimeException(s"Required attribute value $key not found.")
    }
  }

  protected def attrs(node: YamlNode): List[YamlTextNode] = {
    node match {
      case n: YamlMapNode => {
        (n.nodeMap.values collect { case m: YamlTextNode => m}).toList
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
