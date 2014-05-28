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

  val rootNode: YamlRoot = loadNodes(yamlData.toList)

  protected def mapEntry(node: YPersistentNode): (String, String) = {
    node match {
      case n: YamlMapNode => {
        assert(n.nodeMap.size == 1)
        val tn = n.nodeMap.head._2.asInstanceOf[YamlTextNode]
        (tn.label, tn.value)
      }
    }
  }

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
          case None => {
            Option.empty
          }
        }
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

  protected def children(node: YPersistentNode, label: String): List[YPersistentNode] = {
    node match {
      case n: YamlMapNode  => List(n.nodeMap.get(label).get)
      case n: YamlListNode => n.nodes.filter(_.label == label)
    }
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
    //    val library = getAndRemoveMapNode("library", yamlSequence)
    //    val version = getAndRemoveMapNode("version", yamlSequence)

    //    val compNodes = toYamlNode("root", yamlSequence).asInstanceOf[YamlListNode].nodes.map(_.asInstanceOf[YamlMapNode].nodeMap.head).toMap
    //    library.copy(nodeMap = library.nodeMap ++ Map("version" -> version.copy(nodeMap = version.nodeMap ++ compNodes)))

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

  def unmarshal: Library = start(rootNode).asInstanceOf[Library]

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

  def start(root: YamlRoot) = {
    val ver = version(root.version, root.computations)
    Library(attrValue(root.library, "name"), Map(ver.versionNumber -> ver))
  }

  def version(versionNode: YPersistentNode, topLevelComputations: List[YPersistentNode]): Version = {
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
    val importStrings = children(node, "imports").map(x => unmarshalToString(x))
    Imports(importStrings:_*)
  }

  protected def inputs(node: YPersistentNode) : Inputs = {
    val nodes: List[Mapping] = node.asInstanceOf[YamlListNode].nodes.map(x => mapping(x)).toList
    Inputs(nodes.head, nodes.tail:_*)
  }

  protected def mapping(node: YPersistentNode) : Mapping =  {
    val sss = mapEntry(node)
    Mapping(sss._1, sss._2)
  }

  protected def singleTuple(node: YPersistentNode) : Mapping = {
    val aa = node.asInstanceOf[YamlMapNode].nodeMap.head._2.asInstanceOf[YamlTextNode]
    Mapping(aa.label, aa.value)
  }

  protected def extractInnerComputationFrom(innerComputationNode: YPersistentNode) : InnerComputationSpecification = {
    assert(children(innerComputationNode).size == 1)
    val innerComputation = children(innerComputationNode).head
    unmarshal(innerComputation).asInstanceOf[InnerComputationSpecification]
  }

  protected def mapEntry(node: YPersistentNode): (String, String)
  protected def attrValue(node: YPersistentNode, key: String) : String
  protected def optionalAttrValue(node: YPersistentNode, key: String): Option[String]
  protected def children(node: YPersistentNode) : List[YPersistentNode]
  protected def children(node: YPersistentNode, label: String) : List[YPersistentNode]

  protected def asTextBearingNode(node: YPersistentNode): YamlTextNode
  protected def dateTime(timeString: String): DateTime

  //TODO This is hackery. Make this more consistent.
  protected def unmarshalToString(YPersistentNode: YPersistentNode) : String = {
    asTextBearingNode(YPersistentNode).value
  }

  //TODO This is hackery. Make this more consistent.
  protected def unmarshalChildToString(node: YPersistentNode, key: String) : String = {
    optionalAttrValue(node, key) match {
      case Some(value) => value
      case None => asTextBearingNode(childOfType(node, key)).label
    }
  }

  protected def mapChildren(YPersistentNode: YPersistentNode) : YPersistentNode = {
    children(YPersistentNode).find(_.isInstanceOf[YamlMapNode]).get
  }

  protected def childOfType(YPersistentNode: YPersistentNode, label: String) : YPersistentNode = {
    children(YPersistentNode, label).head
  }
}

case class YamlRoot(library: YamlNode, version: YamlNode, computations: List[YamlNode])