package core.actor

import java.util.Date

import akka.actor.{Actor, ActorRef, ActorLogging}
import core.actor.StackOverflowApiClient.{Response, Get}
import core.actor.utils._
import core.stackoverflow.StackOverflowApi._
import models.Tag
import play.modules.reactivemongo.ReactiveMongoPlugin.db
import play.modules.reactivemongo.json.collection.JSONCollection
import akka.pattern.pipe
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.JsonFormats.tagFormat
import play.api.Play.current

object TagStatsUpdater {

  case class TagPersisted(id: String)
  private case class TagUpdated(tag: Tag)

}

class TagStatsUpdater(apiClient: ActorRef, tagName: String) extends Actor with ActorLogging with RandomIdGenerator {

  import TagStatsUpdater._

  def collection: JSONCollection = db.collection[JSONCollection]("tag")

  apiClient ! Get(nextId, "search/advanced", FILTER_TOTAL, ("tagged", tagName))

  def fetchingTotal(totalRequestId: String, tag: Tag): Receive = {
    case Response(`totalRequestId`, 200, content) =>
      apiClient ! Get(nextId, "search/advanced", FILTER_TOTAL, ("tagged", tagName), ("accepted", "True"))
      context.become(fetchingAccepted(currentId, tag.copy(total = (content \ "total").as[Long]))  )
  }

  def fetchingAccepted(acceptedRequestId: String, tag: Tag): Receive = {
    case Response(`acceptedRequestId`, 200, content) =>
      val updatedTag = tag.copy(accepted = (content \ "total").as[Long], updated = new Date)
      val persistingId = nextId
      (for (lastError <- collection.save(updatedTag) if !lastError.inError) yield TagPersisted(persistingId)).pipeTo(self)
      context.become(persisting(persistingId, updatedTag))
  }

  def persisting(persistingId: String, tag: Tag): Receive = {
    case TagPersisted(`persistingId`) =>
      context.parent ! TagUpdated(tag)
      context.stop(self)
  }

  override def unhandled(message: Any) = {
    (logUnhandledResponses(log) orElse throwOnNonTerminated orElse PartialFunction(super.unhandled _))(message)
  }

  override def receive: Receive = fetchingTotal(currentId, Tag(tagName, 0L, 0L, null))

}
