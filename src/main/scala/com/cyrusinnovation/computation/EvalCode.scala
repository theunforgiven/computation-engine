package com.cyrusinnovation.computation

import java.io.File
import java.util.UUID
import java.io.FileWriter
import com.googlecode.scalascriptengine._
import scala.Symbol
import java.security.Policy
import com.googlecode.scalascriptengine.Config

/**
 * Based on code by kostantinos.kougios for ScalaScriptEngine, tailored for this particular application
 */

/**
 * a scala-code evaluator
 */
trait EvalCode[T]
{
	// the Class[T]
	val generatedClass: java.lang.Class[T]

	// creates a new instance of the evaluated class
	def newInstance: T
}

object EvalSimpleComputationString {
	def apply(packageName: String,
            imports: List[String],
            computationName: String,
            body: String,
            securityConfig: SecurityConfiguration): EvalCode[Map[Symbol, Any] => Map[Symbol, Any]] = {

    new EvalCodeImpl[Map[Symbol, Any]](packageName,
                                       imports,
                                       computationName,
                                       body,
                                       "Map[Symbol, Any]",
                                       securityConfig)
  }
}

object EvalPredicateFunctionString {
	def apply(packageName: String,
            imports: List[String],
            computationName: String,
            body: String,
            securityConfig: SecurityConfiguration): EvalCode[Map[Symbol, Any] => Boolean] = {

    new EvalCodeImpl[Boolean](packageName,
                              imports,
                              computationName,
                              body,
                              "Boolean",
                              securityConfig)
  }
}
private class EvalCodeImpl[ResultType](packageName: String,
                                       imports: List[String],
                                       computationName: String,
                                       body: String,
                                       resultTypeName: String,
                                       securityConfig:  SecurityConfiguration
                                       )
                                       extends EvalCode[Map[Symbol, Any] => ResultType] {

  private val sourceDirectory = createSourceDirectory()
  private val classesDir = setTargetDirectory()
  private val config = createConfig(sourceDirectory, classesDir, securityConfig)
  private val sseSM = setupJavaSecurityManager()
  private val sse = ScalaScriptEngine.withoutRefreshPolicy(config, ScalaScriptEngine.currentClassPath)

  useCachedClasses match {
    case Some("t") | Some("y") => ()
    case _ => {
      writeSourceFile(packageName, imports, computationName, body, resultTypeName, sourceDirectory)
      sse.refresh
    }
  }

  val generatedClass = sse.get[Map[Symbol, Any] => ResultType](s"$packageName.$computationName")

	def newInstance: Map[Symbol, Any] => ResultType = sseSM.secured { generatedClass.newInstance() }

  private def createSourceDirectory() : File = {
    val sourceDirectory = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID.toString)
    if (!sourceDirectory.mkdir) throw new IllegalStateException("can't create temp folder %s".format(sourceDirectory))
    sourceDirectory
  }

  private def setTargetDirectory() : File = {
//    Option(System.getProperty("script.classes")) match {
//
//    }
    val targetDirectory = ScalaScriptEngine.tmpOutputFolder
    System.setProperty("script.classes", targetDirectory.toURI.toString)
    targetDirectory
  }

  private def createConfig(srcFolder: File, classesDir: File, securityConfiguration: SecurityConfiguration) : Config = {
    val baseConfig = Config(
      List(SourcePath(srcFolder, classesDir)),
      ScalaScriptEngine.currentClassPath,
      Set()
    )
    val allowedPackageNames = securityConfiguration.allowedPackageNames + packageName
  	baseConfig.copy(
      classLoaderConfig = ClassLoaderConfig.Default.copy(
        allowed = { (thePackage, name) => allowedPackageNames(thePackage) &&
                                          ! securityConfiguration.blacklistedFullyQualifiedClassNames(name) }
      )
    )
  }

  private def setupJavaSecurityManager() : SSESecurityManager = {
    System.setProperty("java.security.policy", securityConfig.securityPolicyURI)
    Policy.getPolicy.refresh()

    val sseSM = new SSESecurityManager(new SecurityManager)
    System.setSecurityManager(sseSM)
    sseSM
  }

  def useCachedClasses: Option[String] = {
    Option(System.getProperty("script.use.cached.classes")).map(x => x.substring(0, 1).toLowerCase)
  }

  private def writeSourceFile(packageName: String,
                      imports: List[String],
                      computationName: String,
                      body: String,
                      resultTypeName: String,
                      srcFolder: File) : Unit = {

    val templateTop = """
       package %s
       %s
      class %s extends %s[Map[Symbol, Any], %s] {
        override def apply(domainFacts: Map[Symbol, Any]): %s = {
          %s
        }
      }""".format(packageName,
                   // import statements
                   imports.map { className => s"import ${className}" }.mkString("\n"),
                   // class name
                   computationName,
                   // superclass name
                   classOf[Map[Symbol, Any] => ResultType].getName,
                   resultTypeName,
                   resultTypeName,
                   body
                   )

    val src = new FileWriter(new File(srcFolder, s"$computationName.scala"))

    try {
      src.write(templateTop)
    } finally {
      src.close()
    }
  }
}