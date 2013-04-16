package com.jeedur

import org.neo4j.graphdb.Node
import org.joda.time.DateTime
import com.lambdaworks.crypto.SCryptUtil
import org.neo4j.kernel.GraphDatabaseAPI

object User {
  val EMAIL_REGEX = """\b[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\b""".r

  def from(node: Node): User = {
    implicit def tos(x: AnyRef): String = x.toString
    implicit def todt(x: AnyRef): DateTime = new DateTime(x)

    new User(node.getProperty("username"),
      Some(node.getProperty("user_id").toString.toInt),
      node.getProperty("email"),
      node.getProperty("join_date"),
      node.getProperty("passhash"))
  }

  def from(app: UserAccountApplication): User = {
    if (EMAIL_REGEX.findFirstIn(app.email) == None) {
      throw new JeedurException(400, ErrorMessages.EMAIL_ADDRESS_INVALID)
    }
    if (app.password.length < 8) {
      throw new JeedurException(400, ErrorMessages.PASSWORD_NOT_LONG_ENOUGH)
    }
    val passhash = SCryptUtil.scrypt(app.password, 65536, 8, 1)
    val join_date = DateTime.now()
    val user_id = Counters.get("user_id")
    new User(app.username, Some(user_id), app.email, join_date, passhash)
  }

  def get(db: GraphDatabaseAPI, user_id: Int): User = {
    User.from(getNode(db, user_id))
  }

  def getNode(db: GraphDatabaseAPI, user_id: Int): Node = {
    val tx = db.beginTx()
    try {
      val node = db.index().forNodes("users").query("user_id:" + user_id).getSingle
      tx.success()
      node
    } finally {
      tx.finish()
    }
  }
}

class User(val username: String,
           val user_id: Option[Int],
           val email: String,
           val join_date: DateTime,
           val passhash: String) {
  def save(db: GraphDatabaseAPI) {
    val tx = db.beginTx()
    try {
      val node = db.createNode()
      node.setProperty("user_id", user_id.get)
      node.setProperty("username", username)
      node.setProperty("email", email)
      node.setProperty("join_date", join_date)
      node.setProperty("passhash", passhash)
      node.setProperty("type", "User")

      db.index().forNodes("users").add(node, "user_id", user_id.get)
      tx.success()
      node
    } finally {
      tx.finish()
    }
  }
}

case class UserAccountApplication(username: String, email: String, password: String)