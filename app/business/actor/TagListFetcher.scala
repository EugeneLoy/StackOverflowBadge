package business.actor

import akka.actor.ActorRef
import akka.pattern.pipe
import business.stackoverflow.StackOverflowApi
import StackOverflowApi.{get, FILTER_TOTAL}
import business.stackoverflow.Implicits.FutureExtensions
import play.api.libs.json.JsValue
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class TagListFetcher(recipient: ActorRef) extends AllHandlingActor {

  get("tags", FILTER_TOTAL).expectOk.pipeTo(self)

  def initial: Receive = {
    case content: JsValue =>
      recipient ! (content \ "total").as[Long]
  }

  override def receive: Receive = initial
}
