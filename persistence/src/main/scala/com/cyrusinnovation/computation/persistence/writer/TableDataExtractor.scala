package com.cyrusinnovation.computation.persistence.writer

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import com.cyrusinnovation.computation.specification._
import LibraryExtractor._

object TableDataExtractor extends LibraryExtractor {
  private val formatter = ISODateTimeFormat.dateTime()

  private def createTextContainerNode(label: String, textValue: String) = {
    createNode(label, Map("text" -> textValue), List())
  }

  protected override def version(version: Version) = {
    val withoutComputationsWrapper = super.version(version).asInstanceOf[EntryNode]
    withoutComputationsWrapper.copy(children = List(createNodeListNode("computations", withoutComputationsWrapper.children)))
  }

  protected override def imports(imports: Imports) = {
    val s = imports.importSequence.map(x =>  createTextContainerNode("import", x))
    createNodeListNode("imports", s.toList)
  }

  protected override def sequentialComputationSpec(computation: SequentialComputationSpecification) = {
    val innerComputations = computation.innerSpecs.map(x => createNodeListNode("innerComputation", List(marshal(x))))
    val computationList = createNodeListNode("innerComputations", innerComputations)
    createNode("sequentialComputation", Map.empty, List(computationList))
  }

  protected override def mapping(mapping: Mapping) = {
    val mappingChildren = List(createTextContainerNode("key", mapping.key), createTextContainerNode("value", mapping.value))
    createNode("mapping", Map.empty, mappingChildren)
  }

  protected override def dateTime(d: DateTime): String = {
    formatter.print(d)
  }
}