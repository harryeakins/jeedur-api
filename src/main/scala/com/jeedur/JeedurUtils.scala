package com.jeedur

import javax.servlet.http.HttpServletRequest
import org.neo4j.kernel.GraphDatabaseAPI

object JeedurUtils {
  def getParameter(request: HttpServletRequest, key: String): Option[String] = {
    val string = request.getParameter(key)
    if (string != null) Some(string) else None
  }

  def withinDbTransaction[A](db: GraphDatabaseAPI)(f: => A): A = {
    val tx = db.beginTx()
    try {
      val res = f
      tx.success()
      res
    } finally {
      tx.finish()
    }
  }
}
