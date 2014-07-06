package actors.messages

import java.util.Date

import actors.messages.UserSession.SessionId
import play.api.libs.json.{JsSuccess, JsString, Json, JsValue}

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
    "recipient" -> (
      recipient match {
        case a:ApplicationChatMessageRecipient =>
          ChatMessages.applicationChatMessageRecipientJsonFormat.writes( a )
        case g:GameChatMessageRecipient =>
          ChatMessages.gameChatMessageRecipientJsonFormat.writes( g )
        case u:UserListChatMessageRecipient =>
          ChatMessages.userListChatMessageRecipient.writes( u )
        case _ =>
          JsString( "unknown" )
      }
    ),
    "sender" -> sender
  )


}

abstract class ChatMessageRecipient

case class ApplicationChatMessageRecipient( applicationGid:String ) extends ChatMessageRecipient
case class GameChatMessageRecipient( gameId:String ) extends ChatMessageRecipient
case class UserListChatMessageRecipient( usernames:Seq[String] ) extends ChatMessageRecipient

object ChatMessages {

  implicit val applicationChatMessageRecipientJsonFormat = Json.format[ApplicationChatMessageRecipient]
  implicit val gameChatMessageRecipientJsonFormat = Json.format[GameChatMessageRecipient]
  implicit val userListChatMessageRecipient = Json.format[UserListChatMessageRecipient]

  def fromGeneralRequest( g:GeneralRequest, senderUsername:String ):Option[ChatMessage] = {

    val recipientData = g.data \ "recipient"

    val recipientJsReader =
      applicationChatMessageRecipientJsonFormat.reads( recipientData ) orElse
      gameChatMessageRecipientJsonFormat.reads( recipientData ) orElse
      userListChatMessageRecipient.reads( recipientData )

    recipientJsReader.asOpt.map { recipientObj =>
      val message = ( g.data \ "message" ).asOpt[String]

      ChatMessage( g.event, g.sessionId, g.applicationId, g.gameId, g.date, g.data, message.getOrElse(""), recipientObj, senderUsername )
    }

  }
}