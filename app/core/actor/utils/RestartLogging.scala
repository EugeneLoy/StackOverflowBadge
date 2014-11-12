package core.actor.utils

import akka.actor.{Actor, ActorLogging}

trait RestartLogging extends Actor with ActorLogging {

  override def preRestart(reason: Throwable, message: Option[Any]) = {
    log.warning(s"preRestart: reason: ${reason}, message: ${message}")
    super.preRestart(reason, message)
  }

}
