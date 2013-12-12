package com.cyrusinnovation.computation

import java.io.File
import java.util.UUID
import java.io.FileWriter
import com.googlecode.scalascriptengine.ClassLoaderConfig
import com.googlecode.scalascriptengine.ScalaScriptEngine

/**
 * Based on code by kostantinos.kougios, hacked to support parameterized types
 */

private class EvalCodeImpl[T](
	clz: java.lang.Class[T],
	typeArgs: List[String],
	argNames: List[String],
	body: String,
	classLoaderConfig: ClassLoaderConfig
	)
	extends EvalCode[T]
{

	private val srcFolder = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID.toString)
	if (!srcFolder.mkdir) throw new IllegalStateException("can't create temp folder %s".format(srcFolder))

	private val config = ScalaScriptEngine.defaultConfig(srcFolder).copy(classLoaderConfig = classLoaderConfig)
	private val sse = ScalaScriptEngine.onChangeRefresh(config, 0)

	private val templateTop = """
		class Eval extends %s%s {
			override def apply(%s):%s = { %s }
		}
	                          """.format(
	// super class name
	clz.getName,
	// type args
	if (typeArgs.isEmpty) ""
	else "[" + typeArgs.mkString(",") + "]",
	// params
	(argNames zip typeArgs).map {
		case (pName, e) =>
			val typeName = e
			pName + " : " + typeName
	}.mkString(","), {
		typeArgs.last
	},
	// body
	body
	)

	private val src = new FileWriter(new File(srcFolder, "Eval.scala"))
	try {
		src.write(templateTop)
	} finally {
		src.close()
	}

	// the Class[T]
	val generatedClass = sse.get[T]("Eval")

	// creates a new instance of the evaluated class
	def newInstance: T = generatedClass.newInstance
}

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
	def apply[T](clz: java.lang.Class[T], typeArgs: List[String], argNames: List[String], body: String, classLoaderConfig: ClassLoaderConfig): EvalCode[T] =
		new EvalCodeImpl(clz, typeArgs, argNames, body, classLoaderConfig)

	def withoutArgs[R](body: String, classLoaderConfig: ClassLoaderConfig = ClassLoaderConfig.Default) =
		apply(classOf[() => R], Nil, Nil, body, classLoaderConfig)

	def with1Arg[A1, R](
		arg1Var: String,
    arg1Type: String,
		body: String,
    returnType: String,
		classLoaderConfig: ClassLoaderConfig = ClassLoaderConfig.Default) =
		apply(classOf[A1 => R], List(arg1Type, returnType), List(arg1Var), body, classLoaderConfig)

	def with2Args[A1, A2, R](
		arg1Var: String,
    arg1Type: String,
		arg2Var: String,
    arg2Type: String,
		body: String,
    returnType: String,
		classLoaderConfig: ClassLoaderConfig = ClassLoaderConfig.Default) =
		apply(classOf[(A1, A2) => R], List(arg1Type, arg2Type, returnType), List(arg1Var, arg2Var), body, classLoaderConfig)

	def with3Args[A1, A2, A3, R](
		arg1Var: String,
    arg1Type: String,
		arg2Var: String,
    arg2Type: String,
		arg3Var: String,
    arg3Type: String,
		body: String,
    returnType: String,
		classLoaderConfig: ClassLoaderConfig = ClassLoaderConfig.Default) =
		apply(classOf[(A1, A2, A3) => R], List(arg1Type, arg2Type, arg3Type, returnType), List(arg1Var, arg2Var, arg3Var), body, classLoaderConfig)
}