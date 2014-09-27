package core.actor

import java.util.UUID

import akka.actor.{Props, Actor, ActorRef, ActorLogging}
import core.actor.StackOverflowApiClient.{Response, Get}
import core.actor.utils._
import core.stackoverflow.StackOverflowApi._
import models.Tag
import org.joda.time.DateTime
import play.modules.reactivemongo.ReactiveMongoPlugin.db
import play.modules.reactivemongo.json.collection.JSONCollection
import akka.pattern.pipe
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.JsonFormats.tagFormat
import play.api.Play.current

object TagStatsUpdater {

  case class TagPersisted(id: String)
  case class TagUpdated(tag: Tag)

  def actorName(tagName: String) = s"tag_stats_updater_${UUID.randomUUID().toString}" // TODO find a way to encode tag name safely

  def props(apiClient: ActorRef, tagName: String) = Props(classOf[TagStatsUpdater], apiClient, tagName)

}

class TagStatsUpdater(apiClient: ActorRef, tagName: String) extends Actor with ActorLogging with RandomIdGenerator {

  import TagStatsUpdater._

  def collection: JSONCollection = db.collection[JSONCollection]("tag")

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
      val updatedTag = tag.copy(accepted = accepted, rate = accepted.toDouble / tag.total.toDouble, updated = DateTime.now)
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
    (
      discardUnhandled(log)(classOf[Response], classOf[TagPersisted])
        orElse throwOnNonTerminated
        orElse PartialFunction(super.unhandled _)
    )(message)
  }

  override def receive: Receive = fetchingTotal(currentId, Tag(tagName, 0L, 0L, 0, null))

}
