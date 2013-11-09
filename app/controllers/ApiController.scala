package controllers

import play.api._
import play.api.mvc.{Controller, WebSocket}
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import akka.actor.{Props, ActorRef}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import actors.Gateway
import akka.util.Timeout
import play.api.libs.iteratee.Step.Done
import actors.messages._
import play.api.libs.iteratee.Input
import java.util.{Date, UUID}
import play.api.libs.iteratee.Step.Done
import actors.messages.UserConnect
import actors.messages.UserConnectAccepted
import actors.messages.UserConnectFailed
import actors.messages.UserDisconnected
import akka.pattern._
import actors.messages.UserSession._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._


object ApiController extends Controller {

  type Payload = JsValue

  lazy val supervisor = Akka.system.actorOf(Props[Gateway])

  def index = {

    implicit val timeout = Timeout( 30 seconds )


    WebSocket.async[JsValue]{ request =>

      val sessionId = UserSession.random

      (supervisor ? UserConnect(sessionId) ).map {

        case c: UserConnectAccepted =>
          val iteratee = Iteratee.foreach[JsValue] { event =>

//            c.receiver ! parseRequestJson(sessionId, event)
            supervisor ! parseRequestJson(sessionId, event)

          }.map { _ =>
            supervisor ! UserDisconnected(sessionId)
          }
          (iteratee, c.enumerator)

        case UserConnectFailed(id, error) =>
          // Connection error

          // A finished Iteratee sending EOF
//        val iteratee = Done[JsValue, Unit]( Json.obj(), Input.EOF)
          val iteratee = Iteratee.skipToEof[JsValue]

          // Send an error and close the socket
          val enumerator = Enumerator[JsValue]( Json.obj("error" -> error) ).andThen(Enumerator.enumInput(Input.EOF))

          (iteratee, enumerator)
      }
    }
  }

  private def parseRequestJson(sessionId:Session, req:JsValue) = {

    // Handle event

    GeneralRequest(
      ( req \ "event" ).asOpt[String].getOrElse("_unknown"),
      sessionId,
      ( req \ "applicationId").asOpt[Int],
      ( req \ "gameId").asOpt[Int],
      new Date(),
      req \ "data"
    )


  }

}