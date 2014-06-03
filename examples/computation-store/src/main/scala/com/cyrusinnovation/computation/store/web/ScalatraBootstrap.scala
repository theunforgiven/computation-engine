package com.cyrusinnovation.computation.store.web

import javax.servlet.ServletContext
import org.scalatra.LifeCycle
import org.eclipse.jetty.servlet.DefaultServlet
import com.cyrusinnovation.computation.store.store.ProductsServlet

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context mount (new ProductsServlet, "/products/*")
    context.addServlet("assets", new DefaultServlet()).addMapping("/", "/*")
  }
}
