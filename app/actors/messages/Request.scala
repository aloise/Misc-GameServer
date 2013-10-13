package actors.messages

import java.util.Date
import akka.actor.ActorRef
import play.api.libs.json._

/**
 * User: aloise
 * Date: 10/12/13
 * Time: 10:43 PM
 */


case class Session(id:String)

case class Request(
  event:String, // event name
  session: Session, // socket-io provided session id
  applicationId:Option[Int], // target application id
  gameId:Option[Int], // target game id
  userId:Option[Int], // source user id
  namespace: String, // event namespace
  date:Date,
  data:JsValue // event data
)

object Session {
  implicit val json = Json.format[Session]
}

object Request {
  implicit val json = Json.format[Request]
}
