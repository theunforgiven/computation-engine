package com.cyrusinnovation.computation.persistence.writer

import org.scalatest.{Matchers, FlatSpec}
import java.io.{OutputStream, ByteArrayInputStream, ByteArrayOutputStream}
import com.cyrusinnovation.computation.util.SampleLibraryVerifier
import com.cyrusinnovation.computation.persistence.reader.YamlReader

class YamlWriterTest extends FlatSpec with Matchers with SampleLibraryVerifier {
  "A Yaml Writer" should "be able to write a YAML file from a Library" in {
    val tableReader = YamlReader.fromFileOnClasspath("/sample.yaml")
    val writtenYamlBytes = captureOutputStream { stream =>
      YamlWriter.forOutputStream(stream).write(tableReader.unmarshal)
    }

    val rereadYamlLibrary = YamlReader.fromInputStream(new ByteArrayInputStream(writtenYamlBytes))
    verifyThatLibraryIsConstructedProperly(rereadYamlLibrary)
  }

  private def captureOutputStream(whatToDo: => (OutputStream) => Unit ): Array[Byte] = {
    val stream = new ByteArrayOutputStream()
    try {
      whatToDo(stream)
      stream.toByteArray
    } finally {
      stream.close()
    }
  }
}
