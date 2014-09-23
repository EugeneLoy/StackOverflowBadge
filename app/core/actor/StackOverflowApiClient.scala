package core.actor

import akka.actor.{ActorRef, Props, Actor, FSM}
import akka.contrib.throttle.TimerBasedThrottler
import core.stackoverflow.StackOverflowApi
import play.api.Logger
import scala.concurrent.duration._
import play.api.libs.json.JsValue
import akka.contrib.throttle.Throttler._
import akka.pattern.pipe
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object StackOverflowApiClient {

  val ACTOR_NAME = "stackOverflowApiClient"
  val ACTOR_PATH = s"../$ACTOR_NAME"

  sealed trait State
  object Active extends State
  object BackingOff extends State

  sealed trait Request
  case class Get(path: String, params: (String, String)*) extends Request

  case class Response(code: Int, content: JsValue)

  case class Perform(request: Request, replyTo: ActorRef)
  case class Deliver(response: Response, to: ActorRef, originalRequest: Request)

}

import StackOverflowApiClient._

class StackOverflowApiClient(api: StackOverflowApi) extends Actor with FSM[State, Unit] {

  val logger = Logger(this.getClass())

  val throttler = context.actorOf(Props(new TimerBasedThrottler(1 msgsPer (1 second)))) // TODO
  throttler ! SetTarget(Some(self))

  def wrapResponse(originalRequest: Request, replyTo: ActorRef)(rawApiResponse: (Int, JsValue)) = {
    val (code, content) = rawApiResponse
    Deliver(Response(code, content), replyTo, originalRequest)
  }

  def perform(request: Request, replyTo: ActorRef) = request match {
    case Get(path, params) =>
      api.get(path, params).map(wrapResponse(request, replyTo)).pipeTo(self)
  }

  startWith(Active, ())

  when(Active) {
    case Event(message: Request, _) =>
      throttler ! Perform(message, sender)
      stay
    case Event(Perform(request, replyTo), _) =>
      perform(request, replyTo)
      stay
    case Event(Deliver(response, to, originalRequest), _) =>
      to ! response
      // TODO handle backoff here
      stay
  }

  when(BackingOff) {
    case _ => stay
  }

  whenUnhandled {
    case message =>
      logger.error(s"Unexpected message: $message, state: $stateName")
      stay
  }

  initialize

}

