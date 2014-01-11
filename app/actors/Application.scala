package actors

import akka.actor.{PoisonPill, Props, ActorRef, Actor}
import messages._
import actors.messages.UserSession.SessionId
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.JsValue
import actors.Application.{GameCreate, UserJoinedSuccessfully, UserJoin}
import models.Games.format
import reactivemongo.bson._
import play.api.libs.concurrent.Execution.Implicits._


// Gateway is the parent of the Application
class Application( application:models.Application) extends Actor {




  var games = Map[Int, ActorRef]()
  var users = Map[SessionId, UserSession]()

  private def userActor(channel:Concurrent.Channel[JsValue]) = Props(classOf[UserActor], channel, self)

  def receive = {


    case Application.UserJoin( sessionId, dbUser, channel ) =>

      // construct the User and pass back the user
      val actor = context.actorOf( userActor(channel) )
      val userSession = UserSession( sessionId, dbUser, actor, self  )

      users = users + ( sessionId -> userSession )

      sender ! UserJoinedSuccessfully( userSession )


    case u@Gateway.UserDisconnected(sessionId) =>
      users.get(sessionId).foreach{ user =>

        // notify games
        games.foreach( _._2 ! u )

        user.userActor ! PoisonPill
        // remove him from the list
        users = users - sessionId
      }

    case GameCreate( data ) =>
      import models.Games.format

      val newGameData = data.copy( id = None, applicationId = application.id.get )
      models.Games.insert( newGameData ).map { lastError =>

      }

  }


  def getActorProps( gameProfile:models.Game ) =
    Props(classOf[actors.Game], context.self, gameProfile)

}

object Application {
  // it's sent from the Gateway to the Application
  case class UserJoin( id:SessionId, dbUser:models.User, channel: Concurrent.Channel[JsValue]) extends InternalMessage

  case class UserJoinedSuccessfully( userSession:UserSession ) extends InternalMessage

  case class GameCreate( data:models.Game ) extends InternalMessage
  case class GameCreatedSuccessfully( gameId:Int, game:ActorRef ) extends InternalMessage
  case class GameCreateFailed( reason:String )


}
