package actors.messages

import play.api.libs.json._
import akka.actor.{ActorRef, Props}
import play.api.libs.iteratee.Enumerator
import actors.messages.UserSession.Session

/**
 * User: aloise
 * Date: 10/12/13
 * Time: 10:54 PM
 */

abstract class Message

case class UserConnect(id: Session) extends Message
case class UserConnectAccepted(id: Session, receiver: ActorRef, enumerator: Enumerator[JsValue]) extends Message
case class UserConnectFailed(id: Session, error: String) extends Message
case class UserDisconnected(id:Session) extends Message

case class UserSenderActorInit(id: String, receiverActor: ActorRef)


abstract class Recipients {
  def get:Seq[Session]
}

case class SingleRecipient(recipient:Session) extends Recipients {
  def get = List(recipient)
}


case class Response(
  event:String,
  recipients: Recipients,
  data:JsValue
) {
  def toJson:JsValue = Json.obj(
    "event" -> event,
    "data" -> data
  )
}

object ErrorResponse {
  def apply( event:String, recipients: Recipients, errorMessage:String = "error") =
    Response(event, recipients, Json.obj( "error" -> errorMessage ) )
}



