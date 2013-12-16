package actors

import akka.actor.{PoisonPill, Props, ActorRef, Actor}
import messages._
import actors.messages.UserSession.SessionId
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.JsValue
import actors.Application.UserJoin


class Application(gateway:ActorRef, application:models.Application) extends Actor {

  var games = Map[Int, ActorRef]()
  var users = Map[SessionId, UserSession]()

  private def userActor() = Props(classOf[UserActor])

  def receive = {


    case UserJoin( sessionId, channel ) =>

      // construct the User and pass back the user

      users = users + ( sessionId -> userSession )

    case u@UserDisconnected(sessionId) =>
      users.get(sessionId).foreach{ user =>

        // notify games
        games.foreach( _._2 ! u )

        user.userActor ! PoisonPill
        // remove him from the list
        users = users - sessionId
      }

  }


}

object Application {
  // it's sent from the Gateway to the Application
  case class UserJoin( id:SessionId, channel: Concurrent.Channel[JsValue]) extends Message

}
