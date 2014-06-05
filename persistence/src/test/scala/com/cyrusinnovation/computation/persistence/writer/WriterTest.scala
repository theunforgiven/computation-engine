package com.cyrusinnovation.computation.persistence.writer

import com.cyrusinnovation.computation.persistence.reader.{SqlTableReader, YamlReader, CsvReaderConfig, CsvDataReader}
import java.io.{OutputStream, ByteArrayInputStream, StringReader, ByteArrayOutputStream}
import org.scalatest.{Matchers, FlatSpec}
import com.cyrusinnovation.computation.util.SampleLibraryVerifier
import java.sql.DriverManager
import com.cyrusinnovation.computation.util.TestUtils.using

class WriterTest extends FlatSpec with Matchers with SampleLibraryVerifier {
  "A CSV Writer" should "be able to write a CSV file from a Library" in {
    val tableReader = CsvDataReader.fromFileOnClasspath("/sampleNodes.csv", "/sampleEdges.csv", "test", "1.0", CsvReaderConfig(1))
    val library = tableReader.unmarshal
    val (nodeStreamReader, edgeStreamReader) = using(new ByteArrayOutputStream()) { nodeStream =>
      using(new ByteArrayOutputStream()) { edgeStream =>
        CsvDataWriter.forOutputStream(nodeStream, edgeStream).write(library)
        (byteStreamToReader(nodeStream), byteStreamToReader(edgeStream))
      }
    }

    val rereadCsvLibrary = CsvDataReader.fromJavaIoReader(nodeStreamReader, edgeStreamReader, "test", "1.0")
    verifyThatLibraryIsConstructedProperly(rereadCsvLibrary)
  }

  "A Yaml Writer" should "be able to write a YAML file from a Library" in {
    val yamlReader = YamlReader.fromFileOnClasspath("/sample.yaml")
    val writtenYamlBytesInputStream = using(new ByteArrayOutputStream()) { stream =>
      YamlWriter.forOutputStream(stream).write(yamlReader.unmarshal)
      new ByteArrayInputStream(stream.toByteArray)
    }

    val rereadYamlLibrary = YamlReader.fromInputStream(writtenYamlBytesInputStream)
    verifyThatLibraryIsConstructedProperly(rereadYamlLibrary)
  }

  "A SQL Writer" should "be able to persist to a SQL database from a Library" in {
    val originalLibrary = using(DriverManager.getConnection("jdbc:h2:./src/test/resources/h2-sample")) { connection =>
      SqlTableReader.fromJdbcConnection("test", "1.0", connection, Some("public")).unmarshal
    }

    using(DriverManager.getConnection("jdbc:h2:mem:;INIT=RUNSCRIPT FROM './src/main/resources/schema/schema.sql'")) { connection =>
      SqlWriter.forJdbcConnection(connection).write(originalLibrary)
      val rereadSqlLibrary = SqlTableReader.fromJdbcConnection("test", "1.0", connection, Some("public"))
      verifyThatLibraryIsConstructedProperly(rereadSqlLibrary)
    }
  }

  private def byteStreamToReader(stream: ByteArrayOutputStream): StringReader = {
    new StringReader(new String(stream.toByteArray))
  }
}
