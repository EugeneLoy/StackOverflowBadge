import akka.actor.{Actor, Props, ActorSystem}
import akka.persistence.{RecoveryCompleted, PersistentActor}
import com.typesafe.config.ConfigFactory
import core.actor.StatsUpdater
import core.actor.StatsUpdater.FetchingTagListStarted
import core.actor.TagListFetcher.TagListFetched
import core.actor.TagPersister.TagsPersisted
import core.actor.TagStatsUpdater.TagUpdated
import models.Tag
import org.joda.time.DateTime
import org.scalatest.{Matchers, WordSpecLike}
import java.util.UUID._


class StatsUpdaterTest(_system: ActorSystem) extends ActorTest(_system) with WordSpecLike with Matchers {

  def this() = this(ActorSystem("StatsUpdaterTest", ConfigFactory.load(ConfigFactory.parseString(
    """
    |akka.persistence.snapshot-store.local.dir = "target/snapshots"
    |akka.persistence.journal.leveldb.dir = "target/journal"
    |akka.persistence.journal.leveldb.native = off
    |akka.test.timefactor = 2
    |""".stripMargin
  ))))

  object Fixture {
    val timestamp = DateTime.now()
    val fetchedTags = Map(
      "tag1" -> Tag("tag1", 10, 10, 1, 0, timestamp),
      "tag2" -> Tag("tag2", 10, 9, 0.9, 0, timestamp),
      "tag3" -> Tag("tag3", 10, 8, 0.8, 0, timestamp),
      "tag4" -> Tag("tag4", 10, 7, 0.7, 0, timestamp),
      "tag5" -> Tag("tag5", 10, 6, 0.6, 0, timestamp),
      "tag6" -> Tag("tag6", 10, 5, 0.5, 0, timestamp),
      "tag7" -> Tag("tag7", 10, 4, 0.4, 0, timestamp),
      "tag8" -> Tag("tag8", 10, 3, 0.3, 0, timestamp),
      "tag9" -> Tag("tag9", 10, 2, 0.2, 0, timestamp),
      "tag10" -> Tag("tag10", 10, 1, 0.1, 0, timestamp)
    )
    val tagsToPersist = Set(
      fetchedTags("tag1").copy(top = 10),
      fetchedTags("tag2").copy(top = 20),
      fetchedTags("tag3").copy(top = 30),
      fetchedTags("tag4").copy(top = 40),
      fetchedTags("tag5").copy(top = 50),
      fetchedTags("tag6").copy(top = 100),
      fetchedTags("tag7").copy(top = 100),
      fetchedTags("tag8").copy(top = 100),
      fetchedTags("tag9").copy(top = 100),
      fetchedTags("tag10").copy(top = 100)
    )
  }

  import Mock._

