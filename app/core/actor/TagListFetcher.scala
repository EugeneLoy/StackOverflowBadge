package core.actor

import akka.actor.ActorRef
import core.stackoverflow.StackOverflowApi
import core.actor.StackOverflowApiClient.{Response, Get}
import core.stackoverflow.StackOverflowApi._

class TagListFetcher(apiClient: ActorRef) extends AllHandlingActor {

  var pagesLeft = 0L
  var tags: Seq[String] = Nil

  apiClient ! Get("tags", FILTER_TOTAL)

  def fetchingTotal: Receive = {
    case Response(200, content) =>
      val total = (content \ "total").as[Long]
      pagesLeft = total / COMMON_ITEMS_PER_PAGE + 1
      (1L to pagesLeft).foreach { page =>
        apiClient ! Get("tags", COMMON_PAGESIZE, ("page", page.toString))
      }
      context.become(fetchingTags)
  }

  def fetchingTags : Receive = {
    case Response(200, content) =>
      tags ++= (content \ "items" \\ "name").map(_.as[String])
      pagesLeft -= 1
      if (pagesLeft == 0) {
        context.parent ! (tags.size, tags)
        context.stop(self)
      }

  }

  override def receive: Receive = fetchingTotal

  // TODO timeouts responses from before restart

}
