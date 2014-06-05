package com.cyrusinnovation.computation.persistence.writer

import java.io.{OutputStreamWriter, OutputStream}
import org.yaml.snakeyaml.{DumperOptions, Yaml}
import com.cyrusinnovation.computation.specification.Library

object YamlWriter {
  def forOutputStream(outputStream: OutputStream): Writer = {
    val opts = new DumperOptions()
    //When writing yaml: Use indents to denote objects instead of { }
    //This prevents small objects like library and version from being rolled up into one line
    opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
    new YamlWriter(outputStream, new Yaml(opts))
  }
}

class YamlWriter(stream: OutputStream, snakeYaml: Yaml) extends Writer {
  override def write(library: Library) {
    val nodeContext = YamlExtractor.marshal(library)
    val transformed = YamlDataTransformer.convertNodeToSnakeYamlMaps(nodeContext)
    val streamWriter = new OutputStreamWriter(stream)
    try {
      snakeYaml.dump(transformed, streamWriter)
    } finally {
      streamWriter.close()
    }
  }
}
