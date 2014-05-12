package com.cyrusinnovation.computation.specification

import org.scalatest.{Matchers, FlatSpec}
import org.scalamock.scalatest.MockFactory
import com.cyrusinnovation.computation.persistence.reader.YamlReader

class SchemaTest extends FlatSpec with Matchers with MockFactory {

  "A library specification" should "not allow cyclical references" in {
    val testAST = Library("test", Map("1.0" -> Version("1.0", VersionState.fromString("Editable"), None, None,

      AbortIfComputationSpecification("test.computation", "ComputationA", "Refers to B", "1.0", false, "",
        new Ref("test.computation.ComputationB"), Imports(), Inputs(Mapping("x", "y")), "logger", "config"),

      AbortIfComputationSpecification("test.computation", "ComputationB", "Refers to A", "1.0", false, "",
        new Ref("test.computation.ComputationA"), Imports(), Inputs(Mapping("x", "y")), "logger", "config")
    )))

    val thrown = the [InvalidComputationSpecException] thrownBy testAST.verifyNoCyclicalReferences()
    thrown.getMessage should be("Computation hierarchy may not contain cyclical references")
  }

  "A known good library specification" should "be verified to have no cyclical references" in {
    val reader = YamlReader.fromFileOnClasspath("/sample.yaml")
    val library = reader.unmarshal
    library.verifyNoCyclicalReferences()
  }
}