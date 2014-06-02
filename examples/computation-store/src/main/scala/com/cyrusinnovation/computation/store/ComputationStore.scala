package com.cyrusinnovation.computation.store

import com.cyrusinnovation.computation.store.web.Webserver

object ComputationStore {
  def main(args: Array[String]) {
    val port = args.headOption.fold(8000)(_.toInt)
    Webserver.startAndListen(port)
  }
}
