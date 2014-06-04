package com.cyrusinnovation.computation.persistence.writer

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import com.cyrusinnovation.computation.specification._
import Writer._

object TableWriter {
  private val formatter = ISODateTimeFormat.dateTime()
}

abstract class TableWriter extends Writer {

  protected override def version(version: Version) = {
    val withoutComputationsWrapper = super.version(version).asInstanceOf[EntryNode]
    withoutComputationsWrapper.copy(children = List(createNodeListNode("computations", withoutComputationsWrapper.children)))
  }

  protected override def imports(imports: Imports) = {
    val s = imports.importSequence.map(x =>  createMapNode("import", Map("text" -> x)))
    createNodeListNode("imports", s.toList)
  }

  protected override def sequentialComputationSpec(computation: SequentialComputationSpecification) = {
    val innerComputations = computation.innerSpecs.map(x => createNodeListNode("innerComputation", List(marshal(x))))
    val computationList = createNodeListNode("innerComputations", innerComputations)
    createNode("sequentialComputation", Map(), List(computationList))
  }

  protected override def mapping(mapping: Mapping) = {
    createNode("mapping", Map.empty, List(createMapNode("key", Map("text" -> mapping.key)), createMapNode("value", Map("text" -> mapping.value))))
  }

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
    TableWriter.formatter.print(d)
  }

  protected override def persist(nodeContext: Node) {
    val context = nodeContext.asInstanceOf[EntryNode]
    val libraryName = context.attrs("name")
    val versionNumber = context.children.head.asInstanceOf[EntryNode].attrs("versionNumber")
    val dataRows = parse(1, context, 1, 1)
    val nodes = dataRows.map(x => NodeDataRow(libraryName, versionNumber, x.id, x.key, x.value))
    val edges = dataRows.map(x => NodeDataEdge(libraryName, versionNumber, x.origin, x.id, x.sequence))
      .filterNot { case edge => edge.isRootRow }
      .distinct
    write(nodes, edges)
  }

  protected def write(rows: List[NodeDataRow], edges: List[NodeDataEdge])

  private def parse(newId: Int, context: Node, origin: Int, sequence: Int): List[DataRow] = {
    def nextId(rows: List[DataRow]) = if (rows.isEmpty) newId else rows.maxBy(_.id).id + 1
    context match {
      case e: EntryNode       => {
        val rows = DataRow(newId, "label", e.label, origin, sequence) :: e.attrs.map(x => DataRow(newId, x._1, x._2, origin, sequence)).toList
        e.children.zipWithIndex.foldLeft(rows)((soFar, ctx) => {
          val next = nextId(soFar)
          parse(next, ctx._1, newId, sequence + 1) ::: soFar
        })
      }
      case e: NodeListNode => {
        e.children.zipWithIndex.foldLeft(List(DataRow(newId, "label", e.label, origin, sequence)))((soFar, ctx) => {
          val next = nextId(soFar)
          soFar ::: parse(next, ctx._1, newId, sequence)
        })
      }
      case e: MapNode         => {
        e.label match {
          case "" => {
            e.children.zipWithIndex.foldLeft(List.empty[DataRow])((soFar, x) => {
              val next = nextId(soFar)
              soFar ::: List(DataRow(next, "label", x._1._1, origin, sequence ), DataRow(next, "text", x._1._2, origin,  sequence))
            })
          }
          case _  => {
            e.children.zipWithIndex.foldLeft(List(DataRow(newId, "label", e.label, origin, sequence)))((soFar, x) => {
              val next = newId
              soFar ::: List(DataRow(next, x._1._1, x._1._2, origin,  sequence ))
            })
          }
        }
      }
      case e: StringListNode        => {
        e.children.zipWithIndex.foldLeft(List(DataRow(newId, "label", e.label, origin, sequence)))((soFar, x) => {
          val next = nextId(soFar)
          val listSequence = sequence
          soFar ::: List(DataRow(next, "text", x._1, origin, listSequence))
        })
      }
    }
  }
}

case class DataRow(id: Int, key: String, value: String, origin: Int, sequence: Int)

case class NodeDataRow(libraryName: String, versionNumber: String, id: Int, key: String, value: String)

case class NodeDataEdge(libraryName: String, versionNumber: String, origin: Int, target: Int, sequenceNumber: Int) {
  def isRootRow = origin == 1 && target == 1 && sequenceNumber == 1
}