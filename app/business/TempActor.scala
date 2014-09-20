package business

import akka.actor.Actor
import akka.pattern.pipe
import StackOverflowApi.COMMON_PAGESIZE
import play.api.libs.concurrent.Execution.Implicits._

class TempActor extends Actor {

  override def receive: Receive = {
    case _ =>
      StackOverflowApi.get("tags", COMMON_PAGESIZE) pipeTo sender
  }

}
