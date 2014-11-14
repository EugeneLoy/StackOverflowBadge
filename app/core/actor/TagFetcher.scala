package core.actor

import java.util.UUID.randomUUID

import akka.actor._
import core.actor.StackOverflowApiClient.{Response, Get}
import core.actor.utils._
import core.stackoverflow.StackOverflowApi._
import models.Tag
import org.joda.time.DateTime

object TagFetcher {

  case class TagFetched(tag: Tag)

  def actorName(tagName: String) = s"tag_fetcher_${tagName.replaceAll("[^a-zA-Z0-9]", "-")}_$randomUUID"

  def props(apiClient: ActorRef, tagName: String) = Props(classOf[TagFetcher], apiClient, tagName)

}

class TagFetcher(apiClient: ActorRef, tagName: String) extends Actor with RandomIdGenerator with RestartLogging {

  import TagFetcher._

  context.watch(apiClient)

  apiClient ! Get(nextId, "search/advanced", FILTER_TOTAL, ("tagged", tagName))

  def fetchingTotal(totalRequestId: String, tag: Tag): Receive = {
    case Response(`totalRequestId`, 200, content) =>
      apiClient ! Get(nextId, "search/advanced", FILTER_TOTAL, ("tagged", tagName), ("accepted", "True"))
      context.become(fetchingAccepted(currentId, tag.copy(total = (content \ "total").as[Long])))
  }

  def fetchingAccepted(acceptedRequestId: String, tag: Tag): Receive = {
    case Response(`acceptedRequestId`, 200, content) =>
      val accepted = (content \ "total").as[Long]
      val updatedTag = tag.copy(
        accepted = accepted,
        rate = accepted.toDouble / tag.total.toDouble,
        updated = DateTime.now
      )
      context.parent ! TagFetched(updatedTag)
      context.stop(self)
  }

  override def unhandled(message: Any) = {
    (
      discardUnhandled(log)(classOf[Response])
        orElse throwOnNonTerminated
        orElse PartialFunction(super.unhandled _)
    )(message)
  }

  override def receive: Receive = fetchingTotal(currentId, Tag(tagName, 0L, 0L, 0.0, 0L, null))

}
