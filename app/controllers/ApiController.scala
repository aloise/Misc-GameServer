package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee.{Enumerator, Iteratee}
import socketio.SocketIOController
import akka.actor.{Props, ActorRef}
import play.api.libs.concurrent.Akka

object ApiController extends SocketIOController {

  lazy val socketIOActor: ActorRef = Akka.system.actorOf(Props[actors.Gateway])

  def index = Action {
    Ok(views.html.index())
  }



}