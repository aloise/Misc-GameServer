package actors.messages

import java.util.{UUID, Date}
import akka.actor.ActorRef
import play.api.libs.json._
import actors.messages.UserSession.Session

/**
 * User: aloise
 * Date: 10/12/13
 * Time: 10:43 PM
 */

object UserSession {
  type Session = String

  def random:Session = UUID.randomUUID().toString
}

case class UserSession(sessionId:Session, user:Option[models.User], sender:ActorRef, receiver:ActorRef)


abstract class Request{
  def event:String // event name
  def sessionId: Session // socket-io provided session id
  def applicationId:Option[Int] // target application id
  def gameId:Option[Int] // target game id
  def date:Date
  def data:JsValue // event data
}

case class GeneralRequest (
   override val event:String, // event name
   override val sessionId: Session,
   override val applicationId:Option[Int], // target application id
   override val gameId:Option[Int], // target game id
   override val date:Date,
   override val data:JsValue // event data
) extends Request
