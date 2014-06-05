package com.cyrusinnovation.computation.persistence.reader

import scala.Some
import org.scalatest.{Matchers, FlatSpec}
import com.cyrusinnovation.computation.util.SampleLibraryVerifier
import java.sql.DriverManager
import com.cyrusinnovation.computation.util.TestUtils.using

class ReaderTest extends FlatSpec with Matchers with SampleLibraryVerifier {

  "A YAML Reader" should "be able to read a library from YAML" in {
    val yamlReader = YamlReader.fromFileOnClasspath("/sample.yaml")
    verifyThatLibraryIsConstructedProperly(yamlReader)
  }

  //Depends on current working directory being persistence module directory
  "A Table Reader" should "be able to read a library from a database" in {
    //To re-load load-sample-db.sql on connection append ";INIT=RUNSCRIPT FROM './src/test/resources/load-sample-db.sql'" to the connection string
    using(DriverManager.getConnection("jdbc:h2:./src/test/resources/h2-sample")) { connection =>
      val tableReader = SqlTableReader.fromJdbcConnection("test", "1.0", connection, Some("public"))
      verifyThatLibraryIsConstructedProperly(tableReader)
    }
  }

  "A Table Reader" should "be able to read a library from parsed CSV data" in {
    val tableReader = CsvDataReader.fromFileOnClasspath("/sampleNodes.csv", "/sampleEdges.csv", "test", "1.0", CsvReaderConfig(1))
    verifyThatLibraryIsConstructedProperly(tableReader)
  }
}
