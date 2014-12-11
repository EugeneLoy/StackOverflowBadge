package core.actor

import akka.actor._
import akka.contrib.throttle.TimerBasedThrottler
import akka.dispatch.{UnboundedPriorityMailbox, PriorityGenerator}
import com.typesafe.config.Config
import core.actor.utils.RestartLogging
import core.stackoverflow.StackOverflowApi
import play.api.Logger
import scala.concurrent.duration._
import play.api.libs.json.JsValue
import akka.contrib.throttle.Throttler._
import akka.pattern.pipe
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object StackOverflowApiClient {

  val ACTOR_NAME = "stackoverflow_api_client"

  // see: https://api.stackexchange.com/docs/error-handling
  val BACKOFF_ERROR_IDS = Set(502, 503)

  sealed trait State
  object Active extends State
  object BackingOff extends State

  sealed trait Request {
    val id: String
  }
  case class Get(id: String, path: String, params: (String, String)*) extends Request

  case class Response(id: String, code: Int, content: JsValue)

  case class Perform(request: Request, replyTo: ActorRef)
  case class Deliver(response: Response, to: ActorRef, originalRequest: Request)

  case class StopBackingOff()

  def props = Props(classOf[StackOverflowApiClient], new StackOverflowApi, 1 msgsPer (1 second), 1 hour)

}


class StackOverflowApiClient(api: StackOverflowApi, requestRate: Rate, backoffDuration: FiniteDuration)
  extends Actor with FSM[StackOverflowApiClient.State, Unit] with Stash with RestartLogging {

  import StackOverflowApiClient._

  val throttler = context.actorOf(Props(classOf[TimerBasedThrottler], requestRate), "throttler")
  throttler ! SetTarget(Some(self))

  def wrapResponse(originalRequest: Request, replyTo: ActorRef)(rawApiResponse: (Int, JsValue)) = {
    val (code, content) = rawApiResponse
    Deliver(Response(originalRequest.id, code, content), replyTo, originalRequest)
  }

  def perform(request: Request, replyTo: ActorRef) = request match {
    case Get(id, path, params @ _*) =>
      api.get(path, params: _*).map(wrapResponse(request, replyTo)).pipeTo(self)
  }

  def isApiLimitsViolated(response: Response) = response match {
    case Response(_, 400, content) if (!(content \ "error_id").asOpt[Int].filter(BACKOFF_ERROR_IDS contains _).isEmpty) => true
    case _ => false
  }

  def repeatLater(message: Any, sender: ActorRef) = {
    // akka.actor.Stash is not capable of stashing arbitrary messages (only current ones)
    // and there is no need to preserve requests order now, so lets just post message to itself, pretending
    // that we are original message sender
    self.tell(message, sender)
  }

  startWith(Active, ())

  when(Active) {
    case Event(request: Request, _) =>
      throttler ! Perform(request, sender)
      stay
    case Event(Perform(request, replyTo), _) =>
      perform(request, replyTo)
      stay
    case Event(Deliver(response, originalSender, originalRequest), _) if (isApiLimitsViolated(response)) =>
      repeatLater(originalRequest, originalSender)
      goto(BackingOff)
    case Event(Deliver(response, to, _), _) =>
      to ! response
      stay
  }

  when(BackingOff) {
    case Event(request: Request, _) =>
      stash
      stay
    case Event(Perform(request, originalSender), _) =>
      repeatLater(request, originalSender)
      stay
    case Event(Deliver(response, originalSender, originalRequest), _) if (isApiLimitsViolated(response)) =>
      repeatLater(originalRequest, originalSender)
      stay
    case Event(Deliver(response, to, _), _) =>
      to ! response
      stay
    case Event(StopBackingOff(), _) =>
      goto(Active)
  }

  whenUnhandled {
    case message =>
      log.error(s"Unexpected message: $message, state: $stateName")
      stay
  }

  onTransition {
    case Active -> BackingOff =>
      log.info("Starting backoff")
      setTimer("backoff", StopBackingOff(), backoffDuration, false)
    case BackingOff -> Active =>
      log.info("Stopping backoff")
      unstashAll()
  }

  initialize

}

