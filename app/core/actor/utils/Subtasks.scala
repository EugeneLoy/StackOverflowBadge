package core.actor.utils

import akka.actor.{Props, ActorRef, Actor}

/**
  Mixin to handle subtasks - actors that are expected to perform some job on their creation, notify parent about job
  completion and terminate itself.
 */
trait Subtasks {

  this: Actor =>

  var subtasks = Set.empty[ActorRef]

  /**
   * Create subtask (create, watch and add actor to subtask list).
   */
  def start(props: Props, name: String): ActorRef = {
    val subtask = context.watch(context.actorOf(props, name))
    subtasks += subtask
    subtask
  }

  /**
   * Complete subtask.
   */
  def complete(actor: ActorRef): Unit = subtasks -= context.unwatch(actor)

}
