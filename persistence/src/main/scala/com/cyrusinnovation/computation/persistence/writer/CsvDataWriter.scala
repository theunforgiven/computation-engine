package com.cyrusinnovation.computation.persistence.writer

import au.com.bytecode.opencsv.CSVWriter
import scala.collection.JavaConversions._

object CsvDataWriter {
  def forJavaIoWriter(nodeFileWriter: java.io.Writer,
                      edgeFileWriter: java.io.Writer,
                      config: CsvWriterConfig = CsvWriterConfig()): Writer = {
    new CsvDataWriter(nodeFileWriter, edgeFileWriter, config)
  }
}

class CsvDataWriter(private val nodeFileWriter: java.io.Writer,
                    private val edgeFileWriter: java.io.Writer,
                    private val config: CsvWriterConfig = CsvWriterConfig()) extends TableWriter {
  override protected def write(nodes: List[NodeDataRow], edges: List[NodeDataEdge]): Unit = {
    val nodeRows = nodes.sortBy(_.id).map(x => Array(x.id, "test", "1.0", x.key, x.value).map(_.toString))
    val edgeRows = edges.map(x => Array("test", "1.0", x.origin, x.target, x.sequence).map(_.toString))
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
