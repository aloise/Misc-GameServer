package actors

import akka.actor.{ActorRef, Actor}
import play.api.libs.json.JsValue
import play.api.libs.iteratee.{Input, Concurrent}
import actors.messages.{SingleRecipient, Response, UserConnectAccepted, UserSenderActorInit}
import play.api.libs.concurrent.Execution.Implicits._

/**
 * User: aloise
 * Date: 11/9/13
 * Time: 11:16 PM
 */
class WebSocketSender extends Actor {



  var channel: Option[Concurrent.Channel[JsValue]] = None
  var application: Option[ActorRef] = None
  var game:Option[ActorRef] = None

  def receive  = {
    case UserSenderActorInit(id, receiverActor) => {
      val me = self
      val enumerator = Concurrent.unicast[JsValue]{ c =>
        channel = Some(c)

//        me ! Connected(id)
      }
      sender ! UserConnectAccepted(id, receiverActor, enumerator)
    }

//    case JoinApplication =>

//    case JoinGame =>



    case s:actors.messages.Response => channel.foreach( _.push( s.toJson ) )


//    case Connected(id) => context.parent ! Broadcast[Payload](id, msgFormatter.connected(id))

//    case Disconnected(id) => context.parent ! Broadcast(id, msgFormatter.disconnected(id))

  }


  override def postStop() {
    channel.foreach(_.push(Input.EOF))
  }

}

class WebSocketReceiver extends Actor {

  def receive = {
    // TODO implement a real response
    case r: actors.messages.Request => context.parent ! Response( "echo", SingleRecipient(r.sessionId), r.data )
  }
}