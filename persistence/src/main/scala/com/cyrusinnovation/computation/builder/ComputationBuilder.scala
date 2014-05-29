package com.cyrusinnovation.computation.builder

import com.cyrusinnovation.computation._
import com.cyrusinnovation.computation.util.Log
import scala.collection.mutable.{Map => MutableMap}
import com.cyrusinnovation.computation.specification._
import com.cyrusinnovation.computation.AbortIfHasResults
import scala.Some
import com.cyrusinnovation.computation.AbortIfNoResults
import com.cyrusinnovation.computation.specification.AbortIfNoResultsComputationSpecification
import com.cyrusinnovation.computation.specification.AbortIfComputationSpecification
import com.cyrusinnovation.computation.specification.Version
import com.cyrusinnovation.computation.specification.SimpleComputationSpecification
import com.cyrusinnovation.computation.persistence.reader.Reader

object ComputationBuilder {
  def build(versionNumber: String, reader: Reader, securityConfigurations: Map[String, SecurityConfiguration], loggers: Map[String, Log]) : Map[String, Computation] = {
    val library = reader.unmarshal
    library.verifyNoCyclicalReferences()

    val version = library.versions(versionNumber)
    val builder = new ComputationBuilder(version, securityConfigurations, loggers)
    builder.build
  }
}

class ComputationBuilder(version: Version, securityConfigurations: Map[String, SecurityConfiguration], loggers: Map[String, Log]) {
  private val computations = MutableMap[String, Computation]()

  def build : Map[String, Computation] = {
    version.children.foreach((spec: TopLevelComputationSpecification) => build(spec))
    computations.toMap
  }

  // We aggregate top level computations into a mutable map as we go along because Ref nodes need to check as we go along
  // to see if the referenced computations have already been built.
  def build(spec: TopLevelComputationSpecification) : Computation = {
    computations.get(spec.fullyQualifiedName) match {
      case Some(computation) => computation
      case None => {
        val computation = buildComputationFrom(spec)
        computations += spec.fullyQualifiedName -> computation
        computation
      }
    }
  }

  def buildComputationFrom(spec: ComputationSpecification) : Computation = spec match {
    case simpleComputationSpec: SimpleComputationSpecification => visit(simpleComputationSpec)
    case abortIfSpec: AbortIfComputationSpecification => visit(abortIfSpec)
    case abortIfNoResultsSpec: AbortIfNoResultsComputationSpecification => visit(abortIfNoResultsSpec)
    case abortIfHasResultsSpec: AbortIfHasResultsComputationSpecification => visit(abortIfHasResultsSpec)
    case iterativeComputationSpec: IterativeComputationSpecification => visit(iterativeComputationSpec)
    case mappingComputationSpec: MappingComputationSpecification => visit(mappingComputationSpec)
    case foldingComputationSpec: FoldingComputationSpecification => visit(foldingComputationSpec)
    case sequentialComputationSpec: SequentialComputationSpecification => visit(sequentialComputationSpec)
    case namedComputationSpec: NamedComputationSpecification => visit(namedComputationSpec)
    case ref: Ref => visit(ref)
    case other => throw new InvalidBuilderException(s"Computation spec of type ${other.getClass.getCanonicalName} not matched.")
  }

  def visit(spec: SimpleComputationSpecification) : Computation = {
    new SimpleComputation(
      spec.packageValue,
      spec.name,
      spec.description,
      visit(spec.imports),
      spec.computationExpression,
      visit(spec.input),
      Symbol(spec.resultKey),
      securityConfigurations(spec.securityConfiguration),
      loggers(spec.logger),
      spec.shouldPropagateExceptions)
  }
  
  def visit(spec: AbortIfComputationSpecification) : Computation = {
    AbortIf(
      spec.packageValue,
      spec.name,
      spec.description,
      visit(spec.imports),
      spec.predicateExpression,
      visit(spec.input),
      buildComputationFrom(spec.innerSpecification),
      securityConfigurations(spec.securityConfiguration),
      loggers(spec.logger),
      spec.shouldPropagateExceptions)
  }
  
  def visit(spec: AbortIfNoResultsComputationSpecification) : Computation = {
    AbortIfNoResults(buildComputationFrom(spec.inner))
  }
  
  def visit(spec: AbortIfHasResultsComputationSpecification) : Computation = {
    AbortIfHasResults(buildComputationFrom(spec.inner))
  }
  
  def visit(spec: IterativeComputationSpecification) : Computation = {
    new IterativeComputation(
      buildComputationFrom(spec.innerSpecification),
      visitForSymbolTuple(spec.inputTuple),
      Symbol(spec.resultKey))
  }
  
  def visit(spec: MappingComputationSpecification) : Computation = {
    new MappingComputation(
      buildComputationFrom(spec.innerSpecification),
      visitForSymbolTuple(spec.inputTuple),
      Symbol(spec.resultKey))
  }
  
  def visit(spec: FoldingComputationSpecification): Computation = {
    new FoldingComputation(
      Symbol(spec.initialAccumulatorKey),
      visitForSymbolTuple(spec.inputTuple),
      visitForSymbolTuple(spec.accumulatorTuple),
      buildComputationFrom(spec.innerSpecification))
  }
  
  def visit(spec: SequentialComputationSpecification): Computation = {
    new SequentialComputation(
      spec.innerSpecs.map(specification => buildComputationFrom(specification)))
  }

  def visit(spec: NamedComputationSpecification): Computation = {
    buildComputationFrom(spec.specForNamableComputation)
  }

  def visit(spec: Ref): Computation = {
    build(version.topLevelSpecifications(spec.referencedSpecification))
  }

  def visit(importSpec: Imports) : List[String] = {
    importSpec.importSequence.toList
  }

  def visit(inputSpec: Inputs): Map[String, Symbol] = {
    inputSpec.inputMappings.foldLeft(Map[String, Symbol]()) {
        (mapSoFar, mapping) => mapSoFar + visitForInputSpec(mapping)
    }
  }

  def visitForInputSpec(mapping: Mapping): (String, Symbol) = {
    (mapping.key, Symbol(mapping.value))
  }

  def visitForSymbolTuple(mapping: Mapping): (Symbol, Symbol) = {
    (Symbol(mapping.key), Symbol(mapping.value))
  }
}

case class InvalidBuilderException(message: String) extends RuntimeException(message)