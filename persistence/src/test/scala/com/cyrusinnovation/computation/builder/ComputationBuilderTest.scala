package com.cyrusinnovation.computation.builder

import org.scalatest.{Matchers, FlatSpec}
import org.scalamock.scalatest.MockFactory
import com.cyrusinnovation.computation._
import com.cyrusinnovation.computation.util.{StdOutLogger, ComputationEngineLog, Log}
import com.cyrusinnovation.computation.specification.Version
import com.cyrusinnovation.computation.util.TestUtils._
import com.cyrusinnovation.computation.persistence.reader.{CsvReaderConfig, CsvDataReader, YamlReader}

class ComputationBuilderTest extends FlatSpec with Matchers with MockFactory {
  val log: ComputationEngineLog = ComputationEngineLog(StdOutLogger)
  val loggers : Map[String, Log] = Map("computationLogger" -> log)
  val securityConfigurations: Map[String, SecurityConfiguration] = Map("testSecurityConfiguration" -> TestSecurityConfiguration)

  //NOTE: Run configurations for tests involving security policy must have the module's root directory as the working directory,
  //in order to pick up the security policy file for the TestSecurityConfiguration.
  "The computation builder" should "build the correct computations given a version number, a reader, security configurations, and loggers" in {
    val reader = CsvDataReader.fromFileOnClasspath("/sampleNodes.csv", "/sampleEdges.csv", "test", "1.0", CsvReaderConfig(1))
    val computations = ComputationBuilder.build("1.0", reader, securityConfigurations, loggers)

    computations.size should be(4)

    val firstComputation = computations("test.computations.MaximumTestValueComputation").asInstanceOf[SimpleComputation]
    val resultsForFirstComputation = firstComputation.compute(Map('testValues -> Map("first" -> 1, "second" -> 5, "third" -> 3)))
    resultsForFirstComputation(firstComputation.resultKey) should be(Map("second" -> 5))

    val fourthComputation = computations("test.computations.FoldingSumComputation").asInstanceOf[FoldingComputation[Int, List[Int]]]
    val resultsForFourthComputation = fourthComputation.compute(Map('initialAccumulator -> 5, 'testValues -> List(1, 2, 3, 4, 5)))
    resultsForFourthComputation(fourthComputation.resultKey) should be(20)

  }

  "The computation builder" should "generate the computations for the library version" in {
    val version = createLibraryVersion("1.0")

    val computationBuilder = new ComputationBuilder(version, securityConfigurations, loggers)
    val computations : Map[String, Computation] = computationBuilder.build

    computations.size should be(4)

    val firstComputation = computations("test.computations.MaximumTestValueComputation").asInstanceOf[SimpleComputation]    
    assertResult('maxTestValue)(firstComputation.resultKey) // Can't use "should" matcher on a symbol

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
  }

  def createLibraryVersion(versionNumber: String) : Version = {
    val reader = YamlReader.fromFileOnClasspath("/sample.yaml")
    val library = reader.unmarshal
    library.verifyNoCyclicalReferences()

    library.versions(versionNumber)
  }
}
