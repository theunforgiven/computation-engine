package com.cyrusinnovation.computation.persistence.writer

import com.cyrusinnovation.computation.persistence.reader.{SqlTableReader, YamlReader, CsvReaderConfig, CsvDataReader}
import java.io.{OutputStream, ByteArrayInputStream, StringReader, ByteArrayOutputStream}
import org.scalatest.{Matchers, FlatSpec}
import com.cyrusinnovation.computation.util.SampleLibraryVerifier
import java.sql.DriverManager

class WriterTest extends FlatSpec with Matchers with SampleLibraryVerifier {
  "A CSV Writer" should "be able to write a CSV file from a Library" in {
    val tableReader = CsvDataReader.fromFileOnClasspath("/sampleNodes.csv", "/sampleEdges.csv", "test", "1.0", CsvReaderConfig(1))
    val library = tableReader.unmarshal
    val edgeOutputStream = new ByteArrayOutputStream()
    val nodeOutputStream = new ByteArrayOutputStream()
    CsvDataWriter.forOutputStream(nodeOutputStream, edgeOutputStream).write(library)
    val nodeText = byteStreamToReader(nodeOutputStream)
    val edgeText = byteStreamToReader(edgeOutputStream)
    val rereadCsvLibrary = CsvDataReader.fromJavaIoReader(nodeText, edgeText, "test", "1.0")
    verifyThatLibraryIsConstructedProperly(rereadCsvLibrary)
  }

  "A Yaml Writer" should "be able to write a YAML file from a Library" in {
    val yamlReader = YamlReader.fromFileOnClasspath("/sample.yaml")
    val writtenYamlBytes = byteArrayCapturingOutputStream { stream =>
      YamlWriter.forOutputStream(stream).write(yamlReader.unmarshal)
    }

    val rereadYamlLibrary = YamlReader.fromInputStream(new ByteArrayInputStream(writtenYamlBytes))
    verifyThatLibraryIsConstructedProperly(rereadYamlLibrary)
  }

  "A SQL Writer" should "be able to persist to a SQL database from a Library" in {
    val sqlReader = SqlTableReader.fromJdbcUrl("test", "1.0", "jdbc:h2:./src/test/resources/h2-sample", Some("public"))

    val connection = DriverManager.getConnection("jdbc:h2:mem:;INIT=RUNSCRIPT FROM './src/main/resources/schema/schema.sql'")
    SqlWriter.forJdbcConnection(connection).write(sqlReader.unmarshal)

    val rereadSqlLibrary = SqlTableReader.fromJdbcConnection("test", "1.0", connection, Some("public"))
    verifyThatLibraryIsConstructedProperly(rereadSqlLibrary)
  }

  private def byteArrayCapturingOutputStream(whatToDo: => (OutputStream) => Unit): Array[Byte] = {
    val stream = new ByteArrayOutputStream()
    try {
      whatToDo(stream)
      stream.toByteArray
    } finally {
      stream.close()
    }
  }

  private def byteStreamToReader(stream: ByteArrayOutputStream): StringReader = {
    new StringReader(new String(stream.toByteArray))
  }
}
