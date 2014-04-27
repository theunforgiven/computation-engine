package com.cyrusinnovation.computation.db.reader

import org.scalatest.{Matchers, FlatSpec}
import scala.xml.{Elem, XML}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import com.cyrusinnovation.computation.db._
import com.cyrusinnovation.computation.db.Imports
import com.cyrusinnovation.computation.db.MappingComputationFactory
import com.cyrusinnovation.computation.db.Ref
import com.cyrusinnovation.computation.db.AbortIfComputationFactory
import com.cyrusinnovation.computation.db.SimpleComputationFactory
import scala.Some
import com.cyrusinnovation.computation.db.SequentialComputationFactory
import com.cyrusinnovation.computation.db.NamedComputationFactory
import com.cyrusinnovation.computation.db.Inputs
import java.io.InputStream

class ReaderTest extends FlatSpec with Matchers {

  "A library" should "be able to be read from XML" in {
    val inputStream: InputStream = getClass.getResourceAsStream("/sample.xml")
    val nodes: Elem = XML.load(inputStream)
    val reader = new XmlReader(nodes)
    verifyAST(reader)
  }
  
  def verifyAST(reader: Reader) = {
    val root = reader.unmarshal
    root.name should be("test")
    root.versions.size should be(1)
    
    val version = root.versions.get("1.0").get
    version.versionNumber should be("1.0")
    version.state should be(Editable)
    version.commitDate should be(None)
    version.lastEditDate should be(Some(time("2014-04-07T09:30:10Z")))
    version.topLevelFactories.size should be(4)

    val firstTopLevelFactory = version.topLevelFactories.get("test.computations.MaximumTestValueComputation").get.asInstanceOf[SimpleComputationFactory]
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

    val secondTopLevelFactory = version.topLevelFactories.get("test.computations.AbortIfContainsMapWithDesiredEntry").get.asInstanceOf[AbortIfComputationFactory]

    secondTopLevelFactory.packageValue should be("test.computations")
    secondTopLevelFactory.name should be("AbortIfContainsMapWithDesiredEntry")
    secondTopLevelFactory.description should be("See if the value is a map with one key 'b and value 5")
    secondTopLevelFactory.changedInVersion should be("1.0")
    secondTopLevelFactory.shouldPropagateExceptions should be(false)
    normalizeSpace(secondTopLevelFactory.predicateExpression) should be("x == MutableMap('b -> 5)")
    secondTopLevelFactory.innerFactory should be(Ref("test.computations.MaximumTestValueComputation"))
    secondTopLevelFactory.imports should be(Imports("scala.collection.mutable.{Map => MutableMap}"))
    secondTopLevelFactory.input should be(Inputs(Mapping("x: MutableMap[Symbol, Int]", "maxTestValue")))
    secondTopLevelFactory.logger should be("computationLogger")
    secondTopLevelFactory.securityConfiguration should be("testSecurityConfiguration")

    val thirdTopLevelFactory = version.topLevelFactories.get("test.computations.SequentialMaxComputation").get.asInstanceOf[NamedComputationFactory]

    thirdTopLevelFactory.packageValue should be("test.computations")
    thirdTopLevelFactory.name should be("SequentialMaxComputation")
    thirdTopLevelFactory.description should be("Compute the maximum and then do a mapping computation")
    thirdTopLevelFactory.changedInVersion should be("1.0")

    val sequentialComputationFactory = thirdTopLevelFactory.factoryForNamableComputation.asInstanceOf[SequentialComputationFactory]
    sequentialComputationFactory.innerFactories.size should be(2)
    val firstInnerFactory = sequentialComputationFactory.innerFactories.head.asInstanceOf[Ref]
    firstInnerFactory should be(Ref("test.computations.MaximumTestValueComputation"))
    val secondInnerFactory = sequentialComputationFactory.innerFactories.tail.head.asInstanceOf[MappingComputationFactory]
    secondInnerFactory.innerFactory should be(Ref("test.computations.AbortIfContainsMapWithDesiredEntry"))
    secondInnerFactory.inputTuple should be(Mapping("testValues: Map[String, Int]", "testValues"))
    secondInnerFactory.resultKey should be("maxTestValue")

    val fourthTopLevelFactory = version.topLevelFactories.get("test.computations.FoldingSumComputation").get.asInstanceOf[NamedComputationFactory]
    fourthTopLevelFactory.packageValue should be("test.computations")
    fourthTopLevelFactory.name should be("FoldingSumComputation")
    fourthTopLevelFactory.description should be("Sum all the values in a sequence")
    fourthTopLevelFactory.changedInVersion should be("1.0")

    val foldingComputationFactory = fourthTopLevelFactory.factoryForNamableComputation.asInstanceOf[FoldingComputationFactory]
    foldingComputationFactory.initialAccumulatorKey should be("initialAccumulator")
    foldingComputationFactory.inputTuple should be(Mapping("testValues", "addend1"))
    foldingComputationFactory.accumulatorTuple should be(Mapping("sumAccumulator", "addend2"))

    val foldedInnerFactory = foldingComputationFactory.innerComputation.asInstanceOf[SimpleComputationFactory]
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