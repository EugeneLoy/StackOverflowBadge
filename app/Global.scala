import akka.actor.{ActorRef, Props}
import core.actor.StackOverflowApiClient
import core.stackoverflow.StackOverflowApi
import play.api._
import play.api.libs.concurrent.Akka

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    implicit val application = app
    Akka.system.actorOf(Props(new StackOverflowApiClient(new StackOverflowApi)), StackOverflowApiClient.ACTOR_NAME)
  }

}