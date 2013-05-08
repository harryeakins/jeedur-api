package com.jeedur

import net.liftweb.json
import json.JsonAST.{JArray, JInt, JString}
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatest.BeforeAndAfterEach

class CardTest extends ScalatraFunSuite with JsonHelpers with BeforeAndAfterEach {
  addServlet(new RestApiServlet, "/*")

  var user_id: BigInt = _

  override def beforeEach() {
    post("/v1/users", """{"username":"Yoda", "email":"yoda@theforce.co.uk", "password":"lollipop"}""") {
      val JInt(id) = jsonResponse \ "user_id"
      user_id = id
    }
  }

  test("Can create card") {
    post("/v1/users/" + user_id + "/cards", """{"front":"Hello", "back":"你好", "tags":["chinese", "lesson01"]}""") {
      status should equal(200)
      val card = jsonResponse.extract[Card]
      card.front should equal("Hello")
      card.back should equal("你好")
      card.tags should contain("chinese")
      card.tags should contain("lesson01")
    }
  }

  test("Can retrieve card and card list") {
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

  test("Cannot create a card with no tags") {
    post("/v1/users/" + user_id + "/cards", """{"front":"Hello", "back":"你好", "tags":[]}""") {
      status should equal(403)
      val JString(message) = jsonResponse \ "message"
      message should equal(ErrorMessages.CARDS_MUST_HAVE_TAGS)
    }
  }

  test("Can get list of 10 cards by default") {
    multiplePosts(TestCardSets.chineseCards)
    get("/v1/users/" + user_id + "/cards") {
      status should equal(200)
      val JArray(jsonCards) = jsonResponse
      val cards = jsonCards.map(jsonCard => jsonCard.extract[Card])
      cards.length should equal(10)
    }
  }

  test("Can get list of 5 cards using limit") {
    multiplePosts(TestCardSets.chineseCards)
    get("/v1/users/" + user_id + "/cards?limit=5") {
      status should equal(200)
      val JArray(jsonCards) = jsonResponse
      val cards = jsonCards.map(jsonCard => jsonCard.extract[Card])
      cards.length should equal(5)
    }
  }

  test("Can get list of last 4 cards using offset") {
    multiplePosts(TestCardSets.chineseCards)
    get("/v1/users/" + user_id + "/cards?offset=7") {
      status should equal(200)
      val JArray(jsonCards) = jsonResponse
      val cards = jsonCards.map(jsonCard => jsonCard.extract[Card])
      cards.length should equal(4)
    }
  }

  test("Can get list of animal cards using tags filter") {
    multiplePosts(TestCardSets.chineseCards)
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
  }

  test("Can get list of cards containing animals or food tags using tags filter") {
    multiplePosts(TestCardSets.chineseCards)
    get("/v1/users/" + user_id + "/cards?tags=animals,food") {
      status should equal(200)
      val JArray(jsonCards) = jsonResponse
      val cards = jsonCards.map(jsonCard => jsonCard.extract[Card])
      cards.length should equal(6)
      cards.foreach {
        card => card.tags.intersect(Set("animals", "food")) should not be ('empty)
      }
    }
  }

  test("Can get list of last two animals cards using tags filter and offset") {
    multiplePosts(TestCardSets.chineseCards)
    get("/v1/users/" + user_id + "/cards?tags=animals&offset=3") {
      status should equal(200)
      val JArray(jsonCards) = jsonResponse
      val cards = jsonCards.map(jsonCard => jsonCard.extract[Card])
      cards should have size 2
    }
  }

  test("Returns an error if limit parameter wrongly specified") {
    multiplePosts(TestCardSets.chineseCards)
    get("/v1/users/" + user_id + "/cards?limit=pig") {
      status should equal(400)
      val JString(message) = jsonResponse \ "message"
      message should equal(ErrorMessages.QUERY_PARAMETERS_NOT_VALID)
    }
  }

  def multiplePosts(bodies: List[String]) {
    bodies.foreach {
      jsonCard =>
        post("/v1/users/" + user_id + "/cards", jsonCard) {
          status should equal(200)
        }
    }
  }

  def jsonResponse = json.parse(response.getContent)
}
