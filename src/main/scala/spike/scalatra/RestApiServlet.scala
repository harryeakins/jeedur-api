package spike.scalatra

import org.scalatra._
import scalate.ScalateSupport
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import org.neo4j.rest.graphdb.RestGraphDatabase
import org.joda.time.DateTime
import com.lambdaworks.crypto.SCryptUtil
import net.liftweb.json.Serialization.write
import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.graphdb.Node

class User(val username: String,
           val user_id: Option[Int],
           val email: String,
           val join_date: DateTime,
           val passhash: String) {
  def this(username: String, email: String, passhash: String) =
    this(username, None: Option[Int], email, DateTime.now(), passhash)

  override def toString = {
    "User[%d, %s, %s]".format(user_id.get, username, email)
  }
}

class RestApiServlet extends ScalatraServlet with ScalateSupport with JsonHelpers {

  def getNextUserId(db: GraphDatabaseAPI): Int = {
    val new_user_id = db.getNodeById(0).getProperty("user_id_counter", 5).asInstanceOf[Int]
    db.getNodeById(0).setProperty("user_id_counter", new_user_id + 1)
    new_user_id
  }

  def createUser(db: GraphDatabaseAPI, user: User): Node = {
    require(!user.user_id.isDefined)

    implicit def reflector(ref: AnyRef) = new {
      def getMember(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
    }

    val tx = db.beginTx()
    try {
      val userProperties = Set("username", "email", "join_date", "passhash")
      val generatedProperties = Map("type" -> "User", "user_id" -> getNextUserId(db))
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
        case (propName, value) => node.setProperty(propName, value)
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

  def getUser(db: GraphDatabaseAPI, user_id: Int): User = {
    implicit def reflector(ref: AnyRef) = new {
      def setV(name: String, value: Any): Unit = ref.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(ref, value.asInstanceOf[AnyRef])
    }

    val tx = db.beginTx()
    try {
      val index = db.index().forNodes("users")
      val node = index.query("user_id", user_id).getSingle

      implicit def tos(x: AnyRef): String = x.toString
      implicit def toi(x: AnyRef): Option[Int] = Some(x.toString.toInt)
      implicit def todt(x: AnyRef): DateTime = new DateTime(x)

      val user = new User(node.getProperty("username"),
        node.getProperty("user_id"),
        node.getProperty("email"),
        node.getProperty("join_date"),
        node.getProperty("passhash"))

      tx.success()
      user
    } finally {
      tx.finish()
    }
  }

  before("/v1/*") {
    contentType = "application/json;charset=UTF-8"
  }


  get("/v1/users/:id") {
    val db = new RestGraphDatabase("http://localhost:7474/db/data")
    write(getUser(db, params("id").toInt))
  }

  get("/v1/users") {
    val db = new RestGraphDatabase("http://localhost:7474/db/data")
    write(getUser(db, 30))
  }

  post("/v1/users") {
    val db = new RestGraphDatabase("http://localhost:7474/db/data");

    val json = parse(request.body) transform {
      case JField("password", JString(x)) => JField("passhash", JString(SCryptUtil.scrypt(x, 65536, 8, 1))) // 2^14
      case JField("user_id", _) => JField("user_id", None: Option[Int])
    }

    val user = json.extract[User]

    createUser(db, user)

    write(user)
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
