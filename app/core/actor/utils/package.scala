package core.actor

import scala.reflect.ClassTag
import akka.actor.Terminated
import akka.event.LoggingAdapter

package object utils {

  def discardUnhandled(log: LoggingAdapter)(types: Class[_]*): PartialFunction[Any, Unit] = {
    case message if types.exists(_.isInstance(message)) =>
      log.warning(s"Unexpected message received (leftover from external actor state before restart?): $message. Discarding it.")
  }

  def throwOnNonTerminated: PartialFunction[Any, Unit] = {
    case message: Exception =>
      throw new Exception("Unhandled exception message received", message)
    case message if !message.isInstanceOf[Terminated] =>
      throw new UnsupportedOperationException(s"Unhandled message received: ${message.toString}")
  }

}
