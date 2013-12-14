package com.cyrusinnovation.computation

import java.io.File
import java.util.UUID
import java.io.FileWriter
import com.googlecode.scalascriptengine.{Config, SSESecurityManager, ClassLoaderConfig, ScalaScriptEngine}

/**
 * Based on code by kostantinos.kougios for ScalaScriptEngine, hacked to support parameterized types
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

object EvalCode
{
	def apply[T](clz: java.lang.Class[T], computationName: String, typeArgs: List[String], argNames: List[String], body: String): EvalCode[T] =
		new EvalCodeImpl(clz, computationName, typeArgs, argNames, body)

	def with1Arg[A1, R](
    computationName: String,
		arg1Var: String,
    arg1Type: String,
		body: String,
    returnType: String) =
		apply(classOf[A1 => R], computationName: String, List(arg1Type, returnType), List(arg1Var), body)
}

private class EvalCodeImpl[T](clz: java.lang.Class[T],
                              computationName: String,
                              typeArgs: List[String],
                              argNames: List[String],
                              body: String
                              )
                              extends EvalCode[T] {

  private lazy val packageWhitelist = allowedPackageNames
  private lazy val classBlacklist = blacklistedFullyQualifiedClassNames

  def allowedPackageNames: Set[String] = Set("java.lang",
                                             "scala",
                                             "scala.collection",
                                             "scala.collection.immutable",
                                             "scala.collection.mutable",
                                             "scala.math",
                                             "scala.runtime",
                                             "com.cyrusinnovation.computation"
                                            )



  def blacklistedFullyQualifiedClassNames: Set[String] = Set("scala.runtime.Runtime"
                                                             // "scala.runtime.BoxesRunTime"    // Need this. WTF?
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
    package com.cyrusinnovation.computation
		class %s extends %s%s {
			override def apply(%s):%s = {
			  %s
			}
		}""".format(// class name
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
	val generatedClass = sse.get[T](s"com.cyrusinnovation.computation.$computationName")

	// creates a new instance of the evaluated class
	def newInstance: T = sseSM.secured { generatedClass.newInstance }
}
