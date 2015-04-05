package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee.{Enumerator, Iteratee}

object ApiController extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def websocketTest = WebSocket.using[String] { request =>
    // Log events to the console
    val in = Iteratee.foreach[String](println).mapDone { _ =>
      println("Disconnected")
    }

    // Send a single 'Hello!' message
    val out = Enumerator("Hello!")

    (in, out)
  }


}