package core.actor

import akka.actor.SupervisorStrategy.{Restart, Escalate}
import akka.actor._
import core.actor.StatsUpdater.StatsUpdated
import core.actor.utils.Subtasks

object UpdateController {

  val ACTOR_NAME = "update_controller"

  def props = Props[UpdateController]

}

class UpdateController extends Actor with ActorLogging {

  val apiClient = context.watch(context.actorOf(StackOverflowApiClient.props, StackOverflowApiClient.ACTOR_NAME))
  val statsUpdater = context.watch(context.actorOf(StatsUpdater.props(apiClient), StatsUpdater.ACTOR_NAME))

  // TODO configure guardian to restart on every exception https://groups.google.com/forum/#!topic/akka-user/QG_DL7FszMU
  override val supervisorStrategy = AllForOneStrategy() {
    case _: Exception => Escalate
  }

  override def receive: Receive = Actor.emptyBehavior

}
