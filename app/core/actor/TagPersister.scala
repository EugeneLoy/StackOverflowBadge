package core.actor

import java.util.UUID.randomUUID

import akka.actor.{ActorLogging, Actor, Props}
import core.actor.utils._
import models.Tag
import play.modules.reactivemongo.ReactiveMongoPlugin._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.Play.current
import akka.pattern._
import models.JsonFormats.tagFormat

import scala.concurrent.Future

object TagPersister {

  case object TagsPersisted

  def actorName = s"tag_persister_$randomUUID"

  def props(tags: Set[Tag]) = Props(classOf[TagPersister], tags)

}

class TagPersister(tags: Set[Tag]) extends Actor with ActorLogging with RandomIdGenerator {

  import TagPersister._
  import context.dispatcher

  def collection: JSONCollection = db.collection[JSONCollection]("tag")

  val saveStatuses = tags.map(collection.save(_).withFilter(!_.inError))
  Future.sequence(saveStatuses).map(_ => TagsPersisted).pipeTo(self)

  override def receive: Receive = {
    case TagsPersisted =>
      context.parent ! TagsPersisted
  }

  override def unhandled(message: Any) = {
    (throwOnNonTerminated orElse PartialFunction(super.unhandled _))(message)
  }

}
