package com.jeedur

import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.rest.graphdb.RestGraphDatabase

object Counters {
  def get(db: GraphDatabaseAPI, name: String): Int = {
    val tx = db.beginTx()
    try {
      val new_id = db.getNodeById(0).getProperty(name + "_counter", 0).asInstanceOf[Int]
      db.getNodeById(0).setProperty(name + "_counter", new_id + 1)
      tx.success()
      new_id
    } finally {
      tx.finish()
    }
  }

  def get(name: String): Int = {
    val db = new RestGraphDatabase(RestApiServlet.neo4jURI)
    get(db, name)
  }
}
