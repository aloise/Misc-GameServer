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
import akka.pattern._
import actors.messages.UserSession._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._


object ApiController extends Controller {

  type Payload = JsValue

  lazy val supervisor = Akka.system.actorOf(Props[Gateway])

  def index = {

    implicit val timeout = Timeout( 60 seconds )


    WebSocket.async[JsValue]{ request =>

      val sessionId = UserSession.random

      (supervisor ? Gateway.UserConnect(sessionId) ).map {

        case c: Gateway.UserConnectAccepted =>

          val iteratee = Iteratee.foreach[JsValue] { event =>

            // c.receiver ! parseRequestJson(sessionId, event)
            // pass the message to the superviser
            supervisor ! parseRequestJson(sessionId, event)

          }.map { _ =>
            supervisor ! Gateway.UserDisconnected(sessionId)
          }

          (iteratee, c.enumerator)

        case Gateway.UserConnectFailed(id, error) =>
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

  private def parseRequestJson(sessionId:SessionId, req:JsValue) = {

    // Handle event

    GeneralRequest(
      ( req \ "event" ).asOpt[String].getOrElse("_unknown"),
      sessionId,
      ( req \ "applicationId").asOpt[String],
      ( req \ "gameId").asOpt[Int],
      new Date(),
      req \ "data"
    )


  }

}