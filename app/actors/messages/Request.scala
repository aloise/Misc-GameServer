package actors.messages

import java.util.Date
import akka.actor.ActorRef
import play.api.libs.json._

/**
 * User: aloise
 * Date: 10/12/13
 * Time: 10:43 PM
 */


case class Session(id:String, userId:Option[Int])

abstract class Request{
  def event:String // event name
  def session: Session // socket-io provided session id
  def applicationId:Option[Int] // target application id
  def gameId:Option[Int] // target game id
  def namespace: String // event namespace
  def date:Date
  def data:JsValue // event data
}

case class GeneralRequest(
   override val event:String, // event name
   override val session: Session, // socket-io provided session id
   override val applicationId:Option[Int], // target application id
   override val gameId:Option[Int], // target game id
   override val namespace: String, // event namespace
   override val date:Date,
   override val data:JsValue // event data
) extends Request

// a very specific case
case class LoginRequest(
  override val event:String, // event name
  override val session: Session, // socket-io provided session id
  override val applicationId:Option[Int], // target application id
  override val gameId:Option[Int], // target game id
  override val namespace: String, // event namespace
  override val date:Date,
  override val data:JsValue, // event data extends Request(
  user:models.User
) extends Request

