package com.cyrusinnovation.computation.persistence.writer

import com.cyrusinnovation.computation.specification.Library

trait Writer {
  def write(library: Library)
}
