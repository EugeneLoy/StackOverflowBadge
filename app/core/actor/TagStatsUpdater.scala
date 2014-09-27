package core.actor

import akka.actor.{Actor, ActorRef, ActorLogging}
import core.actor.StackOverflowApiClient.{Response, Get}
import core.actor.utils._
import core.stackoverflow.StackOverflowApi._

object TagStatsUpdater {

  case class Tag(name: String, total: Long, accepted: Long)

}

class TagStatsUpdater(apiClient: ActorRef, tagName: String) extends Actor with ActorLogging with RandomIdGenerator {

  import TagStatsUpdater._

  apiClient ! Get(nextId, "search/advanced", FILTER_TOTAL, ("tagged", tagName))

  def fetchingTotal(totalRequestId: String, tag: Tag): Receive = {
    case Response(`totalRequestId`, 200, content) =>
      apiClient ! Get(nextId, "search/advanced", FILTER_TOTAL, ("tagged", tagName), ("accepted", "True"))
      context.become(fetchingAccepted(currentId, tag.copy(total = (content \ "total").as[Long]))  )
  }

  def fetchingAccepted(acceptedRequestId: String, tag: Tag): Receive = {
    case Response(`acceptedRequestId`, 200, content) =>
      // TODO persist
      context.parent ! tag.copy(accepted = (content \ "total").as[Long])
  }

  override def unhandled(message: Any) = {
    (logUnhandledResponses(log) orElse throwOnNonTerminated orElse PartialFunction(super.unhandled _))(message)
  }

  override def receive: Receive = fetchingTotal(currentId, Tag(tagName, 0L, 0L))

}
