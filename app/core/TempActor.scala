package core

import akka.actor.{Actor, ActorRef, Props}
import core.actor.{StackOverflowApiClient, TagListFetcher}
import concurrent.duration._
import scala.concurrent.Await

class TempActor extends Actor {

  val apiClient = Await.result(context.actorSelection(StackOverflowApiClient.ACTOR_PATH).resolveOne(1 hour), 1 hour)
  var s: ActorRef = null

  override def receive: Receive = {
    case "" =>
      context.actorOf(Props(new TagListFetcher(apiClient)))
      s = sender
    case message =>
      s ! message
  }

}
