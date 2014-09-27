package core.actor

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging}
import core.actor.utils.RandomIdGenerator

object StatsUpdater {

}

class StatsUpdater extends Actor with ActorLogging with RandomIdGenerator {

  import StatsUpdater._

  override def receive: Receive = ???
}