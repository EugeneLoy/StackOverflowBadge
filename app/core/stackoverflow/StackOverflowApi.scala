package core.stackoverflow

import java.util.zip.GZIPInputStream

import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{Response, WS}
import scala.io.Source

object StackOverflowApi {

  private val STACKEXCHANGE_API_PATH = "http://api.stackexchange.com/2.2/"
  private val STACKOVERFLOW_SITE = ("site", "stackoverflow")
  private lazy val API_KEY = ("key", Play.current.configuration.getString("stackexchange.key").get)

  val COMMON_ITEMS_PER_PAGE = 100
  val COMMON_PAGESIZE = ("pagesize", COMMON_ITEMS_PER_PAGE.toString)
  val FILTER_TOTAL = ("filter", "total")

  private def parseJson(response: Response) = {
    // TODO check, maybe there is a better way to do this:
    val content = Source.fromInputStream(new GZIPInputStream(response.ahcResponse.getResponseBodyAsStream)).getLines().mkString("\n")
    Json.parse(content)
  }

}
class StackOverflowApi {

  import StackOverflowApi._

  def get(path: String, params: (String, String)*) = {
    val query = API_KEY +: STACKOVERFLOW_SITE +: params
    WS.url(STACKEXCHANGE_API_PATH + path).withQueryString(query: _*).get.map { response =>
      (response.status, parseJson(response))
    }
  }

}