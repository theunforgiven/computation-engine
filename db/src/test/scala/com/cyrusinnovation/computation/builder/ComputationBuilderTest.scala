package com.cyrusinnovation.computation.db.builder

import org.scalatest.{Matchers, FlatSpec}
import org.scalamock.scalatest.MockFactory
import org.slf4j.{Marker, Logger}
import java.io.InputStream
import scala.xml.{XML, Elem}
import com.cyrusinnovation.computation.db.reader.XmlReader
import com.cyrusinnovation.computation.{TestSecurityConfiguration, Computation, SimpleComputation, SecurityConfiguration}
import com.cyrusinnovation.computation.util.{ComputationEngineLog, Log}
import com.cyrusinnovation.computation.db.{Version, Library}
import com.cyrusinnovation.computation.builder.ComputationBuilder

class ComputationBuilderTest extends FlatSpec with Matchers with MockFactory {
  //NOTE: Run configurations for tests involving security policy must have the working directory as the module root,
  //in order to put the security policy file on the classpath
  //TODO document this.
  "The computation builder" should "generate the computations for the library version" in {

    val version = createLibraryVersion("1.0")
    val securityConfigurations: Map[String, SecurityConfiguration] = Map("testSecurityConfiguration" -> TestSecurityConfiguration)
    val loggers : Map[String, Log] = Map("computationLogger" -> ComputationEngineLog(StdOutLogger))

    val computationBuilder = new ComputationBuilder(version, securityConfigurations, loggers)
    val computations : Map[String, Computation] = computationBuilder.build

    val firstComputation = computations("test.computations.MaximumTestValueComputation").asInstanceOf[SimpleComputation]
    assertResult('maxTestValue)(firstComputation.resultKey) // Can't use "should" matcher on a symbol
    val results = firstComputation.compute(Map('testValues -> Map("first" -> 1, "second" -> 5, "third" -> 3)))
    results(firstComputation.resultKey) should be(Map("second" -> 5))
  }

  def createLibraryVersion(versionNumber: String) : Version = {
    val inputStream: InputStream = getClass.getResourceAsStream("/sample.xml")
    val nodes: Elem = XML.load(inputStream)
    val reader = new XmlReader(nodes)
    val library = reader.unmarshal
    library.verifyNoCyclicalReferences()

    val securityConfigurations: Map[String, SecurityConfiguration] = Map("testSecurityConfiguration" -> TestSecurityConfiguration)
    val loggers : Map[String, Log] = Map("computationLogger" -> ComputationEngineLog(StdOutLogger))

    library.versions(versionNumber)
  }
}

object StdOutLogger extends Logger {
  def error(marker: Marker, msg: String, t: Throwable) = println(s"ERROR: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def error(marker: Marker, format: String, argArray: Array[AnyRef]) = ???
  def error(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def error(marker: Marker, format: String, arg: scala.Any) = ???
  def error(marker: Marker, msg: String) = println(s"ERROR: $msg")
  def error(msg: String, t: Throwable) = println(s"ERROR: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def error(format: String, argArray: Array[AnyRef]) = ???
  def error(format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def error(format: String, arg: scala.Any) = ???
  def error(msg: String) = println(s"ERROR: $msg")

  def warn(marker: Marker, msg: String, t: Throwable) = println(s"WARN: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def warn(marker: Marker, format: String, argArray: Array[AnyRef]) = ???
  def warn(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def warn(marker: Marker, format: String, arg: scala.Any) = ???
  def warn(marker: Marker, msg: String) = println(s"WARN: $msg")
  def warn(msg: String, t: Throwable) = println(s"WARN: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def warn(format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def warn(format: String, argArray: Array[AnyRef]) = ???
  def warn(format: String, arg: scala.Any) = ???
  def warn(msg: String) = println(s"WARN: $msg")

  def info(marker: Marker, msg: String, t: Throwable) = println(s"INFO: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def info(marker: Marker, format: String, argArray: Array[AnyRef]) = ???
  def info(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def info(marker: Marker, format: String, arg: scala.Any) = ???
  def info(marker: Marker, msg: String) = println(s"INFO: $msg")
  def info(msg: String, t: Throwable) = println(s"INFO: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def info(format: String, argArray: Array[AnyRef]) = ???
  def info(format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def info(format: String, arg: scala.Any) = ???
  def info(msg: String) = println(s"INFO: $msg")

  def debug(marker: Marker, msg: String, t: Throwable) = println(s"DEBUG: ${t.getMessage}\n${t.printStackTrace()}")
  def debug(marker: Marker, format: String, argArray: Array[AnyRef]) = ???
  def debug(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def debug(marker: Marker, format: String, arg: scala.Any) = ???
  def debug(marker: Marker, msg: String) = println(s"DEBUG: $msg")
  def debug(msg: String, t: Throwable) = println(s"DEBUG: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def debug(format: String, argArray: Array[AnyRef]) = ???
  def debug(format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def debug(format: String, arg: scala.Any) = ???
  def debug(msg: String) = println(s"DEBUG: $msg")

  def trace(marker: Marker, msg: String, t: Throwable) = println(s"TRACE: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def trace(marker: Marker, format: String, argArray: Array[AnyRef]) = ???
  def trace(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def trace(marker: Marker, format: String, arg: scala.Any) = ???
  def trace(marker: Marker, msg: String) = println(s"TRACE: $msg")
  def trace(msg: String, t: Throwable) = println(s"TRACE: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def trace(format: String, argArray: Array[AnyRef]) = ???
  def trace(format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def trace(format: String, arg: scala.Any) = ???
  def trace(msg: String) = println(s"TRACE: $msg")

  def getName: String = ???
  def isTraceEnabled: Boolean = true
  def isTraceEnabled(marker: Marker): Boolean = true
  def isDebugEnabled: Boolean = true
  def isDebugEnabled(marker: Marker): Boolean = true
  def isInfoEnabled: Boolean = true
  def isInfoEnabled(marker: Marker): Boolean = true
  def isWarnEnabled: Boolean = true
  def isWarnEnabled(marker: Marker): Boolean = true
  def isErrorEnabled: Boolean = true
  def isErrorEnabled(marker: Marker): Boolean = true
}