package com.jeedur

import org.scalatra._
import scalate.ScalateSupport
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import org.neo4j.rest.graphdb.RestGraphDatabase
import com.lambdaworks.crypto.SCryptUtil
import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.graphdb.{Relationship, Node}
import JeedurRelationships._
import java.lang.Iterable
import collection.JavaConversions.asScalaIterator


class RestApiServlet extends ScalatraServlet with ScalateSupport with JsonHelpers {
  implicit def iterableToList(x: Iterable[Relationship]): List[Relationship] = x.iterator().toList

  def createUser(db: GraphDatabaseAPI, user: User): Node = {
    require(!user.user_id.isDefined)

    implicit def reflector(ref: AnyRef) = new {
      def getMember(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
    }

    val tx = db.beginTx()
    try {
      val userProperties = Set("username", "email", "join_date", "passhash")
      val generatedProperties = Map("type" -> "User", "user_id" -> Counters.get(db, "user_id"))
      println("generatedProperties: " + generatedProperties("user_id"))
      val indexedProperties = Set("username", "user_id", "join_date", "email")

      require(indexedProperties.forall {
        propName => userProperties.contains(propName) || generatedProperties.contains(propName)
      })
      require(userProperties.intersect(generatedProperties.keySet).isEmpty)

      val index = db.index().forNodes("users")
      val node = db.createNode()

      userProperties.foreach {
        propName => node.setProperty(propName, user.getMember(propName))
      }
      generatedProperties.foreach {
        case (propName, value) => println("Setting " + propName + " to " + value); node.setProperty(propName, value)
      }
      indexedProperties.foreach {
        propName => index.add(node, propName, node.getProperty(propName))
      }

      tx.success()
      node
    } finally {
      tx.finish()
    }
  }

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
    User.fromNode(getUserNode(db, user_id))
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
          User.fromNode(userNode).user_id.get == user_id
      }
      require(ownedByUser)

      val card = Card.fromNode(node)

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
      var cards = userNode.getRelationships(CREATED_CARD).map(rel => Card.fromNode(rel.getEndNode))

      tx.success()
      cards
    } finally {
      tx.finish()
    }
  }

  def createCard(db: GraphDatabaseAPI, card: Card, user_id: Int): Node = {
    require(!card.card_id.isDefined)

    implicit def reflector(ref: AnyRef) = new {
      def getMember(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
    }

    val tx = db.beginTx()
    try {
      val cardProperties = Set("front", "back", "create_date")
      val generatedProperties = Map("type" -> "Card", "card_id" -> Counters.get(db, "card_id"))
      val indexedProperties = Set("front", "back", "create_date", "card_id")

      require(indexedProperties.forall {
        propName => cardProperties.contains(propName) || generatedProperties.contains(propName)
      })
      require(cardProperties.intersect(generatedProperties.keySet).isEmpty)

      val index = db.index().forNodes("cards")
      val cardNode = db.createNode()

      cardProperties.foreach {
        propName => cardNode.setProperty(propName, card.getMember(propName))
      }
      generatedProperties.foreach {
        case (propName, value) => cardNode.setProperty(propName, value)
      }
      indexedProperties.foreach {
        propName => index.add(cardNode, propName, cardNode.getProperty(propName))
      }

      val userNode = getUserNode(db, user_id)
      userNode.createRelationshipTo(cardNode, CREATED_CARD)

      tx.success()
      cardNode
    } finally {
      tx.finish()
    }
  }


  before("/v1/*") {
    contentType = "application/json;charset=UTF-8"
  }


  get("/v1/users/:id") {
    val db = new RestGraphDatabase("http://localhost:7474/db/data")
    getUser(db, params("id").toInt).toString
  }

  get("/v1/users") {
    val db = new RestGraphDatabase("http://localhost:7474/db/data")
    getUser(db, 30)
  }

  post("/v1/users") {
    val db = new RestGraphDatabase("http://localhost:7474/db/data")

    val json = parse(request.body) transform {
      case JField("password", JString(x)) => JField("passhash", JString(SCryptUtil.scrypt(x, 65536, 8, 1))) // 2^14
      case JField("user_id", _) => JField("user_id", None: Option[Int])
    }

    val user = json.extract[User]
    val node = createUser(db, user)
    User.fromNode(node)
  }

  get("/error") {
    throw new RuntimeException("oh noez")
  }

  post("/v1/users/:id/cards") {
    val db = new RestGraphDatabase("http://localhost:7474/db/data")

    val json = parse(request.body) transform {
      case JField("card_id", _) => JField("card_id", None: Option[Int])
    }

    val card = json.extract[Card]

    val cardNode = createCard(db, card, params("id").toInt)

    Card.fromNode(cardNode)
  }

  get("/v1/users/:user_id/cards") {
    val db = new RestGraphDatabase("http://localhost:7474/db/data")

    val cards = getAllCards(db, params("user_id").toInt)

    cards
  }


  get("/v1/users/:user_id/cards/:card_id") {
    val db = new RestGraphDatabase("http://localhost:7474/db/data")
    val card_id = params("card_id").toInt
    val user_id = params("user_id").toInt
    val card = getCard(db, card_id, user_id)
    card.toString
  }

  error {
    case e: Exception =>
      // TODO: Use log instead of console
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
