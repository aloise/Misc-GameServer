package actors

import actors.messages.UserSession.SessionId
import akka.actor.{ActorRef, Actor}
import play.api.libs.json.JsValue
import play.api.libs.iteratee.{Input, Concurrent}
import actors.messages.{ChatMessage, SingleRecipient, Response}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import models.Users.{ jsonFormat => userJsonFormat }
/**
 * User: aloise
 * Date: 11/9/13
 * Time: 11:16 PM
 */
class UserActor(channel:Concurrent.Channel[JsValue], application:ActorRef, sessionId:SessionId, dbUser:models.User, appProfile:models.ApplicationProfile ) extends Actor {

  var game:Option[ActorRef] = None
  var gameProfile:Option[models.GameProfile] = None

  def receive  = {

    case s:actors.messages.Response =>
      channel.push( s.toJson )

    // we've got a chat message. let's transform it
    case c@ChatMessage( eventName, fromSessionId, applicationIdOpt, gameIdOpt, date, data, message, recipient, senderUser ) =>
      val responseJs = Json.obj(
        "message" -> message,
        "date" -> date,
        "fromUser" -> Json.toJson( senderUser )
      )



      val response = new Response( eventName, SingleRecipient( sessionId ), responseJs )
      channel.push( response.toJson )

  }


  override def postStop() {
    channel.push(Input.EOF)
  }

}
