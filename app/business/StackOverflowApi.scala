package business

import java.util.zip.GZIPInputStream

import play.api.Play
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.libs.concurrent.Execution.Implicits._
import scala.io.Source

object StackOverflowApi {

  val STACKEXCHANGE_API_PATH = "http://api.stackexchange.com/2.2/"
  val COMMON_PAGESIZE = ("pagesize", "100")
  val STACKOVERFLOW_SITE = ("site", "stackoverflow")

  lazy val API_KEY = ("key", Play.current.configuration.getString("stackexchange.key").get)

  def parseJson(responce: Response) = {
    // TODO check, maybe there is a better way to do this:
    val content = Source.fromInputStream(new GZIPInputStream(responce.ahcResponse.getResponseBodyAsStream)).getLines().mkString("\n")
    Json.parse(content)
  }

  def get(path: String, params: (String, String)*) = {
    val query = API_KEY +: STACKOVERFLOW_SITE +: params
    WS.url(STACKEXCHANGE_API_PATH + path).withQueryString(query:_*).get.map { response =>
      (response.status, parseJson(response))
    }
  }

}