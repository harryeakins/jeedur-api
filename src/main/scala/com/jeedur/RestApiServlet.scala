package com.jeedur

import org.scalatra._
import scalate.ScalateSupport
import net.liftweb.json._
import net.liftweb.json.Serialization.write
import net.liftweb.json.JsonDSL._
import org.neo4j.rest.graphdb.RestGraphDatabase
import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.graphdb.{Relationship, Node}
import JeedurRelationships._
import java.lang.Iterable
import collection.JavaConversions.asScalaIterator

object RestApiServlet {
  val neo4jURI = "http://localhost:7474/db/data"
}

class RestApiServlet extends ScalatraServlet with ScalateSupport with JsonHelpers {

  import RestApiServlet.neo4jURI

  implicit def iterableToList(x: Iterable[Relationship]): List[Relationship] = x.iterator().toList

  def getUserNode(db: GraphDatabaseAPI, user_id: Int): Node = {
    val tx = db.beginTx()
    try {
      val index = db.index().forNodes("users")
      val node = index.query("user_id", user_id).getSingle

      tx.success()
      node
    } finally {
      tx.finish()
    }
  }

  def getUser(db: GraphDatabaseAPI, user_id: Int): User = {
    User.from(getUserNode(db, user_id))
  }

  def getCard(db: GraphDatabaseAPI, card_id: Int, user_id: Int): Card = {
    val tx = db.beginTx()
    try {
      val index = db.index().forNodes("cards")
      val node = index.query("card_id", card_id).getSingle
      val relationships = node.getRelationships
      val ownedByUser = relationships.exists {
        x =>
          val userNode = x.getOtherNode(node)
          User.from(userNode).user_id.get == user_id
      }
      require(ownedByUser)

      val card = Card.from(node)

      tx.success()
      card
    } finally {
      tx.finish()
    }
  }

  def getAllCards(db: GraphDatabaseAPI, user_id: Int): List[Card] = {
    val tx = db.beginTx()
    try {
      val userNode = getUserNode(db, user_id)
      var cards = userNode.getRelationships(CREATED_CARD).map(rel => Card.from(rel.getEndNode))

      tx.success()
      cards
    } finally {
      tx.finish()
    }
  }

  before("/v1/*") {
    contentType = "application/json;charset=UTF-8"
  }


  get("/v1/users/:id") {
    val db = new RestGraphDatabase(neo4jURI)
    getUser(db, params("id").toInt).toString
  }

  get("/v1/users") {
    val db = new RestGraphDatabase(neo4jURI)
    getUser(db, 30)
  }

  post("/v1/users") {
    val db = new RestGraphDatabase(neo4jURI)
    val app =
      try {
        parse(request.body).extract[UserAccountApplication]
      } catch {
        case e: MappingException => throw new JeedurException(400, ErrorMessages.REQUIRED_FIELD_NOT_PRESENT)
      }
    val user = User.from(app)
    User.save(db, user)
    write(user)
  }

  get("/error") {
    throw new RuntimeException("oh noez")
  }

  post("/v1/users/:id/cards") {
    val db = new RestGraphDatabase(neo4jURI)
    val app = parse(request.body).extract[CardCreationApplication]
    val card = Card.from(app)
    card.save(db)

    val user_id = params("id").toInt
    val user = User.get(db, user_id)
    card.setCreatedBy(db, user)
    write(card)
  }

  get("/v1/users/:user_id/cards") {
    val db = new RestGraphDatabase(neo4jURI)
    val cards = getAllCards(db, params("user_id").toInt)
    write(cards)
  }


  get("/v1/users/:user_id/cards/:card_id") {
    val db = new RestGraphDatabase(neo4jURI)
    val card_id = params("card_id").toInt
    val user_id = params("user_id").toInt
    val card = getCard(db, card_id, user_id)

    write(card)
  }

  error {
    case e: JeedurException =>
      status(e.status_code)
      contentType = "application/json;charset=UTF-8"
      compact(render(("message" -> e.message)))
    case e: Exception =>
      println("Unexpected error during http api call.", e)
      status(500)
      contentType = "application/json;charset=UTF-8"
      compact(render(("message" -> "Internal error.")))
  }

  notFound {
    contentType = "application/json;charset=UTF-8"
    status(404)
    compact(render(("message" -> "Not found.")))
  }
}
