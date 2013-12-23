package com.cyrusinnovation.computation

trait SecurityConfiguration {
  def allowedPackageNames: Set[String]                  = Set("java.lang",
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
                                                              "scala.util.matching"
                                                              )

  def blacklistedFullyQualifiedClassNames: Set[String]  = Set("java.util.EventListener",
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

  def securityPolicyFilepath: String                        = "computation.security.policy"
}

object DefaultSecurityConfiguration extends SecurityConfiguration