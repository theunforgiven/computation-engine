package com.cyrusinnovation.computation

import java.io.File

object TestSecurityConfiguration extends SecurityConfiguration {
    override def securityPolicyURI = new File("src/test/resources/sample.policy").toURI.toString
  }

