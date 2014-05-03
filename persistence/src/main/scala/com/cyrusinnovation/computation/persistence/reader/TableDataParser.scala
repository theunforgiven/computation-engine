package com.cyrusinnovation.computation.persistence.reader

trait NodeTableDataParser {

  def parse(dataForThisVersion: List[(Long, String, String)]): Map[Long, Map[String, String]] = {
    val initialAccumulator: Map[Long, Map[String, String]] = Map()

    dataForThisVersion.foldLeft(initialAccumulator) {
      (mapSoFar, valuesForThisRow) => {
        val nodeId = nodeIdFrom(valuesForThisRow)
        val attributeMapForThisNode = mapSoFar.get(nodeId) match {
          case None => Map(attributeNameFrom(valuesForThisRow) -> attributeValueFrom(valuesForThisRow))
          case Some(attributesSoFar) => attributesSoFar + (attributeNameFrom(valuesForThisRow) -> attributeValueFrom(valuesForThisRow))
        }
        mapSoFar + (nodeId -> attributeMapForThisNode)
      }
    }
  }

  def nodeIdFrom(valuesForThisRow: (Long, String, String)): Long = valuesForThisRow._1
  def attributeNameFrom(valuesForThisRow: (Long, String, String)): String = valuesForThisRow._2
  def attributeValueFrom(valuesForThisRow: (Long, String, String)): String = valuesForThisRow._3
}

trait EdgeTableDataParser {

  def parse(sortedDataForThisVersion: List[(Long, Long)]): Map[Long, List[Long]] = {
    val idsToReversedTargetList = sortedDataForThisVersion.foldLeft(Map[Long, List[Long]]()) {
      (mapSoFar, valuesForThisRow) => {
        val originId = originIdFrom(valuesForThisRow)
        val targets = mapSoFar.get(originId) match {
          case None => List(targetIdFrom(valuesForThisRow))
          case Some(targetsSoFar) => targetIdFrom(valuesForThisRow) :: targetsSoFar
        }
        mapSoFar + (originId -> targets)
      }
    }
    idsToReversedTargetList.map(keyValuePair => {
      keyValuePair._1 -> keyValuePair._2.reverse
    })
  }

  def originIdFrom(valuesForThisRow: (Long, Long)): Long = valuesForThisRow._1
  def targetIdFrom(valuesForThisRow: (Long, Long)): Long = valuesForThisRow._2
}
