package com.cyrusinnovation.computation.store

import org.scalatra.ScalatraServlet

class RootServlet extends ScalatraServlet {
  get("/") {
    contentType="text/html"
    <html>
      <head>
        <title>Hello World</title>
      </head>
      <body>
        Hello!
      </body>
    </html>
  }
}
