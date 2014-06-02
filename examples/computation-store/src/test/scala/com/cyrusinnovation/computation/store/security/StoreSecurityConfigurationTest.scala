package com.cyrusinnovation.computation.store.security

import org.scalatest.{Matchers, FlatSpec}
import scala.io.Source
import java.net.URI

class StoreSecurityConfigurationTest extends FlatSpec with Matchers{
  "Store security configuration" should "be able to resolve the store-security.policy file" in {
    val policyUri = new URI(StoreSecurityConfiguration.securityPolicyURI)
    val policyLines = Source.fromFile(policyUri).getLines().toList
    policyLines should not be empty
  }
}
