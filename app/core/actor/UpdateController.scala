package core.actor

import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import core.actor.StatsUpdater.StatsUpdated
import core.actor.utils.Subtasks

object UpdateController {

  val ACTOR_NAME = "update_controller"

  def props = Props[UpdateController]

}

class UpdateController extends Actor with ActorLogging with Subtasks {

  val apiClient = context.watch(context.actorOf(StackOverflowApiClient.props, StackOverflowApiClient.ACTOR_NAME))
  start(StatsUpdater.props(apiClient), StatsUpdater.actorName)

  // TODO configure guardian to restart on every exception https://groups.google.com/forum/#!topic/akka-user/QG_DL7FszMU
  override val supervisorStrategy = AllForOneStrategy() {
    case _: Exception => Escalate
  }

  override def preStart = {
    log.info("About to start update controller")
    super.preStart
  }

  override def receive: Receive = {
    case StatsUpdated() if subtasks contains sender =>
      complete(sender)
      log.info("Updating stats completed. Repeating update cycle.")
      start(StatsUpdater.props(apiClient), StatsUpdater.actorName)
  }

}
