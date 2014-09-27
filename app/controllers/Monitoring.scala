package controllers

import controllers.Application._
import play.api.libs.concurrent.Akka
import core.utils.ExposePrivateMethods
import play.api.mvc.Action
import play.api.Play.current

object Monitoring {

  def akka = Action {
    Ok(Akka.system.exposeMethod('printTree)().toString)
  }

}
