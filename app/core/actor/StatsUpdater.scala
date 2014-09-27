package core.actor

import java.util.UUID

import akka.actor.{Props, ActorRef, Actor, ActorLogging}
import core.actor.TagListFetcher.TagsFetched
import core.actor.TagStatsUpdater.TagUpdated
import core.actor.utils._
import models.{Tag,RateTops}
import org.joda.time.DateTime
import play.modules.reactivemongo.ReactiveMongoPlugin.db
import play.modules.reactivemongo.json.collection.JSONCollection
import akka.pattern.pipe
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.JsonFormats.rateTopsFormat
import play.api.Play.current

object StatsUpdater {

  case class StatsUpdated()
  case class RateTopsPersisted(id: String)

  def actorName = s"stats_updater_${UUID.randomUUID.toString}"

  def props(apiClient: ActorRef) = Props(classOf[StatsUpdater], apiClient)

}

class StatsUpdater(apiClient: ActorRef) extends Actor with ActorLogging with RandomIdGenerator with Subtasks {

  import StatsUpdater._

  context.watch(apiClient)

  def collection: JSONCollection = db.collection[JSONCollection]("rate_tops")

  var tags = Set.empty[Tag]

  start(TagListFetcher.props(apiClient), TagListFetcher.actorName)

  def calculateTops(rates: Seq[Double]) = {
    val data = rates.sorted.reverse
    (for (p <- 10 to 50 by 10) yield (p, data(data.size * p / 100))).toMap
  }

  def fetchingTagList: Receive = {
    case TagsFetched(tags) if subtasks contains sender =>
      complete(sender)
      for (tagName <- tags) {
         start(TagStatsUpdater.props(apiClient, tagName), TagStatsUpdater.actorName(tagName))
      }
      context.become(updatingTagStats)
  }

  def updatingTagStats: Receive = {
    case TagUpdated(tag) if subtasks contains sender =>
      complete(sender)
      tags += tag
      if (subtasks.isEmpty) {
        val tops = calculateTops(tags.map(_.rate).toSeq)
        val rateTops = RateTops("singleton", tops(10), tops(20), tops(30), tops(40), tops(50), DateTime.now)
        val persistingId = nextId
        (for (lastError <- collection.save(rateTops) if !lastError.inError) yield RateTopsPersisted(persistingId)).pipeTo(self)
        context.become(persisting(persistingId))
      }
  }

  def persisting(persistingId: String): Receive = {
    case RateTopsPersisted(`persistingId`) =>
      context.parent ! StatsUpdated()
      context.stop(self)
  }

  override def unhandled(message: Any) = {
    (
      discardUnhandled(log)(classOf[TagsFetched], classOf[TagUpdated], classOf[RateTopsPersisted])
        orElse throwOnNonTerminated
        orElse PartialFunction(super.unhandled _)
    )(message)
  }

  override def receive: Receive = fetchingTagList

}