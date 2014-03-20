package com.cyrusinnovation.computation.util
/*
 * Copyright 2014 Cyrus Innovation, LLC. Licensed under Apache license 2.0.
 */

import org.slf4j.Logger

/**
 * logging is done via slf4j
 *
 * Keeping Logging implementation separate from
 * the one used in the scripting engine
 *
 */

trait Log {
  def debug(msg: String)
 	def info(msg: String)
 	def warn(msg: String)
 	def error(msg: String)
 	def error(msg: String, e: Throwable)
}

case class ComputationEngineLog(logger: Logger) extends Log {

  def debug(msg: String) = if (logger.isDebugEnabled) logger.debug(msg)

	def info(msg: String) = if (logger.isInfoEnabled) logger.info(msg)

	def warn(msg: String) = logger.warn(msg)

	def error(msg: String) = logger.error(msg)

	def error(msg: String, e: Throwable) = logger.error(msg, e)
}