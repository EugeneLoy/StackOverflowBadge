package business

import java.util.zip.GZIPInputStream

import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.libs.concurrent.Execution.Implicits._

import scala.io.Source
object StackOverflowApi {

  val STACKEXCHANGE_API_PATH = "http://api.stackexchange.com/2.2/"
  val COMMON_PAGESIZE = ("pagesize", "100")

  def parseJson(responce: Response) = {
    // TODO check, maybe there is a better way to do this:
    val content = Source.fromInputStream(new GZIPInputStream(responce.ahcResponse.getResponseBodyAsStream)).getLines().mkString("\n")
    Json.parse(content)
  }

  def get(path: String, params: (String, String)*) = {
    WS.url(STACKEXCHANGE_API_PATH + path).withQueryString((("site", "stackoverflow") +: params):_*).get.map { responce =>
      (responce.status, parseJson(responce))
    }
  }

}