package business

import akka.actor.{ActorRef, Props, Actor}
import akka.pattern.pipe
import business.stackoverflow.StackOverflowApi
import StackOverflowApi.COMMON_PAGESIZE
import business.actor.TagListFetcher
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class TempActor extends Actor {

  var s: ActorRef = null

  override def receive: Receive = {
    case "start" =>
      context.system.actorOf(Props(new TagListFetcher(self)))
      s = sender
    case message =>
      s ! message
  }

}
