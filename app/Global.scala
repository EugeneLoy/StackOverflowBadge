import akka.actor.{ActorRef, Props}
import core.actor.{UpdateController, StatsUpdater, StackOverflowApiClient}
import core.stackoverflow.StackOverflowApi
import play.api._
import play.api.libs.concurrent.Akka

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    implicit val application = app
    val apiClient = Akka.system.actorOf(UpdateController.props, UpdateController.ACTOR_NAME)
  }

}