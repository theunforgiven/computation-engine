package com.cyrusinnovation.computation.db.reader

import org.scalatest.{Matchers, FlatSpec}
import scala.xml.{Elem, XML}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import com.cyrusinnovation.computation.db._
import scala.Some
import java.io.InputStream
import com.cyrusinnovation.computation.specification._
import scala.Some
import scala.Some
import scala.Some
import com.cyrusinnovation.computation.specification.Inputs
import com.cyrusinnovation.computation.specification.Imports
import com.cyrusinnovation.computation.specification.MappingComputationSpecification
import com.cyrusinnovation.computation.specification.Mapping
import com.cyrusinnovation.computation.specification.NamedComputationSpecification
import com.cyrusinnovation.computation.specification.AbortIfComputationSpecification
import scala.Some
import com.cyrusinnovation.computation.specification.SequentialComputationSpecification
import com.cyrusinnovation.computation.specification.FoldingComputationSpecification
import com.cyrusinnovation.computation.specification.SimpleComputationSpecification

class ReaderTest extends FlatSpec with Matchers {

  "A library" should "be able to be read from XML" in {
    val inputStream: InputStream = getClass.getResourceAsStream("/sample.xml")
    val nodes: Elem = XML.load(inputStream)
    val reader = new XmlReader(nodes)
    verifyThatLibraryIsConstructedProperly(reader)
  }
  
