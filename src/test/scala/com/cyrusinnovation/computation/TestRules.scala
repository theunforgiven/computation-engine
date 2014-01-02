package com.cyrusinnovation.computation

import com.cyrusinnovation.computation.util.Log

case class TestRules(logger: Log) {

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
                                                  logger,
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
                                                  logger,
                                                  shouldPropagateExceptions = true)

  val noResultsComputation = new SimpleComputation( "test.computations",
                                                    "NoResultsComputation",
                                                    "Return no results",
                                                    List(),
                                                    "{  None }",
                                                    Map("testValues: Map[String, Int]" -> 'testValues),
                                                    'unused,
                                                    TestSecurityConfiguration,
                                                    logger,
                                                    shouldPropagateExceptions = true)

  def exceptionThrowingComputation(shouldPropagate: Boolean) =
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

  def computationWithSyntaxError(shouldPropagate: Boolean) =
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

  val whitelistViolatingComputation = new SimpleComputation("test.computations",
                                                            "SecurityWhitelistViolatingComputation",
                                                            "Use a class from a package that security configuration does not allow",
                                                            List("java.io.File"),
                                                            """{  Some(new File(".")) }""",
                                                            Map("input: Map[Symbol, Int]" -> 'input),
                                                            'unused,
                                                            TestSecurityConfiguration,
                                                            logger,
                                                            shouldPropagateExceptions = true)

  val blacklistViolatingComputation = new SimpleComputation("test.computations",
                                                            "SecurityBlacklistViolatingComputation",
                                                            "Use a class that security configuration blacklists",
                                                            List("java.util.Timer"),
                                                            "{  Some(new Timer()) }",
                                                            Map("input: Map[Symbol, Int]" -> 'input),
                                                            'unused,
                                                            TestSecurityConfiguration,
                                                            logger,
                                                            shouldPropagateExceptions = true)

  lazy val javaPolicyViolatingComputation = new SimpleComputation("test.computations",
                                                                  "NoResultsComputation",
                                                                  "Return no results",
                                                                  List(),
                                                                  "{  None }",
                                                                  Map("testValues: Map[String, Int]" -> 'testValues),
                                                                  'unused,
                                                                  RestrictiveTestSecurityConfiguration,
                                                                  logger,
                                                                  shouldPropagateExceptions = true)
}
