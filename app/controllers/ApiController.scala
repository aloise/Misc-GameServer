package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee.{Enumerator, Iteratee}
import socketio.SocketIOController
import akka.actor.{Props, ActorRef}
import play.api.libs.concurrent.Akka
import play.api.Play.current

object ApiController extends Controller {

  lazy val gatewayActor: ActorRef = Akka.system.actorOf(Props[actors.Gateway])

  def index = WebSocket.using[String] { request =>

  // Log events to the console
    val in = Iteratee.foreach[String](println).map { _ =>
      println("Disconnected")
    }

    // Send a single 'Hello!' message
    val out = Enumerator("Hello!")

    (in, out)
  }



}