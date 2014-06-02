package com.cyrusinnovation.computation.persistence.writer

import org.scalatest.{Matchers, FlatSpec}
import java.io.{StringWriter, StringReader}
import com.cyrusinnovation.computation.util.SampleLibraryVerifier
import com.cyrusinnovation.computation.persistence.reader.{CsvReaderConfig, CsvDataReader}

class CsvDataWriterTest extends FlatSpec with Matchers with SampleLibraryVerifier {
   "A CSV Writer" should "be able to write a CSV file from a Library" in {
     val tableReader = CsvDataReader.fromFileOnClasspath("/sampleNodes.csv", "/sampleEdges.csv", "test", "1.0", CsvReaderConfig(1))
     val library = tableReader.unmarshal
     val nodeTextWriter = new StringWriter()
     val edgeTextWriter = new StringWriter()
     CsvDataWriter.forJavaIoWriter(nodeTextWriter, edgeTextWriter).write(library)
     val nodeText = new StringReader(nodeTextWriter.toString)
     val edgeText = new StringReader(edgeTextWriter.toString)
     val r = CsvDataReader.fromJavaIoReader(nodeText, edgeText, "test", "1.0")
      verifyThatLibraryIsConstructedProperly(r)
   }
 }
