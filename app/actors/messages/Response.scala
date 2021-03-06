package actors.messages

import play.api.libs.json._
import akka.actor.{ActorRef, Props}
import play.api.libs.iteratee.{Concurrent, Enumerator}
import actors.messages.UserSession.SessionId

/**
 * User: aloise
 * Date: 10/12/13
 * Time: 10:54 PM
 */


// internal system messages that are not connected to the external user data
abstract class InternalMessage





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



class Response(
  val event:String,
  val recipients: Recipients,
  val data:JsValue,
  val isSuccess:Boolean = true
) {
  def toJson:JsValue = Json.obj(
    "event" -> event,
    "data" -> data,
    "success" -> isSuccess
  )
}


object Response {
  def apply(event: String, recipients: Recipients, data: JsValue ) =
    new Response(event, recipients, data, true)

  def apply(event: String, recipientSession: SessionId, data: JsValue ) =
    new Response(event, SingleRecipient(recipientSession), data, true)
}


object ErrorResponse {
  def apply( event:String, recipients: Recipients, errorMessage:String = "error") =
    new Response(event, recipients, Json.obj( "error" -> errorMessage ), false )

  def apply( event:String, recipientSession: SessionId, errorMessage:String ) =
    new Response(event, SingleRecipient(recipientSession), Json.obj( "error" -> errorMessage ), false )
}



