package com.jeedur

import org.neo4j.graphdb.Node
import org.joda.time.DateTime
import com.lambdaworks.crypto.SCryptUtil
import org.neo4j.kernel.GraphDatabaseAPI

object User {
  val EMAIL_REGEX = """\b[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\b""".r

  def from(node: Node) = {
    implicit def tos(x: AnyRef): String = x.toString
    implicit def todt(x: AnyRef): DateTime = new DateTime(x)

    new User(node.getProperty("username"),
      Some(node.getProperty("user_id").toString.toInt),
      node.getProperty("email"),
      node.getProperty("join_date"),
      node.getProperty("passhash"))
  }

  def from(app: UserAccountApplication) = {
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

  def save(db: GraphDatabaseAPI, user: User) = {
    val tx = db.beginTx()
    try {
      val node = db.createNode()
      node.setProperty("user_id", user.user_id.get)
      node.setProperty("username", user.username)
      node.setProperty("email", user.email)
      node.setProperty("join_date", user.join_date)
      node.setProperty("passhash", user.passhash)
      node.setProperty("type", "User")

      db.index().forNodes("users").add(node, "user_id", user.user_id.get)
      tx.success()
      node
    } finally {
      tx.finish()
    }
  }

  def get(db: GraphDatabaseAPI, user_id: Int) = {
    User.from(getNode(db, user_id))
  }

  def getNode(db: GraphDatabaseAPI, user_id: Int) = {
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

case class User(username: String, user_id: Option[Int], email: String, join_date: DateTime, passhash: String)

case class UserAccountApplication(username: String, email: String, password: String)