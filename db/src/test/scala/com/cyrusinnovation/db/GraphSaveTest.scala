package com.cyrusinnovation.db

import org.scalatest.{Matchers, FlatSpec}

class GraphSaveTest extends FlatSpec with Matchers {

  "A version" should "be able to be saved under the root node" in {
    val version: Version = Version("1.1")
    val versions = GraphStore.rootNode
    versions.addNodeUnder(version)

    val result = GraphStore.retrieveNode(Version, "1.1")
    result should be(version)
  }

}

case class Version(id: String)
