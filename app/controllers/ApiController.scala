package controllers

import play.api._
import play.api.libs.json._
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
import play.api.libs.functional.syntax._



object ApiController extends Controller {

  type Payload = JsValue

  //
  val supervisor = common.Global.gateway

  def index = {

    implicit val timeout = Timeout( 60 seconds )


    WebSocket.tryAccept[JsValue]{ request =>

      val sessionId = UserSession.random

//      Logger.trace( s"Api WS Request sessionId : $sessionId : $request" )

      (supervisor ? Gateway.UserConnect(sessionId) ).map {

        case c: Gateway.UserConnectAccepted =>

          val iteratee = Iteratee.foreach[JsValue] { event =>

            // c.receiver ! parseRequestJson(sessionId, event)
            // pass the message to the supervisor

            val parsedRequestOpt = parseRequestJson(sessionId, event)

            Logger.trace( s"Api WS Parsed Request sessionId : $sessionId : $parsedRequestOpt" )


            parsedRequestOpt.foreach {
              supervisor ! _
            }


          }.map { _ =>
            supervisor ! Gateway.UserDisconnected(sessionId)
          }

          Right( (iteratee, c.enumerator) )

        case Gateway.UserConnectFailed(id, error) =>
          // Connection error

          // A finished Iteratee sending EOF
//        val iteratee = Done[JsValue, Unit]( Json.obj(), Input.EOF)
//          val iteratee = Iteratee.skipToEof[JsValue]

          // Send an error and close the socket
//          val enumerator = Enumerator[JsValue]( Json.obj("error" -> error) ).andThen(Enumerator.enumInput(Input.EOF))

          Left( BadRequest( Json.obj("error" -> error) ) )
      }
    }
  }

  private def parseRequestJson(sessionId:SessionId, req:JsValue) = {

    // Handle event

    val validator = (
      ( __ \ "event" ).read[String] and
      ( __ \ "applicationId" ).readNullable[String] and
      ( __ \ "gameId" ).readNullable[String] and
      ( __ \ "data" ).readNullable[JsValue]
    ).tupled

    req.validate( validator ).map { case ( event, applicationId, gameIdOpt, data) =>
      GeneralRequest( event, sessionId, applicationId, gameIdOpt, new Date(), data.getOrElse( Json.obj() ) )
    }.asOpt


  }

}