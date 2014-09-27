package core.actor

import akka.actor.Terminated
import akka.event.LoggingAdapter
import core.actor.StackOverflowApiClient.Response

package object utils {

  def logUnhandledResponses(log: LoggingAdapter): PartialFunction[Any, Unit] = {
    case response: Response =>
      log.warning(s"Unexpected response received (to request made before actor restart?): $response. Ignoring it.")
  }

  def throwOnNonTerminated: PartialFunction[Any, Unit] = {
    case message: Exception =>
      throw new Exception("Unhandled exception message received", message)
    case message if !message.isInstanceOf[Terminated] =>
      throw new Exception(s"Unhandled message received: ${message.toString}")
  }

}
