package com.jeedur

import org.neo4j.graphdb.{Node, Relationship}
import org.neo4j.graphdb.Direction._
import org.joda.time.DateTime
import com.lambdaworks.crypto.SCryptUtil
import org.neo4j.kernel.GraphDatabaseAPI
import JeedurRelationships._
import collection.JavaConversions.asScalaIterator
import java.lang.Iterable
import net.liftweb.json.Serialization._
import net.liftweb.json._
import scala.Some
import net.liftweb.json.MappingException
import JeedurUtils._


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
    withinDbTransaction(db) {
      db.index().forNodes("users").query("user_id:" + user_id).getSingle
    }
  }
}

class User(val username: String,
           val user_id: Option[Int],
           val email: String,
           val join_date: DateTime,
           val passhash: String) extends JsonHelpers {
  def save(db: GraphDatabaseAPI) {
    withinDbTransaction(db) {
      val node = db.createNode()
      node.setProperty("user_id", user_id.get)
      node.setProperty("username", username)
      node.setProperty("email", email)
      node.setProperty("join_date", join_date)
      node.setProperty("passhash", passhash)
      node.setProperty("type", "User")

      db.index().forNodes("users").add(node, "user_id", user_id.get)
      node
    }
  }

  def getNode(db: GraphDatabaseAPI): Node = {
    withinDbTransaction(db) {
      db.index().forNodes("users").query("user_id", user_id.get).getSingle
    }
  }

  def addStudiesRelationship(db: GraphDatabaseAPI, card: Card): Relationship = {
    withinDbTransaction(db) {
      val rel = getNode(db).createRelationshipTo(card.getNode(db), STUDIES)
      rel.setProperty("review_history", "[]")
      rel.setProperty("active", true)
      rel.setProperty("create_date", DateTime.now())
      rel
    }
  }

  def getStudiesRelationship(db: GraphDatabaseAPI, card: Card): Option[Relationship] = {
    implicit def iterableToList(x: Iterable[Relationship]): List[Relationship] = x.iterator().toList

    withinDbTransaction(db) {
      val userNode = getNode(db)
      val card_id = card.card_id.getOrElse(-1)
      val rels = userNode.getRelationships(OUTGOING, STUDIES).filter {
        _.getEndNode.getProperty("card_id") == card_id
      }
      require(rels.length == 1 || rels.length == 0)
      if (rels.length > 0) Some(rels(0)) else None
    }
  }

  def recordReview(db: GraphDatabaseAPI, card: Card, review: Review) {
    val rel = getStudiesRelationship(db, card).get
    val reviewHistoryJson = rel.getProperty("review_history").toString
    val reviewHistory = parse(reviewHistoryJson).extract[List[Review]]
    rel.setProperty("review_history", write(review :: reviewHistory))
  }

  def getAllCardsWithHistory(db: GraphDatabaseAPI): List[(Card, List[Review])] = {
    implicit def iterableToList[A](x: Iterable[A]): List[A] = x.iterator.toList
    withinDbTransaction(db) {
      val userNode = getNode(db)
      userNode.getRelationships(STUDIES, OUTGOING).map {
        rel => (Card.from(rel.getEndNode),
          parse(rel.getProperty("review_history").toString).extract[List[Review]])
      }
    }
  }
}

object UserAccountApplication extends JsonHelpers {
  def unapply(s: String): Option[UserAccountApplication] = {
    try {
      Some(parse(s).extract[UserAccountApplication])
    } catch {
      case e: MappingException => throw new JeedurException(400, ErrorMessages.REQUIRED_FIELD_NOT_PRESENT)
    }
  }
}

class UserAccountApplication(val username: String,
                             val email: String,
                             val password: String)