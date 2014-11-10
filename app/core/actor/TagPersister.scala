package core.actor

import java.util.UUID.randomUUID

import models.Tag

object TagPersister {

  def actorName = s"tag_persister_$randomUUID"

  def props(tags: Set[Tag]) = null

  case object TagsPersisted

}
