package com.cyrusinnovation.computation.persistence.writer

import java.sql.{DriverManager, Connection}
import com.cyrusinnovation.computation.specification.Library

object SqlWriter {
  def forJdbcUrl(url: String, schema: Option[String] = None): Writer = {
    val connection: Connection = DriverManager.getConnection(url)
    forJdbcConnection(connection, schema)
  }

  def forJdbcConnection(connection: Connection, schema: Option[String] = None): Writer = {
    new SqlWriter(connection, schema)
  }
}

class SqlWriter(private val connection: Connection, private val schema: Option[String]) extends Writer {
  def write(library: Library) {
    val nodeContext = LibraryInspectorForTables.marshal(library)
    val (nodes, edges) = TableDataTransformer.extractRowsAndEdges(nodeContext)
    write(nodes, edges)
  }

  protected def write(nodes: List[NodeDataRow], edges: List[NodeDataEdge]): Unit = {
    writeNodes(nodes.sortBy(_.id))
    writeEdges(edges)
  }

  def writeEdges(edgeRows: List[NodeDataEdge]) {
    val edgeStmt = connection.prepareStatement("INSERT INTO COMPUTATION_EDGES(library, version, origin_id, target_id, sequence) VALUES(?, ?, ?, ?, ?);")
    edgeRows.foreach(x => {
      edgeStmt.setString(1, x.libraryName)
      edgeStmt.setString(2, x.versionNumber)
      edgeStmt.setLong(3, x.origin)
      edgeStmt.setLong(4, x.target)
      edgeStmt.setInt(5, x.sequenceNumber)
      edgeStmt.execute()
    })
    edgeStmt.close()
  }

  def writeNodes(nodeRows: List[NodeDataRow]) {
    val nodeStmt = connection.prepareStatement("INSERT INTO COMPUTATION_NODES(id, library, version, attribute_name, attribute_value) VALUES (?, ?, ?, ?, ?)")
    nodeRows.foreach(x => {
      nodeStmt.setLong(1, x.id)
      nodeStmt.setString(2, x.libraryName)
      nodeStmt.setString(3, x.versionNumber)
      nodeStmt.setString(4, x.key)
      nodeStmt.setString(5, x.value)
      nodeStmt.execute()
    })
    nodeStmt.close()
  }
}