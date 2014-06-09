package com.cyrusinnovation.computation.store.store

import org.scalatra.{Ok, ScalatraServlet}
import org.scalatra.json.JacksonJsonSupport
import org.json4s.DefaultFormats

abstract class ComputationStoreServlet extends ScalatraServlet with JacksonJsonSupport {
  protected implicit val jsonFormats = DefaultFormats.withBigDecimal
  protected def JsonNoContent = Ok( """{}""")
  before() {
    contentType = formats("json")
  }
}
