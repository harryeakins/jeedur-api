package spike.scalatra

import org.neo4j.graphdb.Node
import org.joda.time.DateTime

object Card {
  def fromNode(node: Node) = {
    implicit def tos(x: AnyRef): String = x.toString
    implicit def toi(x: AnyRef): Option[Int] = Some(x.toString.toInt)
    implicit def todt(x: AnyRef): DateTime = new DateTime(x)

    new Card(node.getProperty("card_id"),
      node.getProperty("front"),
      node.getProperty("back"),
      node.getProperty("create_date"))
  }
}

class Card(val card_id: Option[Int],
           val front: String,
           val back: String,
           val create_date: DateTime) {

  def this(card_id: Option[Int], front: String, back: String) =
    this(card_id, front, back, DateTime.now())

  override def toString = {

    "{\"card_id\":\"%s\", \"front\":\"%s\",\"back\":\"%s\",\"create_date\":\"%s\"}"
      .format(card_id.get, front, back, create_date)
  }
}