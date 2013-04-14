package com.jeedur

import org.neo4j.graphdb.Node
import org.joda.time.DateTime
import org.neo4j.kernel.GraphDatabaseAPI
import net.liftweb.json.Serialization._
import scala.Some


object Card {
  implicit val formats = net.liftweb.json.DefaultFormats

  def from(node: Node) = {
    implicit def tos(x: AnyRef): String = x.toString
    implicit def toi(x: AnyRef): Option[Int] = Some(x.toString.toInt)
    implicit def todt(x: AnyRef): DateTime = new DateTime(x)


    new Card(node.getProperty("card_id"),
      node.getProperty("front"),
      node.getProperty("back"),
      read[Set[String]](node.getProperty("tags")),
      node.getProperty("create_date"))
  }

  def from(app: CardCreationApplication) = {
    val create_date = DateTime.now()
    val card_id = Some(Counters.get("card_id"))
    new Card(card_id, app.front, app.back, app.tags, create_date)
  }

  def save(db: GraphDatabaseAPI, card: Card): Node = {
    val tx = db.beginTx()
    try {
      val node = db.createNode()
      node.setProperty("card_id", card.card_id.get)
      node.setProperty("front", card.front)
      node.setProperty("back", card.back)
      node.setProperty("tags", write(card.tags))
      node.setProperty("create_date", card.create_date)

      db.index().forNodes("cards").add(node, "card_id", card.card_id.get)
      tx.success()
      node
    } finally {
      tx.finish()
    }
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

case class Card(card_id: Option[Int], front: String, back: String, tags: Set[String], create_date: DateTime)

case class CardCreationApplication(front: String, back: String, tags: Set[String])