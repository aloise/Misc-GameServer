package actors.messages

import play.api.libs.json._

/**
 * User: aloise
 * Date: 10/12/13
 * Time: 10:54 PM
 */

abstract class Recipient {
  def get:Seq[Session]
}

case class SessionRecipient(recipient:Session) extends Recipient {
  def get = List(recipient)
}


case class Response(
  event:String,
  recipient: Recipient,
  data:JsValue
)

object ErrorResponse {
  def apply( event:String, recipient: Recipient, errorMessage:String = "error") =
    Response(event, recipient, Json.obj( "error" -> errorMessage ) )
}

