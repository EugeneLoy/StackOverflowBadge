package core

import akka.actor.{Actor, ActorRef, Props}
import core.actor.TagListFetcher

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
