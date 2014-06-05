package com.cyrusinnovation.computation
/*
 * Copyright 2014 Cyrus Innovation, LLC. Licensed under Apache license 2.0.
 */

import com.cyrusinnovation.computation.util.Log

case class TestRules(logger: Log) {

  lazy val maxValueComputation = new SimpleComputation("test.computations",
                                                       "MaximumTestValueComputation",
                                                       "Take the maximum of the values of the testValues map",
                                                       List("scala.collection.mutable.{Map => MutableMap}",
                                                            "scala.collection.mutable.{Set => MutableSet}"),
                                                       """val toTestImports = MutableSet()
                                                          val maxTuple = testValues.maxBy(aTuple => aTuple._2)
                                                          Some(MutableMap(maxTuple))
                                                       """,
                                                       Map("testValues: Map[String, Int]" -> 'testValues),
                                                       'maxTestValue,
                                                       TestSecurityConfiguration,
                                                       logger,
                                                       shouldPropagateExceptions = true
                                                       )

  lazy val negationComputation = new SimpleComputation("test.computations",
                                                       "NegationComputation",
                                                       "Take the negative of the maxTestValue value",
                                                       List("scala.collection.mutable.{Map => MutableMap}"),
                                                       """val maxValue = - (maxTestValue.get('b).getOrElse(0))
                                                          Some(maxValue)""",
                                                       Map("maxTestValue: MutableMap[Symbol, Int]" -> 'maxTestValue),
                                                       'negTestValue,
                                                       TestSecurityConfiguration,
                                                       logger,
                                                       shouldPropagateExceptions = true)

  lazy val sumComputation = new SimpleComputation("test.computations",
                                                  "SumComputation",
                                                  "Take the sum of the values passed in",
                                                  List(),
                                                  "Some(addend1 + addend2)",
                                                  Map("addend1:Int" -> 'addend1, "addend2:Int" -> 'addend2),
                                                  'sum,
                                                  TestSecurityConfiguration,
                                                  logger,
                                                  shouldPropagateExceptions = true)

  lazy val noResultsComputation = new SimpleComputation( "test.computations",
                                                         "NoResultsComputation",
                                                         "Return no results",
                                                         List(),
                                                         "None",
                                                         Map("testValues: Map[String, Int]" -> 'testValues),
                                                         'unused,
                                                         TestSecurityConfiguration,
                                                         logger,
                                                         shouldPropagateExceptions = true)

  def exceptionThrowingSimpleComputation(shouldPropagate: Boolean) =
    new SimpleComputation("test.computations",
                          "ExceptionThrowingComputation",
                          "",
                          List(),
                          """throw new RuntimeException("Boom")""",
                          Map("input: Map[String, Int]" -> 'maxTestValue),
                          'unused,
                          TestSecurityConfiguration,
                          logger,
                          shouldPropagateExceptions = shouldPropagate)

  def simpleComputationWithSyntaxError(shouldPropagate: Boolean) =
      new SimpleComputation("test.computations",
                            "ExceptionThrowingComputation",
                            "",
                            List(),
                            "val var",
                            Map("input: Map[String, Int]" -> 'maxTestValue),
                            'unused,
                            TestSecurityConfiguration,
                            logger,
                            shouldPropagateExceptions = shouldPropagate)

  lazy val whitelistViolatingComputation = new SimpleComputation("test.computations",
                                                                 "SecurityWhitelistViolatingComputation",
                                                                 "Use a class from a package that security configuration does not allow",
                                                                 List("java.io.File"),
                                                                 """Some(new File("."))""",
                                                                 Map("input: Map[Symbol, Int]" -> 'input),
                                                                 'unused,
                                                                 TestSecurityConfiguration,
                                                                 logger,
                                                                 shouldPropagateExceptions = true)

  lazy val blacklistViolatingComputation = new SimpleComputation("test.computations",
                                                                 "SecurityBlacklistViolatingComputation",
                                                                 "Use a class that security configuration blacklists",
                                                                 List("java.util.Timer"),
                                                                 "Some(new Timer())",
                                                                 Map("input: Map[Symbol, Int]" -> 'input),
                                                                 'unused,
                                                                 TestSecurityConfiguration,
                                                                 logger,
                                                                 shouldPropagateExceptions = true)

  lazy val javaPolicyViolatingComputation = new SimpleComputation("test.computations",
                                                                  "NoResultsComputation",
                                                                  "Return no results",
                                                                  List(),
                                                                  "None",
                                                                  Map("testValues: Map[String, Int]" -> 'testValues),
                                                                  'unused,
                                                                  RestrictiveTestSecurityConfiguration,
                                                                  logger,
                                                                  shouldPropagateExceptions = true)

  lazy val simpleNegationComputation = new SimpleComputation("test.computations",
                                                             "NegationComputation",
                                                             "Take the negative of the input value",
                                                             List(),
                                                             "Some(- testValue)",
                                                             Map("testValue: Int" -> 'testValue),
                                                             'negTestValue,
                                                             TestSecurityConfiguration,
                                                             logger,
                                                             shouldPropagateExceptions = true)

  def abortIfComputationWithSyntaxError(shouldPropagate: Boolean) =
      AbortIf("test.computations",
              "AbortIfWithSyntaxError",
              "",
              List(),
              "val var",
              Map("input: Map[String, Int]" -> 'maxTestValue),
              simpleNegationComputation,
              TestSecurityConfiguration,
              logger,
              shouldPropagateExceptions = shouldPropagate)

  def exceptionThrowingAbortIf(shouldPropagate: Boolean) =
    AbortIf("test.computations",
             "ExceptionThrowingAbortIf",
             "",
             List("scala.collection.mutable.{Map => MutableMap}"),
             """throw new RuntimeException("Boom")""",
             Map("x: MutableMap[Symbol, Int]" -> maxValueComputation.resultKey),
             maxValueComputation,
             TestSecurityConfiguration,
             logger,
             shouldPropagateExceptions = shouldPropagate)
}
