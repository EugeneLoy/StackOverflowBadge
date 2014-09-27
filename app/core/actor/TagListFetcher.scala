package core.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import core.actor.utils._
import core.actor.StackOverflowApiClient.{Response, Get}
import core.stackoverflow.StackOverflowApi._

object TagListFetcher {

  case class TagsFetched(tags: Seq[String])

}

class TagListFetcher(apiClient: ActorRef) extends Actor with ActorLogging with RandomIdGenerator {

  import TagListFetcher._

  var pendingRequests = Set.empty[String]
  var tags: Seq[String] = Nil

  apiClient ! Get(nextId, "tags", FILTER_TOTAL)

  def fetchingTotal(totalRequestId: String): Receive = {
    case Response(`totalRequestId`, 200, content) =>
      (1L to totalPages(content)).foreach { page =>
        apiClient ! Get(nextId, "tags", COMMON_PAGESIZE, ("page", page.toString))
        pendingRequests += currentId
      }
      context.become(fetchingTags)
  }

  def fetchingTags : Receive = {
    case Response(id, 200, content) if (pendingRequests contains id)=>
      tags ++= (content \ "items" \\ "name").map(_.as[String])
      pendingRequests -= id
      if (pendingRequests.isEmpty) {
        context.parent ! TagsFetched(tags)
        context.stop(self)
      }
  }

  override def unhandled(message: Any) = {
    (logUnhandledResponses(log) orElse throwOnNonTerminated orElse PartialFunction(super.unhandled _))(message)
  }

  override def receive: Receive = fetchingTotal(currentId)

}
