package com.cyrusinnovation.computation.store.web

import com.cyrusinnovation.computation.store.RootServlet
import javax.servlet.ServletContext
import org.scalatra.LifeCycle
import org.eclipse.jetty.servlet.DefaultServlet

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context mount (new RootServlet, "/root/*")
    context.addServlet("assets", new DefaultServlet()).addMapping("/", "/*")
  }
}
