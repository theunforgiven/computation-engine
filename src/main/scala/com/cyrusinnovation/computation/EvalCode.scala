package com.cyrusinnovation.computation

import java.io.File
import java.util.UUID
import java.io.FileWriter
import com.googlecode.scalascriptengine.{Config, SSESecurityManager, ClassLoaderConfig, ScalaScriptEngine}

/**
 * Based on code by kostantinos.kougios for ScalaScriptEngine, hacked to support parameterized types, imports, etc.
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
	def apply[A1, R](packageName: String, imports: List[String], computationName: String, argVar: String, argType: String, body: String, returnType: String): EvalCode[A1 => R] =
		new EvalCodeImpl(classOf[A1 => R], packageName, imports, computationName, List(argType, returnType), List(argVar), body)
}

private class EvalCodeImpl[T](clz: java.lang.Class[T],
                              packageName: String,
                              imports: List[String],
                              computationName: String,
                              typeArgs: List[String],
                              argNames: List[String],
                              body: String
                              )
                              extends EvalCode[T] {

  private lazy val packageWhitelist = allowedPackageNames
  private lazy val classBlacklist = blacklistedFullyQualifiedClassNames

  def allowedPackageNames: Set[String] = Set("java.lang",
                                             "java.math",
                                             "java.text",
                                             "java.util",
                                             "java.util.regex",
                                             "scala",
                                             "scala.collection",
                                             "scala.collection.generic",
                                             "scala.collection.immutable",
                                             "scala.collection.mutable",
                                             "scala.math",
                                             "scala.runtime",
                                             "scala.util",
                                             "scala.util.matching",
                                             packageName
                                            )

  def blacklistedFullyQualifiedClassNames: Set[String] = Set("java.util.EventListener",
                                                             "java.util.EventObject",
                                                             "java.util.EventListenerProxy",
                                                             "java.util.ServiceLoader",
                                                             "java.util.Timer",
                                                             "java.util.Timer",
                                                             "java.util.TimerTask",
                                                             "scala.runtime.Runtime",
                                                             "scala.runtime.ScalaRuntime",
                                                             "scala.runtime.MethodCache",
                                                             "scala.runtime.MegaMethodCache",
                                                             "scala.runtime.EmptyMethodCache",
                                                             "scala.util.Marshal"
                                                            )

  private val srcFolder = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID.toString)
	if (!srcFolder.mkdir) throw new IllegalStateException("can't create temp folder %s".format(srcFolder))

	private val config: Config = ScalaScriptEngine.defaultConfig(srcFolder).copy(
    classLoaderConfig = ClassLoaderConfig.Default.copy(
      allowed = { (thePackage, name) => packageWhitelist(thePackage) &&
                                        ! classBlacklist(name) }
    )
  )

  // TODO Establish and document location and configuration of computation.security.policy file
  System.setProperty("script.classes", config.sourcePaths.head.targetDir.toURI.toString)
  System.setProperty("java.security.policy", new File("computation.security.policy").toURI.toString)
  val sseSM = new SSESecurityManager(new SecurityManager)
  System.setSecurityManager(sseSM)

	private val templateTop = """
    package %s
    %s
		class %s extends %s%s {
			override def apply(%s):%s = {
			  %s
			}
		}""".format(packageName,

                // import statements
                imports.map { className => s"import ${className}" }.mkString("\n"),

		        // class name
                computationName,

		        // superclass name
                clz.getName,

                // type args
                if (typeArgs.isEmpty) ""
                else "[" + typeArgs.mkString(",") + "]",

                // params
                (argNames zip typeArgs).map {
                  case (pName, e) =>
                    val typeName = e
                    pName + " : " + typeName
                }.mkString(","),

                // return type
                typeArgs.last,

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

	// the Class[T]
	val generatedClass = sse.get[T](s"$packageName.$computationName")

	// creates a new instance of the evaluated class
	def newInstance: T = sseSM.secured { generatedClass.newInstance }
}
