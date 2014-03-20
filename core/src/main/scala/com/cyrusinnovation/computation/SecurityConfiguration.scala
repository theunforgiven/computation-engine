package com.cyrusinnovation.computation
/*
 * Copyright 2014 Cyrus Innovation, LLC. Licensed under Apache license 2.0.
 */

/** Establishes the location of the Java SecurityManager policy file, as well as the packages
  * to be allowed in computations. Individual classes within those classes may be blacklisted.
  */
trait SecurityConfiguration {
  /** Specifies a set of package names that will be allowed in computations. Provides a default
    * set with a focus on allowing only classes that have no side effects.
    */
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

  /** Specifies a set of fully-qualified class names that will not be allowed in computations. It is
    * only necessary to blacklist a class if the containing package is whitelisted. Provides a default
    * set with a focus on allowing only classes that have no side effects.
    */
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

  /** Specifies the value to be assigned to the "java.security.policy" System property that
    * indicates the name and location of the policy file for the Java SecurityManager.
    */
  def securityPolicyURI: String                             = "computation.security.policy"
}

/** A default implementation of the SecurityConfiguration trait inheriting the default implementations
  * of all the methods in the trait.
  */
object DefaultSecurityConfiguration extends SecurityConfiguration