  "StatsUpdater" when {
    "created for the first time" should {
      "start fetching tag list" in {
        val statsUpdaterProps = Props(new StatsUpdater(
          apiClient = system.actorOf(dummy),
          _persistenceId = randomUUID().toString,
          tagListFetcherProps = _ => sendToTestActorWhenCreated("created")
        ))
        system.actorOf(statsUpdaterProps)

        within(commonTimeout) {
          expectMsg("created")
          expectNoMsg
        }
      }
    }

    "recovered from fetching tag list" should {
      "start fetching tag list" in {
        val persistenceId = randomUUID().toString
        val statsUpdaterProps = Props(new StatsUpdater(
          apiClient = system.actorOf(dummy),
          _persistenceId = persistenceId,
          tagListFetcherProps = _ => sendToTestActorWhenCreated("created")
        ))
        var statsUpdater = system.actorOf(statsUpdaterProps)

        within(commonTimeout) {
          expectMsg("created")
          expectNoMsg
        }

        system.stop(statsUpdater)
        statsUpdater = system.actorOf(statsUpdaterProps)

        within(commonTimeout) {
          expectMsg("created")
          expectNoMsg
        }
      }
    }

    "receives tag list" should {
      "start fetching tags" in {
        val statsUpdaterProps = Props(new StatsUpdater(
          apiClient = system.actorOf(dummy),
          _persistenceId = randomUUID().toString,
          tagListFetcherProps = _ => sendToParentWhenCreated(TagListFetched(Set("tag1", "tag2"))),
          tagFetcherProps = (_, tagName) => sendToTestActorWhenCreated(tagName)
        ))
        system.actorOf(statsUpdaterProps)

        within(commonTimeout) {
          expectMsgAllOf("tag1", "tag2")
          expectNoMsg
        }
      }
    }

    "recovered from fetching tags" should {
      "start fetching tags" in {
        val persistenceId = randomUUID().toString
        var statsUpdater = system.actorOf(Props(new StatsUpdater(
          apiClient = system.actorOf(dummy),
          _persistenceId = persistenceId,
          tagListFetcherProps = _ => sendToParentWhenCreated(TagListFetched(Set("tag1", "tag2"))),
          tagFetcherProps = (_, tagName) => sendToTestActorWhenCreated(tagName)
        )))

        within(commonTimeout) {
          expectMsgAllOf("tag1", "tag2")
          expectNoMsg
        }

        system.stop(statsUpdater)
        statsUpdater = system.actorOf(Props(new StatsUpdater(
          apiClient = system.actorOf(dummy),
          _persistenceId = persistenceId,
          tagListFetcherProps = _ => sendToTestActorWhenCreated("should not be sent"),
          tagFetcherProps = (_, tagName) => sendToTestActorWhenCreated(tagName)
        )))

        within(commonTimeout) {
          expectMsgAllOf("tag1", "tag2")
          expectNoMsg
        }
      }
    }

    "recovered from fetching tags (when part of the tags got fetched)" should {
      "start fetching not fetched tags" in {
        val persistenceId = randomUUID().toString
        var statsUpdater = system.actorOf(Props(new StatsUpdater(
          apiClient = system.actorOf(dummy),
          _persistenceId = persistenceId,
          tagListFetcherProps = _ => sendToParentWhenCreated(TagListFetched(Set("tag1", "tag2"))),
          tagFetcherProps = (_, tagName) => tagName match {
            case "tag1" => sendToParentWhenCreated(TagUpdated(Tag("tag1", 0, 0, 0, 0, DateTime.now)))
            case "tag2" => sendToTestActorWhenCreated(tagName)
          }
        )))

        within(commonTimeout) {
          expectMsg("tag2")
          expectNoMsg
        }

        system.stop(statsUpdater)
        statsUpdater = system.actorOf(Props(new StatsUpdater(
          apiClient = system.actorOf(dummy),
          _persistenceId = persistenceId,
          tagListFetcherProps = _ => sendToTestActorWhenCreated("should not be sent"),
          tagFetcherProps = (_, tagName) => tagName match {
            case "tag1" => sendToTestActorWhenCreated("should not be sent")
            case "tag2" => sendToTestActorWhenCreated(tagName)
          }
        )))

        within(commonTimeout) {
          expectMsg("tag2")
          expectNoMsg
        }
      }
    }

    "receives fetched tags" should {
      "start persisting tags" in {
        val statsUpdaterProps = Props(new StatsUpdater(
          apiClient = system.actorOf(dummy),
          _persistenceId = randomUUID().toString,
          tagListFetcherProps = _ => sendToParentWhenCreated(TagListFetched(Fixture.fetchedTags.keySet)),
          tagFetcherProps = (_, tagName) => sendToParentWhenCreated(TagUpdated(Fixture.fetchedTags(tagName))),
          tagPersisterProps = tags => sendToTestActorWhenCreated(tags)
        ))
        system.actorOf(statsUpdaterProps)

        within(commonTimeout) {
          expectMsg(Fixture.tagsToPersist)
          expectNoMsg
        }
      }
    }

    "recovers from persisting tags" should {
      "start persisting tags" in {
        val persistenceId = randomUUID().toString
        var statsUpdater = system.actorOf(Props(new StatsUpdater(
          apiClient = system.actorOf(dummy),
          _persistenceId = persistenceId,
          tagListFetcherProps = _ => sendToParentWhenCreated(TagListFetched(Fixture.fetchedTags.keySet)),
          tagFetcherProps = (_, tagName) => sendToParentWhenCreated(TagUpdated(Fixture.fetchedTags(tagName))),
          tagPersisterProps = tags => sendToTestActorWhenCreated(tags)
        )))

        within(commonTimeout) {
          expectMsg(Fixture.tagsToPersist)
          expectNoMsg
        }
        system.stop(statsUpdater)

        statsUpdater = system.actorOf(Props(new StatsUpdater(
          apiClient = system.actorOf(dummy),
          _persistenceId = persistenceId,
          tagListFetcherProps = _ => sendToTestActorWhenCreated("should not be sent"),
          tagFetcherProps = (_, _) => sendToTestActorWhenCreated("should not be sent"),
          tagPersisterProps = tags => sendToTestActorWhenCreated(tags)
        )))

        within(commonTimeout) {
          expectMsg(Fixture.tagsToPersist)
          expectNoMsg
        }
      }
    }

    "persists tags" should {
      "discard journal and start fetching tag list" in {
        var mockInstanceCounter = 0

        class TagListFetcherMock extends Actor {
          mockInstanceCounter += 1
          if (mockInstanceCounter == 1) context.parent ! TagListFetched(Fixture.fetchedTags.keySet)
          testActor ! s"instance_$mockInstanceCounter"
          def receive = { case _: Any => }
        }

        val _persistenceId = randomUUID().toString

        class StatsUpdaterMock extends Actor with PersistentActor {
          override def receiveRecover: Receive = {
            case message => testActor ! message
          }
          override def receiveCommand: Receive = { case _: Any => }
          override def persistenceId: String = _persistenceId
        }

        val statsUpdaterProps = Props(new StatsUpdater(
          apiClient = system.actorOf(dummy),
          _persistenceId = _persistenceId,
          tagListFetcherProps = _ => Props(new TagListFetcherMock),
          tagFetcherProps = (_, tagName) => sendToParentWhenCreated(TagUpdated(Fixture.fetchedTags(tagName))),
          tagPersisterProps = _ => sendToParentWhenCreated(TagsPersisted)
        ))
        val statsUpdater = system.actorOf(statsUpdaterProps)

        within(commonTimeout) {
          expectMsg("instance_1")
          expectMsg("instance_2")
          expectNoMsg
        }

        system.stop(statsUpdater)

        system.actorOf(Props(new StatsUpdaterMock))
        within(commonTimeout) {
          expectMsg(FetchingTagListStarted)
          expectMsg(RecoveryCompleted)
          expectNoMsg
        }
      }
    }
  }
}
