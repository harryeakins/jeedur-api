package spike.scalatra

import org.scalatra._
import scalate.ScalateSupport
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import org.neo4j.rest.graphdb.RestGraphDatabase
import scala.collection.JavaConversions._

case class User(name: String)

class RestApiServlet extends ScalatraServlet with ScalateSupport with JsonHelpers {
  before("/v1/*") {
    contentType = "application/json;charset=UTF-8"
  }

  get("/v1/users/:id") {
    val db = new RestGraphDatabase("http://localhost:7474/db/data")
    val query = db.index().forNodes("users").query("user_id", params("id"))
    query.getSingle.getProperty("name")
  }

  get("/v1/users") {
    val db = new RestGraphDatabase("http://localhost:7474/db/data")
    val query = db.index.forNodes("users").query("user_id", "*")
    query.iterator.toList.map(_.getProperty("name"))
  }

  post("/v1/users") {
    val db = new RestGraphDatabase("http://localhost:7474/db/data");
    val user = parse(request.body).extract[User]
    val tx = db.beginTx()
    try {
      val new_user_id = db.getNodeById(0).getProperty("user_id_counter", 5).asInstanceOf[Int]
      val node = db.createNode()
      node.setProperty("name", user.name)
      node.setProperty("type", "User")
      node.setProperty("user_id", new_user_id)
      db.index().forNodes("users").add(node, "name", user.name)
      db.index().forNodes("users").add(node, "user_id", new_user_id)
      db.getNodeById(0).setProperty("user_id_counter", new_user_id + 1)
      tx.success()
      "Success!"
    } finally {
      tx.finish()
    }
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
