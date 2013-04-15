package com.jeedur

import org.scalatra.test.scalatest._
import org.scalatest.WordSpec
import net.liftweb.json
import net.liftweb.json._

class RestApiSpec extends ScalatraSuite with WordSpec with JsonHelpers {
  addServlet(new RestApiServlet, "/*")

  "post users" should {
    "return newly created user info" in {
      post("/v1/users", """{"username":"Yoda", "email":"yoda@theforce.co.uk", "password":"lollipop"}""") {
        status should equal(200)
        val user = jsonResponse.extract[User]
        user.username should equal("Yoda")
        user.email should equal("yoda@theforce.co.uk")
        user.passhash.length should equal(80)
      }
    }
    "not allow invalid email addresses" in {
      post("/v1/users", """{"username":"Yoda", "email":"yoda", "password":"lollipop"}""") {
        status should equal(400)
        val JString(message) = jsonResponse \ "message"
        message should equal(ErrorMessages.EMAIL_ADDRESS_INVALID)
      }
    }
    "not allow short passwords" in {
      post("/v1/users", """{"username":"Yoda", "email":"yoda@theforce.co.uk", "password":"hello"}""") {
        status should equal(400)
        val JString(message) = jsonResponse \ "message"
        message should equal(ErrorMessages.PASSWORD_NOT_LONG_ENOUGH)
      }
    }
    "error if required fields not present" in {
      post("/v1/users", """{"username":"Yoda", "password":"lollipop"}""") {
        status should equal(400)
        val JString(message) = jsonResponse \ "message"
        message should equal(ErrorMessages.REQUIRED_FIELD_NOT_PRESENT)
      }
    }
  }

  "post cards" should {
    "return newly created card info" in {
      post("/v1/users", """{"username":"Yoda", "email":"yoda@theforce.co.uk", "password":"lollipop"}""") {
        val JInt(user_id) = jsonResponse \ "user_id"
        post("/v1/users/" + user_id + "/cards", """{"front":"Hello", "back":"你好", "tags":["chinese", "lesson01"]}""") {
          status should equal(200)
          val card = jsonResponse.extract[Card]
          card.front should equal("Hello")
          card.back should equal("你好")
          card.tags should contain("chinese")
          card.tags should contain("lesson01")
          get("/v1/users/" + user_id + "/cards") {
            status should equal(200)
            val JArray(jsonCards) = jsonResponse
            val cards = jsonCards.map(jsonCard => jsonCard.extract[Card])
            cards.length should equal(1)
            cards(0).front should equal("Hello")
            cards(0).back should equal("你好")

            val tags = cards(0).tags
            tags.size should equal(2)
            tags should contain("chinese")
            tags should contain("lesson01")
          }
          get("/v1/users/" + user_id + "/cards/" + card.card_id.get) {
            status should equal(200)
            val card = jsonResponse.extract[Card]
            card.front should equal("Hello")
            card.back should equal("你好")
            val tags = card.tags
            tags.size should equal(2)
            tags should contain("chinese")
            tags should contain("lesson01")
          }
        }
      }
    }
  }

  "get /error" should {
    "return general message with appropriate status code" in {
      get("/error") {
        status should equal(500)
        body should equal( """{"message":"Internal error."}""")
      }
    }
  }

  def jsonResponse = json.parse(response.getContent)
}
