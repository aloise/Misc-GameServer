package controllers

import play.api._
import play.api.mvc._
import models.User

object TestController extends Controller {

  def index = Action {
    Ok(views.html.test.index())
  }

  def addUser = Action {

    val user = User( username = "aloise")
    User.insert(user)

    Ok(views.html.test.addUser())
  }

  def listUsers = Action {

    Ok(views.html.test.listUsers())
  }

}