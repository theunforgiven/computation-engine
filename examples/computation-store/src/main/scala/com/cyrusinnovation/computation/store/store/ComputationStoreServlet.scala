package com.cyrusinnovation.computation.store.store

import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport
import org.json4s.DefaultFormats

abstract class ComputationStoreServlet extends ScalatraServlet with JacksonJsonSupport {
  protected implicit val jsonFormats = DefaultFormats.withBigDecimal

  before() {
    contentType = formats("json")
  }
}
