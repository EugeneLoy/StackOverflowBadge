package core.actor

import akka.actor.SupervisorStrategy.{Restart, Escalate}
import akka.actor._
import core.actor.StatsUpdater.StatsUpdated
import core.actor.utils.{RestartLogging, Subtasks}

object UpdateController {

  val ACTOR_NAME = "update_controller"

  def props = Props[UpdateController]

}

class UpdateController extends Actor with RestartLogging {

  val apiClient = context.watch(context.actorOf(StackOverflowApiClient.props, StackOverflowApiClient.ACTOR_NAME))
  val statsUpdater = context.watch(context.actorOf(StatsUpdater.props(apiClient), StatsUpdater.ACTOR_NAME))

  override val supervisorStrategy = AllForOneStrategy() {
    case _: Exception => Restart
  }

  override def receive: Receive = Actor.emptyBehavior

}
