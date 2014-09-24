package controllers

import akka.actor.{Props, ActorRef}
import akka.util.Timeout
import core.TempActor
import play.api.libs.json.JsValue
import play.api.mvc._


import play.api.Play.current
import akka.pattern.ask
import akka.util.Timeout
//import akka.util.duration._
import scala.concurrent.duration._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits.defaultContext


object Application extends Controller {
  
  def index = Action.async {
    val actor = Akka.system.actorOf(Props[TempActor])
    implicit val timeout = Timeout(10.seconds)
    (actor ? "start").mapTo[Any].map { response =>
      Ok(response.toString)
    }
  }



}