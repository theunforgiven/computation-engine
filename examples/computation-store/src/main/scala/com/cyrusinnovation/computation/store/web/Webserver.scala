package com.cyrusinnovation.computation.store.web

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import org.eclipse.jetty.servlet.DefaultServlet

object Webserver {
  def startAndListen(port: Int) {
    val server = new Server(port)
    val context = new WebAppContext()
    context.setInitParameter(ScalatraListener.LifeCycleKey, classOf[ScalatraBootstrap].getName)
    context setContextPath "/"
    context.setResourceBase("src/main/webapp")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")

    server.setHandler(context)

    server.start()
    server.join()
  }
}
