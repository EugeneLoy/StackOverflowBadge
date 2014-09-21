package business.actor

import akka.actor.{DeathPactException, Terminated, Actor}

/**
 * Base class for actors that either handle all passed messages or raise
 * an exception in case message is unexpected.
 */
abstract class AllHandlingActor extends Actor {

  override def unhandled(message: Any) = message match {
    case message: Terminated ⇒ super.unhandled(message)
    case message: Exception => throw new Exception("Unhandled exception message received", message)
    case message => throw new Exception(s"Unhandled message received: ${message.toString}")
  }

}
