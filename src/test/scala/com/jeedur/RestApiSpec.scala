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
        post("/v1/users/" + user_id + "/cards", """{"front":"Hello", "back":"你好", "tags":[]}""") {
          status should equal(403)
          val JString(message) = jsonResponse \ "message"
          message should equal(ErrorMessages.CARDS_MUST_HAVE_TAGS)
        }
      }
    }
  }

  "get /v1/users" should {
    "return error message with appropriate status code" in {
      get("/v1/users") {
        status should equal(403)
        val JString(message) = jsonResponse \ "message"
        message should equal(ErrorMessages.GET_ALL_USERS_FORBIDDEN)
      }
    }
  }

  "get /v1/users/:id" should {
    "return appropriate user account info" in {
      post("/v1/users", """{"username":"Yoda", "email":"yoda@theforce.co.uk", "password":"lollipop"}""") {
        val JInt(user_id) = jsonResponse \ "user_id"
        get("/v1/users/" + user_id) {
          status should equal(200)
          val user = jsonResponse.extract[User]
          user.user_id.get should equal(user_id)
          user.username should equal("Yoda")
          user.email should equal("yoda@theforce.co.uk")
        }
      }
    }
  }

  def multiplePosts[A](user_id: BigInt, bodies: List[String])(f: => A): A = {
    if (bodies.size > 1) {
      post("/v1/users/" + user_id + "/cards", bodies(0)) {
        status should equal(200)
        multiplePosts(user_id, bodies.tail)(f)
      }
    } else {
      post("/v1/users/" + user_id + "/cards", bodies(0)) {
        f
      }
    }
  }

  val cards = List( """{"front":"Hello", "back":"你好", "tags":["chinese", "lesson01"]}""",
    """{"front":"Goodbye", "back":"再见", "tags":["chinese", "lesson01"]}""",
    """{"front":"Thankyou", "back":"谢谢", "tags":["chinese", "lesson01"]}""",
    """{"front":"Weather", "back":"天气", "tags":["chinese", "lesson01"]}""",
    """{"front":"Friend", "back":"朋友", "tags":["chinese", "lesson01"]}""",
    """{"front":"Pig", "back":"猪", "tags":["chinese", "animals"]}""",
    """{"front":"Cat", "back":"猫", "tags":["chinese", "animals"]}""",
    """{"front":"Dog", "back":"狗", "tags":["chinese", "animals"]}""",
    """{"front":"Sheep", "back":"样", "tags":["chinese", "animals"]}""",
    """{"front":"Snake", "back":"小龙", "tags":["chinese", "animals"]}""",
    """{"front":"noodles", "back":"面", "tags":["chinese", "food"]}""")

  "get /v1/users/:id/cards" should {
    "return cards in an appropriate order" in {
      post("/v1/users", """{"username":"Yoda", "email":"yoda@theforce.co.uk", "password":"lollipop"}""") {
        val JInt(user_id) = jsonResponse \ "user_id"
        multiplePosts(user_id, cards) {
          status should equal(200)
          get("/v1/users/" + user_id + "/cards") {
            status should equal(200)
            val JArray(jsonCards) = jsonResponse
            val cards = jsonCards.map(jsonCard => jsonCard.extract[Card])
            cards.length should equal(10)
          }
          get("/v1/users/" + user_id + "/cards?limit=5") {
            status should equal(200)
            val JArray(jsonCards) = jsonResponse
            val cards = jsonCards.map(jsonCard => jsonCard.extract[Card])
            cards.length should equal(5)
          }
          get("/v1/users/" + user_id + "/cards?limit=5&offset=7") {
            status should equal(200)
            val JArray(jsonCards) = jsonResponse
            val cards = jsonCards.map(jsonCard => jsonCard.extract[Card])
            cards.length should equal(4)
          }
          get("/v1/users/" + user_id + "/cards?tags=animals") {
            status should equal(200)
            val JArray(jsonCards) = jsonResponse
            val cards = jsonCards.map(jsonCard => jsonCard.extract[Card])
            cards.length should equal(5)
            cards.foreach {
              card =>
                card.tags should contain("animals")
            }
          }
          get("/v1/users/" + user_id + "/cards?tags=animals,food") {
            status should equal(200)
            val JArray(jsonCards) = jsonResponse
            val cards = jsonCards.map(jsonCard => jsonCard.extract[Card])
            cards.length should equal(6)
            cards.foreach {
              card => card.tags.intersect(Set("animals", "food")) should not be ('empty)
            }
          }
          get("/v1/users/" + user_id + "/cards?tags=animals&offset=3") {
            status should equal(200)
            val JArray(jsonCards) = jsonResponse
            val cards = jsonCards.map(jsonCard => jsonCard.extract[Card])
            cards.length should equal(2)
          }
          get("/v1/users/" + user_id + "/cards?limit=pig") {
            status should equal(403)
            val JString(message) = jsonResponse \ "message"
            message should equal(ErrorMessages.QUERY_PARAMETERS_NOT_VALID)
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
