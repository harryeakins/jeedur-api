package com.jeedur

import org.neo4j.graphdb.{Relationship, Node}
import org.joda.time.DateTime
import org.neo4j.kernel.GraphDatabaseAPI
import net.liftweb.json.Serialization._
import com.jeedur.JeedurRelationships._
import net.liftweb.json._
import scala.Some
import java.lang.Iterable
import collection.JavaConversions.asScalaIterator
import JeedurUtils._

object Card {
  implicit def iterableToList(x: Iterable[Relationship]): List[Relationship] = x.iterator().toList

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
    withinDbTransaction(db) {
      db.index().forNodes("cards").query("card_id:" + card_id).getSingle
    }
  }

  def getCard(db: GraphDatabaseAPI, card_id: Int, user_id: Int): Card = {
    withinDbTransaction(db) {
      val cardNode = Card.getNode(db, card_id)
      val relationships = cardNode.getRelationships
      val ownedByUser = relationships.exists {
        x =>
          val userNode = x.getOtherNode(cardNode)
          User.from(userNode).user_id.get == user_id
      }
      require(ownedByUser)

      Card.from(cardNode)
    }
  }

  def getAllFromUser(db: GraphDatabaseAPI, user_id: Int): List[Card] = {
    withinDbTransaction(db) {
      val userNode = User.getNode(db, user_id)
      userNode.getRelationships(CREATED_CARD).map(rel => Card.from(rel.getEndNode))
    }
  }
}

class Card(val card_id: Option[Int],
           val front: String,
           val back: String,
           val tags: Set[String],
           val create_date: DateTime) extends JsonHelpers {
  if (tags.size == 0) throw new JeedurException(403, ErrorMessages.CARDS_MUST_HAVE_TAGS)

  def setCreatedBy(db: GraphDatabaseAPI, user: User) {
    withinDbTransaction(db) {
      val userNode = User.getNode(db, user.user_id.get)
      val cardNode = Card.getNode(db, card_id.get)
      userNode.createRelationshipTo(cardNode, CREATED_CARD)
    }
  }

  def save(db: GraphDatabaseAPI) {
    implicit val formats = DefaultFormats
    withinDbTransaction(db) {
      val node = db.createNode()
      node.setProperty("card_id", card_id.get)
      node.setProperty("front", front)
      node.setProperty("back", back)
      node.setProperty("tags", write(tags))
      node.setProperty("create_date", create_date)

      db.index().forNodes("cards").add(node, "card_id", card_id.get)
      node
    }
  }

  def getNode(db: GraphDatabaseAPI): Node = {
    withinDbTransaction(db) {
      db.index().forNodes("cards").query("card_id", card_id.get).getSingle
    }
  }

  override def toString: String = {
    "Card(" + write(this) + ")"
  }
}

object CardCreationApplication {
  def unapply(s: String): Option[CardCreationApplication] = {
    implicit val formats = DefaultFormats
    try {
      Some(parse(s).extract[CardCreationApplication])
    } catch {
      case e: MappingException => throw new JeedurException(400, ErrorMessages.REQUIRED_FIELD_NOT_PRESENT)
    }
  }
}

class CardCreationApplication(val front: String,
                              val back: String,
                              val tags: Set[String])