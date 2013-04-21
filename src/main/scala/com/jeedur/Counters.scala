package com.jeedur

import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.rest.graphdb.RestGraphDatabase
import JeedurUtils._

object Counters {
  def get(db: GraphDatabaseAPI, name: String): Int = {
    withinDbTransaction(db) {
      val new_id = db.getNodeById(0).getProperty(name + "_counter", 0).asInstanceOf[Int]
      db.getNodeById(0).setProperty(name + "_counter", new_id + 1)
      new_id
    }
  }

  def get(name: String): Int = {
    val db = new RestGraphDatabase(RestApiServlet.neo4jURI)
    get(db, name)
  }
}
