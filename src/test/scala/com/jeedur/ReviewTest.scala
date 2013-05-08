package com.jeedur

import net.liftweb.json
import json.JsonAST.JInt
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatest.BeforeAndAfterEach

class ReviewTest extends ScalatraFunSuite with JsonHelpers with BeforeAndAfterEach {
  addServlet(new RestApiServlet, "/*")

  var user_id: BigInt = _
  var card_id: BigInt = _

  override def beforeEach() {
    post("/v1/users", """{"username":"Yoda", "email":"yoda@theforce.co.uk", "password":"lollipop"}""") {
      val JInt(id) = jsonResponse \ "user_id"
      user_id = id
      post("/v1/users/" + user_id + "/cards", """{"front":"Hello", "back":"你好", "tags":["chinese", "lesson01"]}""") {
        status should equal(200)
        val card = jsonResponse.extract[Card]
        card.front should equal("Hello")
        card.back should equal("你好")
        card.tags should contain("chinese")
        card.tags should contain("lesson01")
        card_id = card.card_id.get
      }
    }
  }

  test("Can post reviews to card") {
    post("/v1/users/" + user_id + "/cards/" + card_id + "/reviews", """{"timeOnFront": 12, "timeOnBack": 32, "difficulty": "EASY"}""") {
      status should equal(200)
      val review = jsonResponse.extract[Review]
      review.difficulty should equal(Difficulty.EASY)
      review.timeOnFront should equal(12.0)
      review.timeOnBack should equal(32.0)
    }
  }

  test("Can post reviews with parameters in any order") {
    post("/v1/users/" + user_id + "/cards/" + card_id + "/reviews", """{"difficulty": "EASY", "timeOnBack": 32, "timeOnFront": 12}""") {
      status should equal(200)
      val review = jsonResponse.extract[Review]
      review.difficulty should equal(Difficulty.EASY)
      review.timeOnFront should equal(12.0)
      review.timeOnBack should equal(32.0)
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
