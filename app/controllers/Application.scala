package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Game server"))
  }

  def test = Action { implicit request =>
    Ok(views.html.application.test())
  }
  
}