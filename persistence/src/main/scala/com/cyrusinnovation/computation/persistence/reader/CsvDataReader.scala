package com.cyrusinnovation.computation.persistence.reader

import java.io._
import java.net.URI
import scala.collection.JavaConversions._
import au.com.bytecode.opencsv.{CSVParser, CSVReader}

object CsvDataReader {
  def fromFileOnClasspath(nodeFileResourcePath: String, 
                          edgeFileResourcePath: String, 
                          library: String, 
                          version: String, 
                          config: CsvReaderConfig = CsvReaderConfig()) : Reader = {
    val nodeInputStream: InputStream = getClass.getResourceAsStream(nodeFileResourcePath)
    val edgeInputStream: InputStream = getClass.getResourceAsStream(edgeFileResourcePath)
    fromInputStream(nodeInputStream, edgeInputStream, library, version, config)
  }

  def fromInputStream(nodeInputStream: InputStream,
                      edgeInputStream: InputStream,
                      library: String,
                      version: String,
                      config: CsvReaderConfig = CsvReaderConfig()): Reader = {
    fromJavaIoReader(new InputStreamReader(nodeInputStream), new InputStreamReader(edgeInputStream), library, version, config)
  }

  def fromFile(nodeFilePath: String,
               edgeFilePath: String,
               library: String,
               version: String,
               config: CsvReaderConfig = CsvReaderConfig()) : Reader = {
    fromJavaIoReader(new FileReader(new File(nodeFilePath)), new FileReader(new File(edgeFilePath)), library, version, config)
  }

  def fromFileUri(nodeFileUri: URI, 
                  edgeFileUri: URI,
                  library: String,
                  version: String,
                  config: CsvReaderConfig = CsvReaderConfig()) : Reader = {
    fromJavaIoReader(new FileReader(new File(nodeFileUri)), new FileReader(new File(edgeFileUri)), library, version, config)
  }

  def fromJavaIoReader(nodeFileReader: java.io.Reader, 
                       edgeFileReader: java.io.Reader, 
                       library: String, 
                       version: String, 
                       config: CsvReaderConfig = CsvReaderConfig()) : Reader = {
    val nodeTable: Map[Long, Map[String, String]] = CsvNodeFileParser.parse(nodeFileReader, library, version, config)
    val edgeTable: Map[Long, List[Long]] = CsvEdgeFileParser.parse(edgeFileReader, library, version, config)
    new TableReader(nodeTable, edgeTable)
  }
}

case class CsvReaderConfig(numberOfInitialLinesToSkip: Int = 0,
                           separator: Char = CSVParser.DEFAULT_SEPARATOR,
                           quoteCharacter: Char = CSVParser.DEFAULT_QUOTE_CHARACTER,
                           escapeCharacter: Char = CSVParser.DEFAULT_ESCAPE_CHARACTER,
                           ignoreCharactersOutsideQuotes: Boolean = CSVParser.DEFAULT_STRICT_QUOTES,
                           ignoreWhitespaceBeforeQuotes: Boolean = CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE) {

  def createCsvReaderFrom(reader: java.io.Reader) : CSVReader = {
    new CSVReader(reader, this.separator, this.quoteCharacter, this.escapeCharacter,
      this.numberOfInitialLinesToSkip, this.ignoreCharactersOutsideQuotes, this.ignoreWhitespaceBeforeQuotes)
  }
}

// Expected format: NodeID, Library, Version, Attribute Name, Attribute Value
// CsvReaderConfig could be generalized to allow reading format from column headers, and to skip first line if column headers exist.
object CsvNodeFileParser extends NodeTableDataParser {
  val NODE_ID = 0
  val LIBRARY = 1
  val VERSION = 2
  val ATTRIBUTE_NAME = 3
  val ATTRIBUTE_VALUE = 4

  def parse(reader: java.io.Reader, library: String, version: String, config: CsvReaderConfig = CsvReaderConfig()): Map[Long, Map[String, String]] = {
    val csvReader = config.createCsvReaderFrom(reader)
    val dataForThisVersion: List[(Long, String, String)] = csvReader.readAll().toList
      .filter(row => row(LIBRARY) == library && row(VERSION) == version)
      .map(row => (row(NODE_ID).toLong, row(ATTRIBUTE_NAME), row(ATTRIBUTE_VALUE)))

    parse(dataForThisVersion)
  }
}

// Expected format: Library, Version, Origin, Target, Sequence. (Sequence number operates within rows with the same origin)
// CsvReaderConfig could be generalized to allow reading format from column headers, and to skip first line if column headers exist.
object CsvEdgeFileParser extends EdgeTableDataParser {
  val LIBRARY = 0
  val VERSION = 1
  val ORIGIN = 2
  val TARGET = 3
  val SEQUENCE = 4

  def parse(reader: java.io.Reader, library: String, version: String, config: CsvReaderConfig): Map[Long, List[Long]] = {
    val csvReader = config.createCsvReaderFrom(reader)

    val sortedDataForThisVersion = csvReader.readAll().toList
      .filter(row => row(LIBRARY) == library && row(VERSION) == version)
      .sortWith {
        (row1, row2) => if(row1(ORIGIN) == row2(ORIGIN)) row1(SEQUENCE) < row2(SEQUENCE) else row1(ORIGIN) < row2(ORIGIN)
      }
      .map(row => (row(ORIGIN).toLong, row(TARGET).toLong))

    parse(sortedDataForThisVersion)
  }
}


