package actors

import akka.actor._
import scala.collection.mutable
import actors.messages.UserSession._
import actors.messages.UserSession
import models.ApplicationProfile

/**
 * User: aloise
 * Date: 9/4/13
 * Time: 11:08 PM
 */
class Game(application:ActorRef, game:models.Game) extends Actor {

  var users = mutable.Map[SessionId, (UserSession, models.ApplicationProfile, models.GameProfile ) ]()

  def receive = {

    case Game.UserJoin( userSession, appProfile ) =>
//        users = users + ( userSession.sessionId -> userSession )

    case r:messages.Response =>
//      application ! r
  }

}


object Game {

  case class UserJoin( userSession:UserSession, applicationProfile: models.ApplicationProfile )
}