  def verifyThatLibraryIsConstructedProperly(reader: Reader) = {
    val root = reader.unmarshal
    root.name should be("test")
    root.versions.size should be(1)
    
    val version = root.versions("1.0")
    version.versionNumber should be("1.0")
    version.state should be(Editable)
    version.commitDate should be(None)
    version.lastEditDate should be(Some(time("2014-04-07T09:30:10Z")))
    version.topLevelSpecifications.size should be(4)

    val firstTopLevelFactory = version.topLevelSpecifications("test.computations.MaximumTestValueComputation").asInstanceOf[SimpleComputationSpecification]
    firstTopLevelFactory.packageValue should be("test.computations")
    firstTopLevelFactory.name should be("MaximumTestValueComputation")
    firstTopLevelFactory.description should be("Take the maximum of the values of the testValues map")
    firstTopLevelFactory.changedInVersion should be("1.0")
    firstTopLevelFactory.shouldPropagateExceptions should be(false)
    normalizeSpace(firstTopLevelFactory.computationExpression) should be(normalizeSpace("""
       | val toTestImports = MutableSet()
       | val maxTuple = testValues.maxBy(aTuple => aTuple._2)
       | Some(MutableMap(maxTuple))
       |""".stripMargin))
    firstTopLevelFactory.imports should be(Imports("scala.collection.mutable.{Map => MutableMap}",
                                                        "scala.collection.mutable.{Set => MutableSet}"))
    firstTopLevelFactory.input should be(Inputs(Mapping("testValues: Map[String, Int]", "testValues")))
    firstTopLevelFactory.resultKey should be("maxTestValue")
    firstTopLevelFactory.logger should be("computationLogger")
    firstTopLevelFactory.securityConfiguration should be("testSecurityConfiguration")

    val secondTopLevelFactory = version.topLevelSpecifications("test.computations.AbortIfContainsMapWithDesiredEntry").asInstanceOf[AbortIfComputationSpecification]

    secondTopLevelFactory.packageValue should be("test.computations")
    secondTopLevelFactory.name should be("AbortIfContainsMapWithDesiredEntry")
    secondTopLevelFactory.description should be("See if the value is a map with one key 'b and value 5")
    secondTopLevelFactory.changedInVersion should be("1.0")
    secondTopLevelFactory.shouldPropagateExceptions should be(false)
    normalizeSpace(secondTopLevelFactory.predicateExpression) should be("x == MutableMap('b -> 5)")
    secondTopLevelFactory.innerSpecification.asInstanceOf[Ref].referencedSpecification should be("test.computations.MaximumTestValueComputation")
    secondTopLevelFactory.imports should be(Imports("scala.collection.mutable.{Map => MutableMap}"))
    secondTopLevelFactory.input should be(Inputs(Mapping("x: MutableMap[Symbol, Int]", "maxTestValue")))
    secondTopLevelFactory.logger should be("computationLogger")
    secondTopLevelFactory.securityConfiguration should be("testSecurityConfiguration")

    val thirdTopLevelFactory = version.topLevelSpecifications("test.computations.SequentialMaxComputation").asInstanceOf[NamedComputationSpecification]

    thirdTopLevelFactory.packageValue should be("test.computations")
    thirdTopLevelFactory.name should be("SequentialMaxComputation")
    thirdTopLevelFactory.description should be("Compute the maximum and then do a mapping computation")
    thirdTopLevelFactory.changedInVersion should be("1.0")

    val sequentialComputationFactory = thirdTopLevelFactory.specForNamableComputation.asInstanceOf[SequentialComputationSpecification]
    sequentialComputationFactory.innerSpecs.size should be(2)
    val firstInnerFactory = sequentialComputationFactory.innerSpecs.head.asInstanceOf[Ref]
    firstInnerFactory.asInstanceOf[Ref].referencedSpecification should be("test.computations.MaximumTestValueComputation")
    val secondInnerFactory = sequentialComputationFactory.innerSpecs.tail.head.asInstanceOf[MappingComputationSpecification]
    secondInnerFactory.innerSpecification.asInstanceOf[Ref].referencedSpecification should be("test.computations.AbortIfContainsMapWithDesiredEntry")
    secondInnerFactory.inputTuple should be(Mapping("testValues: Map[String, Int]", "testValues"))
    secondInnerFactory.resultKey should be("maxTestValue")

    val fourthTopLevelFactory = version.topLevelSpecifications("test.computations.FoldingSumComputation").asInstanceOf[NamedComputationSpecification]
    fourthTopLevelFactory.packageValue should be("test.computations")
    fourthTopLevelFactory.name should be("FoldingSumComputation")
    fourthTopLevelFactory.description should be("Sum all the values in a sequence")
    fourthTopLevelFactory.changedInVersion should be("1.0")

    val foldingComputationFactory = fourthTopLevelFactory.specForNamableComputation.asInstanceOf[FoldingComputationSpecification]
    foldingComputationFactory.initialAccumulatorKey should be("initialAccumulator")
    foldingComputationFactory.inputTuple should be(Mapping("testValues", "addend1"))
    foldingComputationFactory.accumulatorTuple should be(Mapping("sumAccumulator", "addend2"))

    val foldedInnerFactory = foldingComputationFactory.innerSpecification.asInstanceOf[SimpleComputationSpecification]
    foldedInnerFactory.packageValue should be("test.computations")
    foldedInnerFactory.name should be("SumComputation")
    foldedInnerFactory.description should be("Take the sum of two addends")
    foldedInnerFactory.changedInVersion should be("1.0")
    foldedInnerFactory.shouldPropagateExceptions should be(false)
    normalizeSpace(foldedInnerFactory.computationExpression) should be("Some(addend1 + addend2)")
    foldedInnerFactory.imports should be(Imports())
    foldedInnerFactory.input should be(Inputs(Mapping("addend1:Int", "addend1"), Mapping("addend2:Int", "addend2")))
    foldedInnerFactory.resultKey should be("sum")
    foldedInnerFactory.logger should be("computationLogger")
    foldedInnerFactory.securityConfiguration should be("testSecurityConfiguration")
  }

  
  //2014-04-07T09:30:10Z
  def time(timeString: String): DateTime = {
    val formatter = ISODateTimeFormat.dateTimeParser()
    val dateTime: DateTime = formatter.parseDateTime(timeString)
    dateTime
  }

  def normalizeSpace(stringWithWhitespace: String) = {
    val trimmed = stringWithWhitespace.trim
    trimmed.replaceAll("\\s+", " ")
  }
}
