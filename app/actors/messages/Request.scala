package actors.messages

import java.util.{UUID, Date}
import akka.actor.ActorRef
import play.api.libs.json._
import actors.messages.UserSession.SessionId

/**
 * User: aloise
 * Date: 10/12/13
 * Time: 10:43 PM
 */

object UserSession {
  type SessionId = String

  def random:SessionId = UUID.randomUUID().toString
}

case class UserSession(sessionId:SessionId, user:Option[models.User], userActor:ActorRef)


abstract class Request{
  def event:String // event name
  def sessionId: SessionId // socket-io provided session id
  def applicationId:Option[String] // target application id
  def gameId:Option[Int] // target game id
  def date:Date
  def data:JsValue // event data
}

case class GeneralRequest (
   override val event:String, // event name
   override val sessionId: SessionId,
   override val applicationId:Option[String], // target application id
   override val gameId:Option[Int], // target game id
   override val date:Date,
   override val data:JsValue // event data
) extends Request
