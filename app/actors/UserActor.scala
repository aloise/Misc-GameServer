package actors

import akka.actor.{ActorRef, Actor}
import play.api.libs.json.JsValue
import play.api.libs.iteratee.{Input, Concurrent}
import actors.messages.{SingleRecipient, Response}
import play.api.libs.concurrent.Execution.Implicits._

/**
 * User: aloise
 * Date: 11/9/13
 * Time: 11:16 PM
 */
class UserActor(channel:Concurrent.Channel[JsValue], application:ActorRef ) extends Actor {

  var game:Option[ActorRef] = None

  def receive  = {

    case s:actors.messages.Response => channel.push( s.toJson )

  }


  override def postStop() {
    channel.push(Input.EOF)
  }

}
