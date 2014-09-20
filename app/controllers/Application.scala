package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

import business.StackOverflowApi
import StackOverflowApi.COMMON_PAGESIZE


object Application extends Controller {
  
  def index = Action {
    Async {
      StackOverflowApi.get("tags", COMMON_PAGESIZE).map { case (_, content)  =>
        Ok(content)
      }
    }
  }

}