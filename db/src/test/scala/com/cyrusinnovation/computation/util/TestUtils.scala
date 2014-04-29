package com.cyrusinnovation.computation.util

import org.slf4j.{Marker, Logger}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

object TestUtils {
    
  //2014-04-07T09:30:10Z
  def time(xmlTimeString: String): DateTime = {
    val formatter = ISODateTimeFormat.dateTimeParser()
    val dateTime: DateTime = formatter.parseDateTime(xmlTimeString)
    dateTime
  }

  def normalizeSpace(stringWithWhitespace: String) = {
    val trimmed = stringWithWhitespace.trim
    trimmed.replaceAll("\\s+", " ")
  }
  
}

object StdOutLogger extends Logger {
  def error(marker: Marker, msg: String, t: Throwable) = println(s"ERROR: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def error(marker: Marker, format: String, argArray: Array[AnyRef]) = ???
  def error(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def error(marker: Marker, format: String, arg: scala.Any) = ???
  def error(marker: Marker, msg: String) = println(s"ERROR: $msg")
  def error(msg: String, t: Throwable) = println(s"ERROR: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def error(format: String, argArray: Array[AnyRef]) = ???
  def error(format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def error(format: String, arg: scala.Any) = ???
  def error(msg: String) = println(s"ERROR: $msg")

  def warn(marker: Marker, msg: String, t: Throwable) = println(s"WARN: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def warn(marker: Marker, format: String, argArray: Array[AnyRef]) = ???
  def warn(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def warn(marker: Marker, format: String, arg: scala.Any) = ???
  def warn(marker: Marker, msg: String) = println(s"WARN: $msg")
  def warn(msg: String, t: Throwable) = println(s"WARN: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def warn(format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def warn(format: String, argArray: Array[AnyRef]) = ???
  def warn(format: String, arg: scala.Any) = ???
  def warn(msg: String) = println(s"WARN: $msg")

  def info(marker: Marker, msg: String, t: Throwable) = println(s"INFO: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def info(marker: Marker, format: String, argArray: Array[AnyRef]) = ???
  def info(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def info(marker: Marker, format: String, arg: scala.Any) = ???
  def info(marker: Marker, msg: String) = println(s"INFO: $msg")
  def info(msg: String, t: Throwable) = println(s"INFO: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def info(format: String, argArray: Array[AnyRef]) = ???
  def info(format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def info(format: String, arg: scala.Any) = ???
  def info(msg: String) = println(s"INFO: $msg")

  def debug(marker: Marker, msg: String, t: Throwable) = println(s"DEBUG: ${t.getMessage}\n${t.printStackTrace()}")
  def debug(marker: Marker, format: String, argArray: Array[AnyRef]) = ???
  def debug(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def debug(marker: Marker, format: String, arg: scala.Any) = ???
  def debug(marker: Marker, msg: String) = println(s"DEBUG: $msg")
  def debug(msg: String, t: Throwable) = println(s"DEBUG: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def debug(format: String, argArray: Array[AnyRef]) = ???
  def debug(format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def debug(format: String, arg: scala.Any) = ???
  def debug(msg: String) = println(s"DEBUG: $msg")

  def trace(marker: Marker, msg: String, t: Throwable) = println(s"TRACE: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def trace(marker: Marker, format: String, argArray: Array[AnyRef]) = ???
  def trace(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def trace(marker: Marker, format: String, arg: scala.Any) = ???
  def trace(marker: Marker, msg: String) = println(s"TRACE: $msg")
  def trace(msg: String, t: Throwable) = println(s"TRACE: $msg: ${t.getMessage}\n${t.printStackTrace()}")
  def trace(format: String, argArray: Array[AnyRef]) = ???
  def trace(format: String, arg1: scala.Any, arg2: scala.Any) = ???
  def trace(format: String, arg: scala.Any) = ???
  def trace(msg: String) = println(s"TRACE: $msg")

  def getName: String = ???
  def isTraceEnabled: Boolean = true
  def isTraceEnabled(marker: Marker): Boolean = true
  def isDebugEnabled: Boolean = true
  def isDebugEnabled(marker: Marker): Boolean = true
  def isInfoEnabled: Boolean = true
  def isInfoEnabled(marker: Marker): Boolean = true
  def isWarnEnabled: Boolean = true
  def isWarnEnabled(marker: Marker): Boolean = true
  def isErrorEnabled: Boolean = true
  def isErrorEnabled(marker: Marker): Boolean = true
}