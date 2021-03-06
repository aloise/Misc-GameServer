package actors.messages

import java.util.Date

import actors.messages.UserSession.SessionId
import play.api.libs.json.{JsSuccess, JsString, Json, JsValue}
import models.Users.{ jsonFormat => userJsonFormat}
/**
 * User: aloise
 * Date: 06.07.14
 * Time: 19:08
 */
case class ChatMessage (
  override val event:String = ChatMessages.eventName, // event name
  override val sessionId: SessionId,
  override val applicationId:Option[String], // target application id
  override val gameId:Option[String], // target game id
  override val date:Date,
  override val data:JsValue, // event data

  message:String,
  recipient: ChatMessageRecipient,
  sender: Option[models.User] // sender username

) extends Request {

  def toJson:JsValue = Json.obj(
    "event" -> event,
    "data" -> Json.obj( "message" -> message ),
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
    "sender" -> Json.toJson( sender )
  )


}

abstract class ChatMessageRecipient

case class ApplicationChatMessageRecipient( applicationId:String ) extends ChatMessageRecipient
case class GameChatMessageRecipient( gameId:String ) extends ChatMessageRecipient
case class UserListChatMessageRecipient( usernames:Seq[String] ) extends ChatMessageRecipient

object ChatMessages {
  val eventName = "chat-message"


  implicit val applicationChatMessageRecipientJsonFormat = Json.format[ApplicationChatMessageRecipient]
  implicit val gameChatMessageRecipientJsonFormat = Json.format[GameChatMessageRecipient]
  implicit val userListChatMessageRecipient = Json.format[UserListChatMessageRecipient]

  def fromGeneralRequest( g:GeneralRequest, senderUser: Option[models.User] = None ):Option[ChatMessage] = {

    val recipientData = g.data \ "recipient"

    val recipientJsReader =
      applicationChatMessageRecipientJsonFormat.reads( recipientData ) orElse
      gameChatMessageRecipientJsonFormat.reads( recipientData ) orElse
      userListChatMessageRecipient.reads( recipientData )

    recipientJsReader.asOpt.map { recipientObj =>
      val message = ( g.data \ "message" ).asOpt[String]

      ChatMessage( g.event, g.sessionId, g.applicationId, g.gameId, g.date, g.data, message.getOrElse(""), recipientObj, senderUser )
    }

  }
}