package com.cyrusinnovation.computation

import java.io.File
import java.util.UUID
import java.io.FileWriter
import com.googlecode.scalascriptengine.{Config, SSESecurityManager, ClassLoaderConfig, ScalaScriptEngine}
import scala.Symbol
import java.security.Policy

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

object EvalCode {
	def apply(packageName: String,
            imports: List[String],
            computationName: String,
            body: String,
            securityConfig: SecurityConfiguration): EvalCode[Map[Symbol, Any] => Map[Symbol, Any]] = {

    new EvalCodeImpl(packageName,
                     imports,
                     computationName,
                     body,
                     securityConfig)
  }
}

private class EvalCodeImpl(packageName: String,
                           imports: List[String],
                           computationName: String,
                           body: String,
                           securityConfig:  SecurityConfiguration
                           )
                           extends EvalCode[Map[Symbol, Any] => Map[Symbol, Any]] {

  private val srcFolder = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID.toString)
	if (!srcFolder.mkdir) throw new IllegalStateException("can't create temp folder %s".format(srcFolder))

  System.setProperty("java.security.policy", securityConfig.securityPolicyURI)
  Policy.getPolicy.refresh()

  val sseSM = new SSESecurityManager(new SecurityManager)
  System.setSecurityManager(sseSM)

  private val allowedPackageNames = securityConfig.allowedPackageNames + packageName
	private val config: Config = ScalaScriptEngine.defaultConfig(srcFolder).copy(
    classLoaderConfig = ClassLoaderConfig.Default.copy(
      allowed = { (thePackage, name) => allowedPackageNames(thePackage) &&
                                        ! securityConfig.blacklistedFullyQualifiedClassNames(name) }
    )
  )
  System.setProperty("script.classes", config.sourcePaths.head.targetDir.toURI.toString)

	private val templateTop = """
    package %s
    %s
		class %s extends %s[Map[Symbol, Any], Map[Symbol, Any]] {
			override def apply(domainFacts: Map[Symbol, Any]): Map[Symbol, Any] = {
			  %s
			}
		}""".format(packageName,
                // import statements
                imports.map { className => s"import ${className}" }.mkString("\n"),
                // class name
                computationName,
                // superclass name
                classOf[Map[Symbol, Any] => Map[Symbol, Any]].getName,
                // body
                body
                )

	private val src = new FileWriter(new File(srcFolder, s"$computationName.scala"))

	try {
		src.write(templateTop)
	} finally {
		src.close()
	}

  private val sse = ScalaScriptEngine.onChangeRefresh(config, 0)
  sse.refresh

	val generatedClass = sse.get[Map[Symbol, Any] => Map[Symbol, Any]](s"$packageName.$computationName")

	def newInstance: Map[Symbol, Any] => Map[Symbol, Any] = sseSM.secured { generatedClass.newInstance() }
}
