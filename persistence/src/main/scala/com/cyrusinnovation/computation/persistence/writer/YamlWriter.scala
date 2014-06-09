package com.cyrusinnovation.computation.persistence.writer

import java.io._
import org.yaml.snakeyaml.{DumperOptions, Yaml}
import com.cyrusinnovation.computation.specification.Library
import java.net.URI

object YamlWriter {
  def forFile(path: String) : Writer = {
    val writer = new FileWriter(new File(path))
    forWriter(writer)
  }

  def forFileUri(uri: URI) : Writer = {
    val writer = new FileWriter(new File(uri))
    forWriter(writer)
  }

  def forOutputStream(outputStream: OutputStream): Writer = {
    forWriter(new OutputStreamWriter(outputStream))
  }

  def forWriter(writer: java.io.Writer): Writer = {
    val opts = new DumperOptions()
    //When writing yaml: Use indents to denote objects instead of { }
    //This prevents small objects like library and version from being rolled up into one line
    opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
    new YamlWriter(writer, new Yaml(opts))
  }
}

class YamlWriter(writer: java.io.Writer, snakeYaml: Yaml) extends Writer {
  override def write(library: Library) {
    val nodeContext = LibraryInspectorForYaml.marshal(library)
    val transformed = YamlDataTransformer.convertLibraryNodeToSnakeYamlMaps(nodeContext)
    try {
      snakeYaml.dump(transformed, writer)
    } finally {
      writer.close()
    }
  }
}
