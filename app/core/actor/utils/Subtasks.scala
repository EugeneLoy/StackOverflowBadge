package core.actor.utils

import akka.actor.{Props, ActorRef, Actor}

trait Subtasks {

  this: Actor =>

  var subtasks = Set.empty[ActorRef]

  /**
   * Create subtask (create, watch and add actor to subtask list).
   */
  def start(props: Props, name: String): Unit = subtasks += context.watch(context.actorOf(props, name))

  /**
   * Complete subtask.
   */
  def complete(actor: ActorRef): Unit = subtasks -= context.unwatch(actor)

}
