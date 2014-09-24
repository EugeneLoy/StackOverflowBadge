package core.stackoverflow

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.ning.NingWSResponse
import play.api.libs.ws.{WSResponse, Response, WS}
import scala.io.Source
import play.api.Play.current

object StackOverflowApi {

  private val STACKEXCHANGE_API_PATH = "http://api.stackexchange.com/2.2/"
  private val STACKOVERFLOW_SITE = ("site", "stackoverflow")
  private lazy val API_KEY = ("key", current.configuration.getString("stackexchange.key").get)

  val COMMON_ITEMS_PER_PAGE = 100
  val COMMON_PAGESIZE = ("pagesize", COMMON_ITEMS_PER_PAGE.toString)
  val FILTER_TOTAL = ("filter", "total")

  private def parseJson(response: WSResponse): JsValue = {
    // TODO find a better way to do this:
    val content = Source.fromInputStream(new GZIPInputStream(response.asInstanceOf[NingWSResponse].ahcResponse.getResponseBodyAsStream)).getLines().mkString("\n")
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