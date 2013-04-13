package com.jeedur

import org.neo4j.kernel.GraphDatabaseAPI

object Counters {
  def get(db: GraphDatabaseAPI, name: String): String = {
    val tx = db.beginTx()
    try {
      val new_id = db.getNodeById(0).getProperty(name + "_counter", 0).asInstanceOf[Int]
      println("Retrieved value from counter '" + name + "': " + new_id)
      db.getNodeById(0).setProperty(name + "_counter", new_id + 1)
      tx.success()
      new_id.toString
    } finally {
      tx.finish()
    }
  }
}
