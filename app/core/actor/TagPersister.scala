package core.actor

import java.util.UUID.randomUUID

import akka.actor.{Actor, Props}
import core.actor.utils._
import models.Tag

import play.modules.reactivemongo.ReactiveMongoPlugin._
import play.api.Play.current
import akka.pattern._

import play.api.libs.json.Json
import models.JsonFormats.tagFormat
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.bson.{BSONArray, BSONDocument}
import reactivemongo.core.commands.RawCommand


object TagPersister {

  case object TagsPersisted

  def actorName = s"tag_persister_$randomUUID"

  def props(tags: Set[Tag]) = Props(classOf[TagPersister], tags)

}

class TagPersister(tags: Set[Tag]) extends Actor with RandomIdGenerator with RestartLogging {

  import TagPersister._
  import context.dispatcher

  val updateStatements = BSONArray(
    for {
      tag <- tags
      tagBson = Json.toJson(tag).as[BSONDocument]
    } yield BSONDocument(
      "q" -> BSONDocument("_id" -> tag._id),
      "u" -> tagBson,
      "upsert" -> true
    )
  )

  val updateCommand = BSONDocument(
    "update" -> "tag",
    "updates" -> updateStatements,
    "ordered" -> true
  )

  db.command(RawCommand(updateCommand)).map(_ => TagsPersisted).pipeTo(self)

  override def receive: Receive = {
    case TagsPersisted =>
      context.parent ! TagsPersisted
      context.stop(self)
  }

  override def unhandled(message: Any) = {
    (throwOnNonTerminated orElse PartialFunction(super.unhandled _))(message)
  }

}
