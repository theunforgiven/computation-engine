package com.cyrusinnovation.computation

import com.cyrusinnovation.computation.util.Log

case class TestRules(noOpLogger: Log) {

  val maxValueComputation = new SimpleComputation("test.computations",
                                                  "MaximumTestValueComputation",
                                                  "Take the maximum of the values of the testValues map",
                                                  List("scala.collection.mutable.{Map => MutableMap}", 
                                                       "scala.collection.mutable.{Set => MutableSet}"),
                                                  """{  val toTestImports = MutableSet()
                                                        val maxTuple = testValues.maxBy(aTuple => aTuple._2)
                                                        Some(MutableMap(maxTuple)) }
                                                  """,
                                                  Map("testValues: Map[String, Int]" -> 'testValues),
                                                  'maxTestValue,
                                                  TestSecurityConfiguration,
                                                  noOpLogger,
                                                  shouldPropagateExceptions = true
                                                  )

  val negationComputation = new SimpleComputation("test.computations",
                                                  "NegationComputation",
                                                  "Take the negative of the maxTestValue value",
                                                  List("scala.collection.mutable.{Map => MutableMap}"),
                                                  """{  val maxValue = - (maxTestValue.get('b).getOrElse(0))
                                                        Some(maxValue) }""",
                                                  Map("maxTestValue: MutableMap[Symbol, Int]" -> 'maxTestValue),
                                                  'negTestValue,
                                                  TestSecurityConfiguration,
                                                  noOpLogger,
                                                  shouldPropagateExceptions = true)

  def exceptionThrowingComputation(shouldPropagate: Boolean, logger: Log = noOpLogger) =
    new SimpleComputation("test.computations",
                          "ExceptionThrowingComputation",
                          "",
                          List(),
                          """{ throw new RuntimeException("Boom") }""",
                          Map("input: Map[String, Int]" -> 'maxTestValue),
                          'unused,
                          TestSecurityConfiguration,
                          logger,
                          shouldPropagateExceptions = shouldPropagate)

  def computationWithSyntaxError(shouldPropagate: Boolean, logger: Log = noOpLogger) =
      new SimpleComputation("test.computations",
                            "ExceptionThrowingComputation",
                            "",
                            List(),
                            "{ val var }",
                            Map("input: Map[String, Int]" -> 'maxTestValue),
                            'unused,
                            TestSecurityConfiguration,
                            logger,
                            shouldPropagateExceptions = shouldPropagate)
}
