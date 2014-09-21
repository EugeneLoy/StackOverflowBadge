package business.stackoverflow

import play.api.libs.json.JsValue

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Implicits {

  implicit class FutureExtensions(f: Future[(Int, JsValue)]) {
    def expect(code: Int): Future[JsValue] = f.flatMap {
      case (`code` , response) => Future.successful(response)
      case (code, response) => Future.failed(new UnexpectedApiResponse(code, response.toString))
    }
    def expectOk: Future[JsValue] = expect(200)
  }

}
