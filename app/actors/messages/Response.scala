package actors.messages

import play.api.libs.json._
import akka.actor.{ActorRef, Props}
import play.api.libs.iteratee.Enumerator
import actors.messages.UserSession.SessionId

/**
 * User: aloise
 * Date: 10/12/13
 * Time: 10:54 PM
 */


// internal system messages that are not connected to the external user data
abstract class Message

case class UserConnect(id: SessionId) extends Message
case class UserConnectAccepted(id: SessionId, receiver: ActorRef, enumerator: Enumerator[JsValue]) extends Message
case class UserConnectFailed(id: SessionId, error: String) extends Message

// it's sent to gateway and forwarded to the Application and the game
case class UserDisconnected(id:SessionId) extends Message

// it's sent from the Gateway to the Application
case class UserLoggedIn( user:UserSession) extends Message


case class UserSenderActorInit(id: String, receiverActor: ActorRef)


abstract class Recipients {
  def foreach[A]( f: SessionId => A )
}

object EmptyRecipient extends Recipients {
  def foreach[A]( f: SessionId => A ) {
    Unit
  }
}

case class SingleRecipient(recipient:SessionId) extends Recipients {
  def foreach[A]( f:SessionId => A ) {
    f(recipient)
  }
}

case class MultipleRecipients(recipients:Seq[SessionId]) extends Recipients {
  def foreach[A]( f:SessionId => A ) {
    recipients.foreach( f )
  }
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

  def apply( event:String, recipientSession: SessionId, errorMessage:String ) =
    Response(event, SingleRecipient(recipientSession), Json.obj( "error" -> errorMessage ) )
}



