package com.cyrusinnovation.computation.db

import org.scalatest.{Matchers, FlatSpec}
import java.io.InputStream
import scala.xml.{XML, Elem}
import com.cyrusinnovation.computation.db.reader.XmlReader

class SchemaTest extends FlatSpec with Matchers {

  "A syntax tree" should "not allow cyclical references" in {
    val testAST = Library("test", Map("1.0" -> Version("1.0", VersionState.fromString("Editable"), None, None,

      AbortIfComputationFactory("test.computation", "ComputationA", "Refers to B", "1.0", false, "",
        new Ref("test.computation.ComputationB"), Imports(), Inputs(Mapping("x", "y")), "logger", "config"),

      AbortIfComputationFactory("test.computation", "ComputationB", "Refers to A", "1.0", false, "",
        new Ref("test.computation.ComputationA"), Imports(), Inputs(Mapping("x", "y")), "logger", "config")
    )))

    val thrown = the [InvalidComputationSpecException] thrownBy testAST.verifyNoCyclicalReferences()
    thrown.getMessage should be("Computation hierarchy may not contain cyclical references")
  }

  "A known good syntax tree" should "be verified to have no cyclical references" in {
    val inputStream: InputStream = getClass.getResourceAsStream("/sample.xml")
    val nodes: Elem = XML.load(inputStream)
    val reader = new XmlReader(nodes)
    val library = reader.unmarshal
    library.verifyNoCyclicalReferences()
  }
}
