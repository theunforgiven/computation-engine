package com.cyrusinnovation.computation.persistence.reader

import com.cyrusinnovation.computation.specification.Library

trait Reader {
  def unmarshal: Library
}
