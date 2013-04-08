package spike.scalatra

import org.scalatra._
import scalate.ScalateSupport
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import org.neo4j.rest.graphdb.RestGraphDatabase
import scala.collection.JavaConversions._
import org.joda.time.DateTime
import com.lambdaworks.crypto.SCryptUtil
import net.liftweb.json.Serialization.write

case class User(username: String,
                user_id: Option[Int],
                email: String,
                join_date: DateTime,
                passhash: String
                 ) {
  def this(username: String, email: String, passhash: String) =
    this(username, None: Option[Int], email, DateTime.now(), passhash)
}

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

    val json = parse(request.body) transform {
      case JField("password", JString(x)) => JField("passhash", JString(SCryptUtil.scrypt(x, 65536, 8, 1))) // 2^14
      case JField("user_id", _) => JField("user_id", None: Option[Int])
    }
    val user = json.extract[User]

    val tx = db.beginTx()
    try {
      val node = db.createNode()

      node.setProperty("username", user.username)
      node.setProperty("email", user.email)
      node.setProperty("join_date", user.join_date)
      node.setProperty("passhash", user.passhash)
      node.setProperty("type", "User")

      val new_user_id = db.getNodeById(0).getProperty("user_id_counter", 5).asInstanceOf[Int]
      node.setProperty("user_id", new_user_id)
      db.getNodeById(0).setProperty("user_id_counter", new_user_id + 1)

      db.index().forNodes("users").add(node, "username", user.username)
      db.index().forNodes("users").add(node, "user_id", new_user_id)
      tx.success()

      write(user)
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
