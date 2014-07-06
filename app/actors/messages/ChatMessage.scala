package actors.messages

import java.util.Date

import actors.messages.UserSession.SessionId
import play.api.libs.json.{JsString, Json, JsValue}

/**
 * User: aloise
 * Date: 06.07.14
 * Time: 19:08
 */
case class ChatMessage (
  override val event:String = "chat", // event name
  override val sessionId: SessionId,
  override val applicationId:Option[String], // target application id
  override val gameId:Option[String], // target game id
  override val date:Date,
  override val data:JsValue, // event data

  message:String,
  recipient: ChatMessageRecipient,
  sender: String // sender username

) extends Request {
  def toJson:JsValue = Json.obj(
    "event" -> event,
    "message" -> message,
    "data" -> "data",
    "recipient" -> ( recipient match {
      case ApplicationChatMessageRecipient( applicationGid ) =>
        Json.obj( "applicationGid" -> applicationGid )
      case GameChatMessageRecipient( gameId ) =>
        Json.obj( "gameId" -> gameId )
      case UserListChatMessageRecipient( usernames ) =>
        Json.obj( "usernames" -> usernames )
      case _ =>
        JsString( "unknown" )
    } ),
    "sender" -> sender
  )
}

abstract class ChatMessageRecipient

case class ApplicationChatMessageRecipient( applicationGid:String ) extends ChatMessageRecipient
case class GameChatMessageRecipient( gameId:String ) extends ChatMessageRecipient
case class UserListChatMessageRecipient( usernames:Seq[String] ) extends ChatMessageRecipient