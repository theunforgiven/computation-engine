package com.cyrusinnovation.computation

object TestRules {

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
                                                  shouldPropagateExceptions = true)

  val negationComputation = new SimpleComputation("test.computations",
                                                  "NegationComputation",
                                                  "Take the negative of the maxTestValue value",
                                                  List("scala.collection.mutable.{Map => MutableMap}"),
                                                  """{  val maxValue = - (maxTestValue.get('b).getOrElse(0))
                                                        Some(maxValue) }""",
                                                  Map("maxTestValue: MutableMap[Symbol, Int]" -> 'maxTestValue),
                                                  'negTestValue,
                                                  TestSecurityConfiguration,
                                                  shouldPropagateExceptions = true)

  def exceptionThrowingComputation(shouldPropagate: Boolean) = 
    new SimpleComputation("test.computations",
                          "ExceptionThrowingComputation",
                          "",
                          List(),
                          "{ throw new Exception() }",
                          Map("input: Map[String, Int]" -> 'maxTestValue),
                          'unused,
                          TestSecurityConfiguration,
                          shouldPropagateExceptions = shouldPropagate)
}
