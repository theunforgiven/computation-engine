package com.cyrusinnovation.computation.persistence.reader

import java.sql.{ResultSet, PreparedStatement, DriverManager, Connection}

object SqlTableReader {
  def fromJdbcUrl(library: String, version: String, url: String, schema: Option[String] = None): TableReader = {
    val connection: Connection = DriverManager.getConnection(url)
    fromJdbcConnection(library, version, connection, schema)
  }

  def fromJdbcConnection(library: String, version: String, connection: Connection, schema: Option[String] = None): TableReader = {
    val nodeTable = SqlNodeDataParser.nodeTableFor(library, version, connection, schema)
    val edgeTable = SqlEdgeDataParser.edgeTableFor(library, version, connection, schema)
    new TableReader(nodeTable, edgeTable)
  }
}
object SqlNodeDataParser extends NodeTableDataParser {
  
  def nodeTableFor(library: String, version: String, connection: Connection, schema: Option[String] = None) : Map[Long, Map[String, String]] = {
    val theSchema = schema match {
      case Some(sch) => s"${sch}."
      case None => ""
    }
    val sql = s"SELECT ID, ATTRIBUTE_NAME, ATTRIBUTE_VALUE FROM ${theSchema}COMPUTATION_NODES WHERE LIBRARY = ? AND VERSION = ?"
    val statement: PreparedStatement = connection.prepareStatement(sql)
    statement.setString(1, library)
    statement.setString(2, version)
    val nodeResults: ResultSet = statement.executeQuery

    def iterateNodeResultSet(resultSet: ResultSet) : List[(Long, String, String)] = resultSet.next match {
      case false => List()
      case true => {
        val id = resultSet.getLong(1)
        val attributeName = resultSet.getString(2)
        val attributeValue = resultSet.getString(3)
        (id, attributeName, attributeValue) :: iterateNodeResultSet(resultSet)
      }
    }

    val tableResults = iterateNodeResultSet(nodeResults)
    parse(tableResults)
  }
}

object SqlEdgeDataParser extends EdgeTableDataParser {

  def edgeTableFor(library: String, version: String, connection: Connection, schema: Option[String] = None) : Map[Long, List[Long]] = {
    val theSchema = schema match {
      case Some(sch) => s"${sch}."
      case None => ""
    }
    val sql = s"SELECT ORIGIN_ID, TARGET_ID FROM ${theSchema}COMPUTATION_EDGES WHERE LIBRARY = ? AND VERSION = ? ORDER BY ORIGIN_ID ASC, SEQUENCE ASC"
    val statement: PreparedStatement = connection.prepareStatement(sql)
    statement.setString(1, library)
    statement.setString(2, version)
    val edgeResults: ResultSet = statement.executeQuery

    def iterateEdgeResultSet(resultSet: ResultSet) : List[(Long, Long)] = resultSet.next match {
      case false => List()
      case true => {
        val originId = resultSet.getLong(1)
        val targetId = resultSet.getLong(2)
        (originId, targetId) :: iterateEdgeResultSet(resultSet)
      }
    }

    val tableResults = iterateEdgeResultSet(edgeResults)
    parse(tableResults)
  }
}