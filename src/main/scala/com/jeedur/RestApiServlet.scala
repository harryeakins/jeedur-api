package com.jeedur

import org.scalatra._
import scalate.ScalateSupport
import net.liftweb.json._
import net.liftweb.json.Serialization.write
import net.liftweb.json.JsonDSL._
import org.neo4j.rest.graphdb.RestGraphDatabase

object RestApiServlet {
  val neo4jURI = "http://localhost:7474/db/data"
}

class RestApiServlet extends ScalatraServlet with ScalateSupport with JsonHelpers {

  import RestApiServlet.neo4jURI
  import JeedurUtils._

  before("/v1/*") {
    contentType = "application/json;charset=UTF-8"
  }

  get("/v1/users/:id") {
    val db = new RestGraphDatabase(neo4jURI)
    write(User.get(db, params("id").toInt))
  }

  get("/v1/users") {
    throw new JeedurException(403, ErrorMessages.GET_ALL_USERS_FORBIDDEN)
  }

  post("/v1/users") {
    val db = new RestGraphDatabase(neo4jURI)
    val UserAccountApplication(app) = request.body
    val user = User.from(app)
    user.save(db)
    write(user)
  }

  post("/v1/users/:id/cards") {
    val db = new RestGraphDatabase(neo4jURI)
    val CardCreationApplication(app) = request.body
    val card = Card.from(app)
    card.save(db)

    val user_id = params("id").toInt
    val user = User.get(db, user_id)
    card.setCreatedBy(db, user)
    user.addStudiesRelationship(db, card)
    write(card)
  }

  get("/v1/users/:user_id/cards") {
    val db = new RestGraphDatabase(neo4jURI)
    val user = User.get(db, params("user_id").toInt)
    val cards = Card.getAllFromUser(db, user.user_id.get)
    val limit =
      try {
        getParameter(request, "limit").getOrElse("10").toInt
      } catch {
        case e: NumberFormatException => throw new JeedurException(400, ErrorMessages.QUERY_PARAMETERS_NOT_VALID)
      }

    val offset =
      try {
        getParameter(request, "offset").getOrElse("0").toInt
      } catch {
        case e: NumberFormatException => throw new JeedurException(403, ErrorMessages.QUERY_PARAMETERS_NOT_VALID)
      }

    val tags = getParameter(request, "tags").getOrElse("")

    val filtered_cards =
      if (tags.equals("")) cards
      else cards.filter {
        x => x.tags.exists {
          s => tags.split(",").contains(s)
        }
      }

    write(filtered_cards.slice(offset, offset + limit))
  }


  get("/v1/users/:user_id/cards/:card_id") {
    val db = new RestGraphDatabase(neo4jURI)
    val card_id = params("card_id").toInt
    val user_id = params("user_id").toInt
    val card = Card.getCard(db, card_id, user_id)

    write(card)
  }

  post("/v1/users/:user_id/cards/:card_id/reviews") {
    val db = new RestGraphDatabase(neo4jURI)
    val card_id = params("card_id").toInt
    val user_id = params("user_id").toInt
    val card = Card.getCard(db, card_id, user_id)
    val user = User.get(db, user_id)
    val review = parse(request.body).extract[Review]
    user.recordReview(db, card, review)
    write(review)
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

  get("/error") {
    throw new RuntimeException("oh noez")
  }

  notFound {
    contentType = "application/json;charset=UTF-8"
    status(404)
    compact(render(("message" -> "Not found.")))
  }
}
