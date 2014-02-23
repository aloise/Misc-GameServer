package actors

import akka.actor.{PoisonPill, Props, ActorRef, Actor}
import messages._
import actors.messages.UserSession.SessionId
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.{Json, JsValue}
import actors.Application.{GameCreate, UserJoinedSuccessfully, UserJoin}
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.bson._
import play.api.libs.concurrent.Execution.Implicits._
import models.{ApplicationProfiles, ApplicationProfile}
import scala.concurrent.Future
import akka.pattern._


// Gateway is the parent of the Application
class Application( application:models.Application) extends Actor {

  import models.Games.{ format => f0 }
  import models.ApplicationProfiles.{ jsonFormat => f1 }

  var games = Map[Int, ActorRef]()
  var users = Map[SessionId, (UserSession, ApplicationProfile)]()

  private def userActor(channel:Concurrent.Channel[JsValue]) = Props(classOf[UserActor], channel, self)

  def receive = {

    case Application.UserJoin( sessionId, dbUser, channel ) =>

      // construct the User and pass back the user
      val actor = context.actorOf( userActor(channel) )
      val userSession = UserSession( sessionId, dbUser, actor, self  )

      // get the user's application profile or create one
      val userAppData:Future[ ApplicationProfile ] =
        ApplicationProfiles.
          collection.
          find( Json.obj("userId" -> dbUser.id ) ).
          one[ApplicationProfile].
          flatMap{
            case Some(appProfile) =>
              Future.successful( appProfile )
            case None =>
              val newAppProfile = ApplicationProfiles.getForUser(application, dbUser)
              ApplicationProfiles.
                collection.
                insert( newAppProfile ).
                map{ lastError =>
                  // TODO add an error check here
                  newAppProfile
                }
          }

      // pipe the response back to the sender
      val userJoinedMsg = userAppData.map { case appProfile =>
        users.synchronized{
          users = users + ( sessionId -> ( userSession, appProfile) )
        }
        UserJoinedSuccessfully( userSession, appProfile )
      }

      userJoinedMsg pipeTo sender




    case u@Gateway.UserDisconnected(sessionId) =>
      users.get(sessionId).foreach{ user =>

        // notify games
        games.foreach( _._2 ! u )

        user._1.userActor ! PoisonPill
        // remove him from the list
        users = users - sessionId
      }

    case GameCreate( data ) =>
      val realSender = sender
      val newGameData = data.copy( id = Some(BSONObjectID.generate), applicationId = application.id.get )
      models.Games.insert( newGameData ).onSuccess { case lastError =>
        realSender
      }

    // process an application-wide event - gameId is empty
    case actors.messages.GeneralRequest( event, sessionId, applicationId, None, date, data ) =>
      // process the request or send to the correponding game
      // none at the moment

    // pass the event to the corresponding game
    case r@actors.messages.GeneralRequest( _, sessionId, _, Some(gameId), _, _ ) if games.contains(gameId) && users.contains(sessionId) =>
      games(gameId) ! r



  }


  def getActorProps( gameProfile:models.Game ) =
    Props(classOf[actors.Game], context.self, gameProfile)

}

object Application {
  // it's sent from the Gateway to the Application
  case class UserJoin( id:SessionId, dbUser:models.User, channel: Concurrent.Channel[JsValue]) extends InternalMessage
  case class UserJoinedSuccessfully( userSession:UserSession, applicationProfile: ApplicationProfile ) extends InternalMessage

  case class GameCreate( data:models.Game ) extends InternalMessage
  case class GameCreatedSuccessfully( gameId:Int, game:ActorRef ) extends InternalMessage
  case class GameCreateFailed( reason:String )


}
