package com.cyrusinnovation.computation
/*
 * Copyright 2014 Cyrus Innovation, LLC. Licensed under Apache license 2.0.
 */

import java.io.File
import org.scalatest.{BeforeAndAfterEach, FlatSpec}
import org.scalatest.Matchers
import org.scalamock.scalatest.MockFactory
import com.cyrusinnovation.computation.util.Log
import com.googlecode.scalascriptengine.ScalaScriptEngine
import java.net.URI

class EvalCodeTests extends FlatSpec with Matchers with MockFactory with BeforeAndAfterEach {

  override def beforeEach = {
    System.getProperties.remove("script.use.cached.classes")
    System.getProperties.remove("script.classes")
  }

  override def afterEach = {
    System.getProperties.remove("script.use.cached.classes")
    System.getProperties.remove("script.classes")
  }

  "EvalCode" should "recompile classes when the script.use.cached.classes system property is not set" in {
    System.getProperty("script.use.cached.classes") should be(null)
    val targetDirectory = Option(System.getProperty("script.classes")) match {
      case Some(dir) => dir
      case None => ScalaScriptEngine.tmpOutputFolder.toURI.toString
    }

    val classFileURI = List(targetDirectory, "test", "computations", "ExceptionThrowingComputation.class").mkString(java.io.File.separator)

    val firstComputation = TestRules(stub[Log]).exceptionThrowingSimpleComputation(shouldPropagate = false)
    val file = new File(new URI(classFileURI))
    waitFor(500) { file.exists() }
    val lastModifiedTime: Long = file.lastModified

    Thread.sleep(1100)

    val secondComputation = TestRules(stub[Log]).exceptionThrowingSimpleComputation(shouldPropagate = true)
    file.lastModified should be > lastModifiedTime
  }

  "EvalCode" should "recompile classes when the script.use.cached.classes system property is not set to true" in {
    System.setProperty("script.use.cached.classes", "false")
    val targetDirectory = Option(System.getProperty("script.classes")) match {
      case Some(dir) => dir
      case None => ScalaScriptEngine.tmpOutputFolder.toURI.toString
    }

    val classFileURI = List(targetDirectory, "test", "computations", "ExceptionThrowingComputation.class").mkString(java.io.File.separator)

    val firstComputation = TestRules(stub[Log]).exceptionThrowingSimpleComputation(shouldPropagate = false)
    val file = new File(new URI(classFileURI))
    waitFor(500) { file.exists() }
    val lastModifiedTime: Long = file.lastModified

    Thread.sleep(1100)

    val secondComputation = TestRules(stub[Log]).exceptionThrowingSimpleComputation(shouldPropagate = true)
    file.lastModified should be > lastModifiedTime
  }

  // May also be set to "t" or "yes" or "y" (case insensitive)
  "EvalCode" should "use cached classes when the script.use.cached.classes system property is set to true" in {
    System.setProperty("script.use.cached.classes", "true")
    val targetDirectory = Option(System.getProperty("script.classes")) match {
      case Some(dir) => dir
      case None => ScalaScriptEngine.tmpOutputFolder.toURI.toString
    }

    val classFileURI = List(targetDirectory, "test", "computations", "ExceptionThrowingComputation.class").mkString(java.io.File.separator)

    val firstComputation = TestRules(stub[Log]).exceptionThrowingSimpleComputation(shouldPropagate = false)
    val file = new File(new URI(classFileURI))
    waitFor(500) { file.exists() }
    val lastModifiedTime: Long = file.lastModified

    Thread.sleep(1100)

    val secondComputation = TestRules(stub[Log]).exceptionThrowingSimpleComputation(shouldPropagate = true)
    file.lastModified should be(lastModifiedTime)
  }

  "EvalCode" should "compile classes to a directory that the user can specify as a URI using the script.classes system property" in {
    val tmpdir = System.getProperty("java.io.tmpdir")
    val targetDirName = List(tmpdir, "special").mkString(java.io.File.separator)
    val targetDir = new java.io.File(targetDirName)

    System.setProperty("script.classes", targetDir.toURI.toString)

    try {
      (! targetDir.exists || targetDir.list.isEmpty) should be(true)

      val computation = TestRules(stub[Log]).noResultsComputation
      targetDir.list.isEmpty should be(false)
      targetDir.list.head should be("test")
    }
    finally {
      targetDir.listFiles.foreach(file => deleteFileTree(file))
      targetDir.delete() should be(true)
    }
  }

  private def waitFor(timeoutMillis: Int)(condition: =>Boolean) = {
    val startTime = System.currentTimeMillis
    while (! condition) {
      Thread.sleep(100)
      if ((System.currentTimeMillis - startTime) > timeoutMillis) { throw new RuntimeException("Failed while waiting") }
    }
  }

  // DANGER! USE CAREFULLY!
  private def deleteFileTree(fileOrDirectory: File) {
    if(fileOrDirectory.isDirectory) {
      fileOrDirectory.listFiles.foreach(file => deleteFileTree(file))
    }
    println(s"Deleting ${fileOrDirectory.getPath}")
    fileOrDirectory.delete()
  }
}
