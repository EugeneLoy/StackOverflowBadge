package core.actor

import akka.actor.ActorRef
import core.stackoverflow.StackOverflowApi
import StackOverflowApiClient.Get
import StackOverflowApi.FILTER_TOTAL

class TagListFetcher(recipient: ActorRef) extends AllHandlingActor {

  val client = context.actorSelection(StackOverflowApiClient.ACTOR_PATH)
  client ! Get("tags", FILTER_TOTAL)

  def initial: Receive = {
    case content =>
      recipient ! content
  }

  override def receive: Receive = initial
}
