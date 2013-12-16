package actors

import akka.actor.{ActorRef, Actor}
import messages._
import actors.messages.UserSession.SessionId


class Application(gateway:ActorRef, application:models.Application) extends Actor {

  var games = Map[Int, ActorRef]()
  var users = Map[SessionId, UserSession]()

  def receive = {


    case UserLoggedIn( userSession ) =>
      users = users + ( userSession.sessionId -> userSession )

    case u@UserDisconnected(sessionId) =>
      users.get(sessionId).foreach{ user =>

        // notify games
        games.foreach( _._2 ! u )

        // remove him from the list
        users = users - sessionId
      }

  }


}
