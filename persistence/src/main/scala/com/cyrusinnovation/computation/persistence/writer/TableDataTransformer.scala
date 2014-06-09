package com.cyrusinnovation.computation.persistence.writer

import com.cyrusinnovation.computation.persistence.writer.LibraryInspector._

object TableDataTransformer {
  def extractRowsAndEdges(rootNode: Node): (List[NodeDataRow], List[NodeDataEdge]) = {
    val node = rootNode.asInstanceOf[CompoundNode]
    val libraryName = node.attrs("name")
    val versionNumber = node.children.head.asInstanceOf[CompoundNode].attrs("versionNumber")
    val dataRows = parse(1, node, 1, 1)
    val nodes = dataRows.map(x => NodeDataRow(libraryName, versionNumber, x.id, x.key, x.value))
    val edges = dataRows.map(x => NodeDataEdge(libraryName, versionNumber, x.origin, x.id, x.sequence))
      .filterNot { case edge => edge.isRootRow }
      .distinct
    (nodes, edges)
  }

  private def parse(newId: Int, context: Node, origin: Int, sequence: Int): List[DataRow] = {
    def nextId(rows: List[DataRow]) = if (rows.isEmpty) newId else rows.maxBy(_.id).id + 1
    context match {
      case e: CompoundNode       => {
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
      case e: MapKeyValueNode => {
        e.children.zipWithIndex.foldLeft(List.empty[DataRow])((soFar, x) => {
          val next = nextId(soFar)
          soFar ::: List(DataRow(next, "label", x._1._1, origin, sequence ), DataRow(next, "text", x._1._2, origin,  sequence))
        })
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

  private case class DataRow(id: Int, key: String, value: String, origin: Int, sequence: Int)
}

case class NodeDataRow(libraryName: String, versionNumber: String, id: Int, key: String, value: String)

case class NodeDataEdge(libraryName: String, versionNumber: String, origin: Int, target: Int, sequenceNumber: Int) {
  def isRootRow = origin == 1 && target == 1 && sequenceNumber == 1
}