package com.cyrusinnovation.computation.specification

trait SyntaxTreeNode {
  def verifyNoCyclicalReferences(topLevelSpecificationMap: Map[String, TopLevelComputationSpecification], refNodesVisited: Set[Ref]) : Set[Ref] = this match {
    case thisRefNode: Ref => {
      if(refNodesVisited.contains(thisRefNode)) {
        throw new InvalidComputationSpecException("Computation hierarchy may not contain cyclical references")
      } else {
        val nextNodeToVisit = topLevelSpecificationMap(thisRefNode.referencedSpecification)
        nextNodeToVisit.verifyNoCyclicalReferences(topLevelSpecificationMap, (refNodesVisited + thisRefNode))
      }
    }
    case _ => {
      children.foldLeft(refNodesVisited){
        (nodesVisitedSoFar, node) => node.verifyNoCyclicalReferences(topLevelSpecificationMap, nodesVisitedSoFar)
      }
    }
  }

  def children : List[SyntaxTreeNode]
}

case class SyntaxTreeTextNode(text: String) extends SyntaxTreeNode {
  def children = List()
}
