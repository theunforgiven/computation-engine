package com.cyrusinnovation.db

import org.scalatest.{Matchers, FlatSpec}
import org.fgraph.base.DefaultGraph
import org.fgraph.tstore.mem.HashTripleStore
import org.fgraph.{Direction, Edge}
import org.fgraph.traverse.NodeTraverser
import collection.JavaConversions._
import java.util
import java.util.Map.Entry

// import org.fgraph.tstore.sql.PostgresTableAdapter

class FilamentTest extends FlatSpec with Matchers {

  "A filament" should "be able to store a graph of values" in {
    val graph = DefaultGraph.create( HashTripleStore.FACTORY )
    val n1 = graph.newNode ; n1.toString should be("Node[main#-1]")
    val n2 = graph.newNode ; n2.toString should be("Node[main#-2]")
    val n3 = graph.newNode ; n3.toString should be("Node[main#-3]")
    val n4 = graph.newNode ; n4.toString should be("Node[main#-4]")

    val e1 = graph.addEdge( n1, n2, "Demo" ) ; e1.toString should be("Edge[Node[main#-1] -(Demo)-> Node[main#-2]]")
    val e2 = graph.addEdge( n1, n3, "Demo" ) ; e2.toString should be("Edge[Node[main#-1] -(Demo)-> Node[main#-3]]")
    val e3 = graph.addEdge( n1, n4, "Demo" ) ; e3.toString should be("Edge[Node[main#-1] -(Demo)-> Node[main#-4]]")

    e1.otherEnd(n1) should be(n2)
    e2.otherEnd(n1) should be(n3)
    e3.otherEnd(n1) should be(n4)

    n1.put( "name", "Node 1" )
    n1.put( "clusterRoot", Boolean.box(true) )
    n2.put( "name", "Node 2" )
    n3.put( "name", "Node 3" )

    n1.entrySet.toString should be("[name=Node 1, clusterRoot=true]")
    e1.entrySet.toString should be("[]")

    for((edge, index) <- n1.edges("Demo").view.zipWithIndex) {
      edge.put("clusterIndex", index.asInstanceOf[AnyRef])
    }

    n1.entrySet.toString should be("[name=Node 1, clusterRoot=true]")
    e1.entrySet.toString should be("[clusterIndex=0]")
    e2.entrySet.toString should be("[clusterIndex=1]")
    e3.entrySet.toString should be("[clusterIndex=2]")

    val basicTraverser = new NodeTraverser().out( "Demo" )

    for(obj <- basicTraverser.nodes(n1).iterator.toList) {
      println( s"Traversing node: $obj with entry set ${obj.entrySet}" )
    }

    for(step <- basicTraverser.steps(n1).iterator.toList) {
      println( "Step: " + step)
      println( "Target: " + step.getValue.get("name")
                          + "  Edge: " + step.getEdge
                          + "  Edge entry set: " + (if(step.getEdge != null) step.getEdge.entrySet else "")) //First edge is null
      println( "\tdepth: " + step.getDepth
                         + "  index: " + step.getIndex )
      //println( "   child count:" + step.step.getDescendantCount ) //obsolete
    }
  }
}

/* To create a DB-backed Graph:

 import org.fgraph.sql.SqlStoreFactory;

 String jdbcUrl = ...
 String user = ...
 String pword = ...

 Connection conn = DriverManager.getConnection( jdbcUrl, user, pword );

 // User code has complete control over
 // transaction granularity through the JDBC
 // connection standard mechanisms
 conn.setAutoCommit(false);

 SimpleGraph graph = DefaultGraph.create( new SqlStoreFactory(conn) );

 // From there, the graph can be used just like with the in-memory examples.
 // The difference is that changes will be persisted when calling:

 conn.commit().
*/