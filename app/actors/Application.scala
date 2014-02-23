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

  var games = Map[BSONObjectID, ( models.Game, ActorRef )]()
  var users = Map[SessionId, (UserSession, ApplicationProfile)]()

  private def userActor(channel:Concurrent.Channel[JsValue]) = Props(classOf[UserActor], channel, self)

  def receive = {

    case Application.UserJoin( sessionId, dbUser, channel ) =>

      // construct the User and pass back the user
      val appActor = self

      // get the user's application profile or create one
      val userAppData:Future[ ApplicationProfile ] = getApplicationProfileForUser(dbUser)

      // pipe the response back to the sender
      val userJoinedMsg = userAppData.map { case appProfile =>

        val actor = context.actorOf( userActor(channel), "User#" + dbUser.id.get )
        val userSession = UserSession( sessionId, dbUser, actor, appActor  )

        users.synchronized{
          users = users + ( sessionId -> ( userSession, appProfile) )
        }
        UserJoinedSuccessfully( userSession, appProfile )
      }

      userJoinedMsg pipeTo sender




    case u@Gateway.UserDisconnected(sessionId) =>
      users.get(sessionId).foreach{ user =>

        // notify games
        games.foreach{
          case (_,(_, actor)) => actor ! u
        }

        // kill the user actor
        user._1.userActor ! PoisonPill

        // remove him from the list
        users = users - sessionId
      }

    case GameCreate( data ) =>

      val cntx = context
      val newGameData = data.copy( id = Some(BSONObjectID.generate), applicationId = application.id.get )
      val responseMsg =
        models.Games.
          insert( newGameData ).
          map { case lastError =>
            if( lastError.ok ){

              val actor = cntx.actorOf( getGameActorProps(newGameData), "Game#"+newGameData.id.get )

              games.synchronized{
                games = games + ( newGameData.id.get -> ( newGameData, actor ) )
              }

              Application.GameCreatedSuccessfully(newGameData, actor)
            } else {
              Application.GameCreateFailed( lastError.errMsg.getOrElse("game_create_error") )
            }

          }
      responseMsg pipeTo sender

    // process an application-wide event - gameId is empty
    case actors.messages.GeneralRequest( event, sessionId, applicationId, None, date, data ) =>
      // process the request or send to the correponding game
      // none at the moment

    // pass the event to the corresponding game
    case r@actors.messages.GeneralRequest( _, sessionId, _, Some(gameId), _, _ )
      if games.contains( BSONObjectID( gameId )) && users.contains(sessionId) =>
        games(BSONObjectID( gameId ))._2 ! r



  }


  def getApplicationProfileForUser(dbUser: models.User): Future[ApplicationProfile] = {
    ApplicationProfiles.
      collection.
      find(Json.obj("userId" -> dbUser.id)).
      one[ApplicationProfile].
      flatMap {
      case Some(appProfile) =>
        Future.successful(appProfile)
      case None =>
        val newAppProfile = ApplicationProfiles.getForUser(application, dbUser)
        ApplicationProfiles.
          collection.
          insert(newAppProfile).
          map {
          lastError =>
          // TODO add an error check here
            newAppProfile
        }
    }
  }

  // TODO override this method for specific games
  def getGameActorProps( gameProfile:models.Game ) =
    Props(classOf[actors.Game], context.self, gameProfile)

}

object Application {
  // it's sent from the Gateway to the Application
  case class UserJoin( id:SessionId, dbUser:models.User, channel: Concurrent.Channel[JsValue]) extends InternalMessage
  case class UserJoinedSuccessfully( userSession:UserSession, applicationProfile: ApplicationProfile ) extends InternalMessage

  case class GameCreate( data:models.Game ) extends InternalMessage
  case class GameCreatedSuccessfully( game:models.Game, gameActor:ActorRef ) extends InternalMessage
  case class GameCreateFailed( reason:String )


}
