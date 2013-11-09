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




object ApiController extends Controller {

  type Payload = JsValue

  lazy val supervisor = Akka.system.actorOf(Props[Gateway])

  lazy val gatewayActor: ActorRef = Akka.system.actorOf(Props[actors.Gateway])


  /*
  def index = WebSocket.async[JsValue] { request =>

  // Log events to the console

    val in = Iteratee.foreach[JsValue](println).map { _ =>
      println("Disconnected")
    }

    // Send a single 'Hello!' message
    val out = Enumerator[JsValue]( Json.obj("message" -> "Hello!"))

    (in, out)

  }
  */



  def index =  WebSocket.using[JsValue] { request =>


    // Concurernt.broadcast returns (Enumerator, Concurrent.Channel)
    val (out,channel) = Concurrent.broadcast[JsValue]

    //log the message to stdout and send response back to client
    val in = Iteratee.foreach[JsValue] {
      msg => println(msg)
        //the Enumerator returned by Concurrent.broadcast subscribes to the channel and will
        //receive the pushed messages
        val response = Json.obj("message" -> ("RESPONSE TO: " + (msg \ "message").as[String] ) )
        // val response = "RESPONSE :" + msg
        channel.push( response )
    }
    (in,out)
  }


  def index2 = {
    implicit val timeout = Timeout( 1000 )


    WebSocket.async[Payload]{ request =>

      val sessionId = UserSession.random

      (supervisor ? UserConnect(sessionId) ).map {

        case c: UserConnectAccepted[Payload] =>
          val iteratee = Iteratee.foreach[Payload] { event =>

            c.receiver ! parseRequestJson(sessionId, event)

          }.map { _ =>
            supervisor ! UserDisconnected(sessionId)
          }
          (iteratee, c.enumerator)

        case UserConnectFailed(id, error) =>
          // Connection error

          // A finished Iteratee sending EOF
          val iteratee = Done[Payload, Unit]( Json.obj("error" -> error), Input.EOF)

          // Send an error and close the socket
          val enumerator = Enumerator[Payload]( Json.obj("error" -> error) )
            .andThen(Enumerator.enumInput(Input.EOF))

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