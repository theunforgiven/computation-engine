package com.cyrusinnovation.computation.db.builder

import org.scalatest.{Matchers, FlatSpec}
import org.scalamock.scalatest.MockFactory
import java.io.InputStream
import scala.xml.{XML, Elem}
import com.cyrusinnovation.computation.db.reader.XmlReader
import com.cyrusinnovation.computation._
import com.cyrusinnovation.computation.util.{StdOutLogger, ComputationEngineLog, Log}
import com.cyrusinnovation.computation.builder.ComputationBuilder
import com.cyrusinnovation.computation.specification.Version
import com.cyrusinnovation.computation.util.TestUtils._

class ComputationBuilderTest extends FlatSpec with Matchers with MockFactory {
  //NOTE: Run configurations for tests involving security policy must have the module's root directory as the working directory,
  //in order to pick up the security policy file for the TestSecurityConfiguration.
  "The computation builder" should "generate the computations for the library version" in {

    val version = createLibraryVersion("1.0")
    val log: ComputationEngineLog = ComputationEngineLog(StdOutLogger)
    val loggers : Map[String, Log] = Map("computationLogger" -> log)
    val securityConfigurations: Map[String, SecurityConfiguration] = Map("testSecurityConfiguration" -> TestSecurityConfiguration)

    val computationBuilder = new ComputationBuilder(version, securityConfigurations, loggers)
    val computations : Map[String, Computation] = computationBuilder.build

    computations.size should be(4)

    val firstComputation = computations("test.computations.MaximumTestValueComputation").asInstanceOf[SimpleComputation]    
    assertResult('maxTestValue)(firstComputation.resultKey) // Can't use "should" matcher on a symbol
    val resultsForFirstComputation = firstComputation.compute(Map('testValues -> Map("first" -> 1, "second" -> 5, "third" -> 3)))
    resultsForFirstComputation(firstComputation.resultKey) should be(Map("second" -> 5))

    val secondComputation = computations("test.computations.AbortIfContainsMapWithDesiredEntry").asInstanceOf[AbortIf]
    secondComputation.packageName should be("test.computations")
    secondComputation.name should be("AbortIfContainsMapWithDesiredEntry")
    secondComputation.description should be("See if the value is a map with one key 'b and value 5")
    normalizeSpace(secondComputation.predicateExpression) should be("x == MutableMap('b -> 5)")
    secondComputation.inputMapWithTypes should be(Map("x: MutableMap[Symbol, Int]" -> 'maxTestValue))
    secondComputation.inner should be(firstComputation)
    secondComputation.securityConfiguration should be(TestSecurityConfiguration)
    secondComputation.computationEngineLog should be(log)
    secondComputation.shouldPropagateExceptions should be(false)
    
    val thirdComputation = computations("test.computations.SequentialMaxComputation").asInstanceOf[SequentialComputation]
    thirdComputation.steps.head should be(firstComputation)
    val secondInnerComputationOfThirdComputation = thirdComputation.steps.tail.head.asInstanceOf[MappingComputation[String, Int, Map[String, Int]]]
    secondInnerComputationOfThirdComputation.inner should be(secondComputation)
    assertResult('maxTestValue)(secondInnerComputationOfThirdComputation.resultKey)
    
    val fourthComputation = computations("test.computations.FoldingSumComputation").asInstanceOf[FoldingComputation[Int, List[Int]]]
    assertResult('sumAccumulator)(fourthComputation.resultKey)
    val resultsForFourthComputation = fourthComputation.compute(Map('initialAccumulator -> 5, 'testValues -> List(1, 2, 3, 4, 5)))
    resultsForFourthComputation(fourthComputation.resultKey) should be(20)
  }

  def createLibraryVersion(versionNumber: String) : Version = {
    val inputStream: InputStream = getClass.getResourceAsStream("/sample.xml")
    val nodes: Elem = XML.load(inputStream)
    val reader = new XmlReader(nodes)
    val library = reader.unmarshal
    library.verifyNoCyclicalReferences()

    library.versions(versionNumber)
  }
}
