package core.actor


import akka.actor.{Terminated, Props, ActorRef}
import akka.persistence.{RecoveryCompleted, PersistentActor}
import core.actor.utils._
import models.Tag

object StatsUpdater {

  case class StatsUpdated()
  case class RateTopsPersisted(id: String)

  def ACTOR_NAME = "stats_updater"

  def props(apiClient: ActorRef) = Props(new StatsUpdater(apiClient))

  sealed trait Command
  sealed trait Event

  case object Recover extends Command
  case object FetchTagList extends Command
  case class FetchTags(tags: Set[String])

  case object UpdateStats

  case object FetchingTagListStarted
  case class FetchingTagsStarted(tags: Set[String])
  case class TagFetched(tag: Tag)
  case object UpdatingStatsStarted

}

class StatsUpdater(
  apiClient: ActorRef,
  _persistenceId: String = "stats_updater_persistence_id",
  tagListFetcherProps: ActorRef => Props = TagListFetcher.props,
  tagFetcherProps: (ActorRef, String) => Props = TagFetcher.props,
  tagPersisterProps: Set[Tag] => Props = TagPersister.props
) extends PersistentActor with Subtasks with  RestartLogging {

  import StatsUpdater._

  override def persistenceId: String = _persistenceId

  var pendingTags = Set.empty[String]
  var fetchedTags = Set.empty[Tag]

  context.watch(apiClient)

  override def receiveRecover: Receive = {
    case e @ FetchingTagListStarted =>
      log.info(s"Recovery: $e")
      context.become(fetchingTagList)
    case e @ FetchingTagsStarted(tags) =>
      log.info(s"Recovery: $e")
      pendingTags = tags
      fetchedTags = Set.empty
      context.become(fetchingTags)
    case e @ TagFetched(tag) =>
      // TODO uncomment
      //log.debug(s"Recover: $e")
      pendingTags -= tag._id
      fetchedTags += tag
    case e @ UpdatingStatsStarted =>
      log.info(s"Recover: $e")
      context.become(updatingStats)
    case RecoveryCompleted =>
      log.info("Triggering recovery")
      self ! Recover
  }

  override def receiveCommand: Receive = {
    case Recover =>
      log.info("Recovering from clean state - starting to fetch tag list")
      fetchTagList
  }

  def fetchTagList = {
    log.info("fetchTagList invoked")
    persist(FetchingTagListStarted) { _ =>
      context.become(fetchingTagList)
      startFetchingTagList
    }
  }

  def fetchingTagList: Receive = {
    case TagListFetcher.TagListFetched(tags) if subtasks contains sender =>
      log.info(s"tag list fetched, size: ${tags.size}")
      complete(sender)
      fetchTags(tags)
    case Recover =>
      startFetchingTagList
  }

  def startFetchingTagList = start(tagListFetcherProps(apiClient), TagListFetcher.actorName)

  def fetchTags(tags: Set[String]) = {
    log.info("fetchTags invoked")
    persist(FetchingTagsStarted(tags)) { event =>
      pendingTags = event.tags
      fetchedTags = Set.empty[Tag]
      context.become(fetchingTags)
      startFetchingTags
    }
  }

  def fetchingTags: Receive = {
    case TagFetcher.TagFetched(tag) if subtasks contains sender =>
      complete(sender)
      context.unwatch(sender)
      persist(TagFetched(tag)) { event =>
        pendingTags -= event.tag._id
        fetchedTags += event.tag
        checkPendingTags
      }
    case UpdateStats =>
      updateStats
    case Recover =>
      startFetchingTags
  }

  def checkPendingTags = if (pendingTags.isEmpty) {
    log.info(s"no more tags to be fetched - updating stats")
    self ! UpdateStats
  }

  def startFetchingTags = {
    log.info(s"startFetchingTags invoked (fetched: ${fetchedTags.size}, pending: ${pendingTags.size})")
    for (tag <- pendingTags) start(tagFetcherProps(apiClient, tag), TagFetcher.actorName(tag))
    checkPendingTags
  }

  def updateStats = {
    log.info("updateStats invoked")
    persist(UpdatingStatsStarted) { _ =>
      context.become(updatingStats)
      startUpdatingStats
    }
  }

  def updatingStats: Receive = {
    case TagPersister.TagsPersisted if subtasks contains sender =>
      log.info("tags persisted - erasing journal and restarting process")
      complete(sender)
      deleteMessages(lastSequenceNr, true)
      fetchTagList
    case Recover =>
      startUpdatingStats
  }

  def startUpdatingStats = {
    val rates = fetchedTags.map(_.rate).toSeq.sorted.reverse
    val tops = (for (p <- 10 to 50 by 10) yield (p, rates(rates.size * p / 100)))
    val tags = for {
      tag <- fetchedTags
      (top, _) = tops.find(tag.rate > _._2).getOrElse((100, 0))
    } yield tag.copy(top = top)
    start(tagPersisterProps(tags), TagPersister.actorName)
  }

  override def unhandled(message: Any) = {
    // PersistentActor does not always maintain death watch guarantees (Terminated may be delivered even despite
    // unwatch has been called if watchee died while watcher was persisting event/running `persist` handler).
    // The following hack will fix this by discarding irrelevant Terminated messages
    val hack: PartialFunction[Any, Unit] = { case Terminated(actor) if !((subtasks + apiClient) contains actor) => }

    val discardStale = discardUnhandled(log)(
      classOf[TagListFetcher.TagListFetched],
      classOf[TagFetcher.TagFetched],
      TagPersister.TagsPersisted.getClass
    )

    (discardStale orElse hack orElse throwOnNonTerminated orElse PartialFunction(super.unhandled _))(message)
  }

}