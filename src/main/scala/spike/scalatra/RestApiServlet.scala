package spike.scalatra

import org.scalatra._
import scalate.ScalateSupport
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

case class User(name: String, age: Int)

class RestApiServlet extends ScalatraServlet with ScalateSupport with JsonHelpers {
  before("/v1/*") {
    contentType = "application/json;charset=UTF-8"
  }

  get("/v1/users/:id") {
    params("id") match {
      case "1" => Json(User("John", 30))
      case "2" => Json(User("Harry", 22))
    }
  }

  post("/v1/users") {
    val user = parse(request.body).extract[User]
    println(Json(user))
    1
  }

  get("/error") {
    throw new RuntimeException("oh noez")
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
