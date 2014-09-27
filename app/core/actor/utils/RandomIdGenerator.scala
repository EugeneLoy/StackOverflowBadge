package core.actor.utils

trait RandomIdGenerator {

  var currentId: String = nextId

  def nextId: String = {
    currentId = java.util.UUID.randomUUID.toString
    currentId
  }

}
