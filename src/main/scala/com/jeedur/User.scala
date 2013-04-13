package com.jeedur

import org.neo4j.graphdb.Node
import org.joda.time.DateTime

object User {
  def fromNode(node: Node) = {
    implicit def tos(x: AnyRef): String = x.toString
    implicit def toi(x: AnyRef): Option[Int] = Some(x.toString.toInt)
    implicit def todt(x: AnyRef): DateTime = new DateTime(x)

    new User(node.getProperty("username"),
      node.getProperty("user_id"),
      node.getProperty("email"),
      node.getProperty("join_date"),
      node.getProperty("passhash"))
  }
}

class User(val username: String,
           val user_id: Option[Int],
           val email: String,
           val join_date: DateTime,
           val passhash: String) {

  def this(username: String, email: String, passhash: String) =
    this(username, None: Option[Int], email, DateTime.now(), passhash)

  override def toString = {
    "{\"user_id\":\"%s\", \"username\":\"%s\",\"email\":\"%s\",\"join_date\":\"%s\"}"
      .format(if (user_id.isEmpty) "NONE" else user_id.get, username, email, join_date)
  }
}