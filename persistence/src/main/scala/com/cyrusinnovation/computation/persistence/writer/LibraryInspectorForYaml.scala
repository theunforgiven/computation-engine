package com.cyrusinnovation.computation.persistence.writer

import org.joda.time.DateTime
import java.text.SimpleDateFormat

object LibraryInspectorForYaml extends LibraryInspector {
  protected override def dateTime(d: DateTime): String = {
    val formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
    formatter.format(d.toDate)
  }
}
