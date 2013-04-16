package com.jeedur

import org.neo4j.graphdb.Node
import org.joda.time.DateTime
import org.neo4j.kernel.GraphDatabaseAPI
import net.liftweb.json.Serialization._
import com.jeedur.JeedurRelationships._
import scala.Some
import net.liftweb.json.DefaultFormats

object Card {
  def from(node: Node): Card = {
    implicit val formats = DefaultFormats
    implicit def tos(x: AnyRef): String = x.toString
    implicit def toi(x: AnyRef): Option[Int] = Some(x.toString.toInt)
    implicit def todt(x: AnyRef): DateTime = new DateTime(x)

    new Card(node.getProperty("card_id"),
      node.getProperty("front"),
      node.getProperty("back"),
      read[Set[String]](node.getProperty("tags")),
      node.getProperty("create_date"))
  }

  def from(app: CardCreationApplication): Card = {
    val create_date = DateTime.now()
    val card_id = Some(Counters.get("card_id"))
    new Card(card_id, app.front, app.back, app.tags, create_date)
  }

  def getNode(db: GraphDatabaseAPI, card_id: Int): Node = {
    val tx = db.beginTx()
    try {
      val node = db.index().forNodes("cards").query("card_id:" + card_id).getSingle
      tx.success()
      node
    } finally {
      tx.finish()
    }
  }
}

class Card(val card_id: Option[Int],
           val front: String,
           val back: String,
           val tags: Set[String],
           val create_date: DateTime) {

  def setCreatedBy(db: GraphDatabaseAPI, user: User) {
    val tx = db.beginTx()
    try {
      val userNode = User.getNode(db, user.user_id.get)
      val cardNode = Card.getNode(db, card_id.get)
      userNode.createRelationshipTo(cardNode, CREATED_CARD)
      tx.success()
    } finally {
      tx.finish()
    }
  }

  def save(db: GraphDatabaseAPI) {
    implicit val formats = DefaultFormats
    val tx = db.beginTx()
    try {
      val node = db.createNode()
      node.setProperty("card_id", card_id.get)
      node.setProperty("front", front)
      node.setProperty("back", back)
      node.setProperty("tags", write(tags))
      node.setProperty("create_date", create_date)

      db.index().forNodes("cards").add(node, "card_id", card_id.get)
      tx.success()
      node
    } finally {
      tx.finish()
    }
  }
}

case class CardCreationApplication(front: String, back: String, tags: Set[String])