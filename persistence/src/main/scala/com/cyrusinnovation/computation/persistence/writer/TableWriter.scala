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
    withoutComputationsWrapper.copy(children = List(createContextNodeList("computations", withoutComputationsWrapper.children)))
  }

  protected override def imports(imports: Imports) = {
    val s = imports.importSequence.map(x =>  createMapNode("import", Map("text" -> x)))
    createContextNodeList("imports", s.toList)
  }

  protected override def sequentialComputationSpec(computation: SequentialComputationSpecification) = {
    val inners = computation.innerSpecs.map(x => createContextNodeList("innerComputation", List(marshal(x))))
    val ictx = createContextNodeList("innerComputations", inners)
    createNode("sequentialComputation", Map(), List(ictx))
  }

  protected override def mapping(mapping: MappingWrapper) = {
    val maps = List(createMapNode("", Map("key" -> mapping.mapping.key)), createMapNode("", Map("value" -> mapping.mapping.value)))
    val mappingNode = createContextNodeList("mapping", maps)
    mapping.label match {
      case "" => mappingNode
      case label: String => createContextNodeList(label, List(mappingNode))
    }
  }

  protected override def createNode(label: String, attrs: Map[String, String], children: List[Node]): Node = {
    EntryNode(label, attrs, children)
  }

  protected override def createNodeList(label: String, children: List[String]): Node = {
    ListNode(label, children)
  }

  protected override def createContextNodeList(label: String, children: List[Node]): Node = {
    ContextListNode(label, children)
  }

  protected override def createMapNode(label: String, children: Map[String, String]): Node = {
    MapNode(label, children)
  }

  protected override def dateTime(d: DateTime): String = {
    TableWriter.formatter.print(d)
  }

  protected override def persist(nodeContext: Node) {
    val context = nodeContext.asInstanceOf[EntryNode]
    val dataRows = parse(1, context, 1, 1)
    val nodes = dataRows.map(x => NodeDataRow(x.id, x.key, x.value))
    val edges = dataRows.map(x => NodeDataEdge(x.origin, x.id, x.sequence))
      .filterNot { case row => row == NodeDataEdge(1, 1, 1)}
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
      case e: ContextListNode => {
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
      case e: ListNode        => {
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

case class NodeDataRow(id: Int, key: String, value: String)

case class NodeDataEdge(origin: Int, target: Int, sequenceNumber: Int)