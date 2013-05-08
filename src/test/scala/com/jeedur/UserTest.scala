package com.jeedur

import net.liftweb.json
import json.JsonAST.JString
import org.scalatra.test.scalatest.ScalatraFunSuite

class UserTest extends ScalatraFunSuite with JsonHelpers {
  addServlet(new RestApiServlet, "/*")

  test("Can create a new user") {
    post("/v1/users", """{"username":"Yoda", "email":"yoda@theforce.co.uk", "password":"lollipop"}""") {
      status should equal(200)
      val user = jsonResponse.extract[User]
      user.username should equal("Yoda")
      user.email should equal("yoda@theforce.co.uk")
      user.passhash.length should equal(80)
    }
  }

  test("Can not list all users") {
    get("/v1/users") {
      status should equal(403)
      val JString(message) = jsonResponse \ "message"
      message should equal(ErrorMessages.GET_ALL_USERS_FORBIDDEN)
    }
  }

  test("invalid email addresses not allowed") {
    post("/v1/users", """{"username":"Yoda", "email":"yoda", "password":"lollipop"}""") {
      status should equal(400)
      val JString(message) = jsonResponse \ "message"
      message should equal(ErrorMessages.EMAIL_ADDRESS_INVALID)
    }
  }

  test("short passwords not allowed") {
    post("/v1/users", """{"username":"Yoda", "email":"yoda@theforce.co.uk", "password":"hello"}""") {
      status should equal(400)
      val JString(message) = jsonResponse \ "message"
      message should equal(ErrorMessages.PASSWORD_NOT_LONG_ENOUGH)
    }
  }

  test("email is a required field") {
    post("/v1/users", """{"username":"Yoda", "password":"lollipop"}""") {
      status should equal(400)
      val JString(message) = jsonResponse \ "message"
      message should equal(ErrorMessages.REQUIRED_FIELD_NOT_PRESENT)
    }
  }

  def jsonResponse = json.parse(response.getContent)
}
