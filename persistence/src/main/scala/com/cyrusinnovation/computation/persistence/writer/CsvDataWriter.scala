package com.cyrusinnovation.computation.persistence.writer

import au.com.bytecode.opencsv.CSVWriter
import scala.collection.JavaConversions._
import java.io._
import java.net.URI
import com.cyrusinnovation.computation.specification.Library

object CsvDataWriter {
  def fromFile(nodeFilePath: String,
               edgeFilePath: String,
               config: CsvWriterConfig = CsvWriterConfig()) : Writer = {
    forJavaIoWriter(new FileWriter(new File(nodeFilePath)), new FileWriter(new File(edgeFilePath)), config)
  }

  def fromFileUri(nodeFileUri: URI,
                  edgeFileUri: URI,
                  config: CsvWriterConfig = CsvWriterConfig()) : Writer = {
    forJavaIoWriter(new FileWriter(new File(nodeFileUri)), new FileWriter(new File(edgeFileUri)), config)
  }

  def forOutputStream(nodeFileWriter: OutputStream,
                      edgeFileWriter: OutputStream,
                      config: CsvWriterConfig = CsvWriterConfig()): Writer = {
    forJavaIoWriter(new OutputStreamWriter(nodeFileWriter), new OutputStreamWriter(edgeFileWriter), config)
  }

  def forJavaIoWriter(nodeFileWriter: java.io.Writer,
                      edgeFileWriter: java.io.Writer,
                      config: CsvWriterConfig = CsvWriterConfig()): Writer = {
    new CsvDataWriter(nodeFileWriter, edgeFileWriter, config)
  }
}



class CsvDataWriter(private val nodeFileWriter: java.io.Writer,
                    private val edgeFileWriter: java.io.Writer,
                    private val config: CsvWriterConfig = CsvWriterConfig()) extends Writer {
  def write(library: Library) {
    val nodeContext = LibraryInspectorForTables.marshal(library)
    val (nodes, edges) = TableDataTransformer.extractRowsAndEdges(nodeContext)
    write(nodes, edges)
  }

  protected def write(nodes: List[NodeDataRow], edges: List[NodeDataEdge]): Unit = {
    val nodeRows = nodes.sortBy(_.id).map(x => List(x.id, x.libraryName, x.versionNumber, x.key, x.value).map(_.toString).toArray)
    val edgeRows = edges.map(x => List(x.libraryName, x.versionNumber, x.origin, x.target, x.sequenceNumber).map(_.toString).toArray)
    writeRows(nodeFileWriter, nodeRows)
    writeRows(edgeFileWriter, edgeRows)
  }

  private def writeRows(target: java.io.Writer, rows: List[Array[String]]) {
    val writer = config.createCsvWriterFor(target)
    writer.writeAll(rows)
    writer.close()
  }
}

case class CsvWriterConfig(separator: Char = CSVWriter.DEFAULT_SEPARATOR,
                           quoteCharacter: Char = CSVWriter.DEFAULT_QUOTE_CHARACTER,
                           escapeCharacter: Char = CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                           lineEnd: String = CSVWriter.DEFAULT_LINE_END) {

  def createCsvWriterFor(writer: java.io.Writer): CSVWriter = {
    new CSVWriter(writer, this.separator, this.quoteCharacter, this.escapeCharacter, lineEnd)
  }
}
