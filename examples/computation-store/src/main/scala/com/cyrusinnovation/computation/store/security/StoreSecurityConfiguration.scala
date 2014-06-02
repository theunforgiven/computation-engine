package com.cyrusinnovation.computation.store.security

import com.cyrusinnovation.computation.SecurityConfiguration

object StoreSecurityConfiguration extends SecurityConfiguration {
  private val path = this.getClass.getResource("/computation-store.policy").toURI
  override def securityPolicyURI = path.toString
}
