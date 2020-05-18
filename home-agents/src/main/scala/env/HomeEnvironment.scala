package env

import java.util
import java.util.logging.{Level, Logger}
import java.util.Collections

import jason.asSyntax.Literal
import jason.asSyntax.Structure
import jason.environment.Environment
import play.api.libs.json._
import sttp.client.quick._

class HomeEnvironment extends Environment {

  val exampleAction: Literal = Literal.parseLiteral("action(some)")

  private val logger = Logger.getLogger(classOf[HomeEnvironment].getName)

  private val apiUri = uri"http://127.0.0.1:8090/api/home"

  override def init(args: Array[String]): Unit = {
  }

  override def getPercepts(agName: String): util.Collection[Literal] = {
    val response = quickRequest.get(apiUri).send()
    val literal = jsonToLiteralString(Json.parse(response.body), "home")
    Collections.singletonList(Literal.parseLiteral(literal))
  }

  def jsonToLiteralString(jsValue: JsValue, wrapper: String): String = {
    def jsonToLiteralString: JsValue => String = {
      case JsObject(fields) => fields.map(f => s"'${f._1}'(${jsonToLiteralString(f._2)})").mkString(",")
      case JsArray(values) => s"[${values.map {
        case obj: JsObject => s"e(${jsonToLiteralString(obj)})"
        case primitive => jsonToLiteralString(primitive)
      }.mkString(",")}]"
      case JsString(str) => s"'$str'"
      case JsBoolean(bool) => bool.toString
      case JsNumber(num) => num.toString
      case JsNull => "null"
      case _ => ""
    }
    s"$wrapper(${jsonToLiteralString(jsValue)})"
  }

  override def executeAction(ag: String, action: Structure): Boolean = {
    if (exampleAction == action) logger.log(Level.INFO, "Acting..")
    true
  }
}
