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
import play.api.libs.concurrent.Execution.Implicits._


// Gateway is the parent of the Application
class Application( application:models.Application) extends Actor {

  import models.Games.{ format => f0 }
  import models.ApplicationProfiles.{ jsonFormat => f1 }

  protected var games = Map[BSONObjectID, ( models.Game, ActorRef )]()
  protected var users = Map[SessionId, (UserSession, ApplicationProfile)]()

  private def getUserActorProps(channel:Concurrent.Channel[JsValue], applicationActor:ActorRef ) =
    Props(classOf[UserActor], channel, applicationActor)

  def receive = {

    case Application.UserJoin( sessionId, dbUser, channel ) =>

      val cntx = context

      // construct the User and pass back the user
      val applicationActor = self

      // get the user's application profile or create one
      val userAppData:Future[ ApplicationProfile ] = getApplicationProfileForUser(dbUser)

      // pipe the response back to the sender
      val userJoinedMsg = userAppData.map { appProfile =>

//        println(appProfile)
        val actorName = "User_" + dbUser._id.stringify + "_" + sessionId

        val actor = cntx.actorOf( getUserActorProps(channel, applicationActor), actorName )

        val userSession = UserSession( sessionId, dbUser, actor, applicationActor )

        users.synchronized{
          users = users + ( sessionId -> ( userSession, appProfile) )
        }

        userSession.userActor ! Application.UserJoinedSuccessfullyResponse( userSession.sessionId, application, appProfile )

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

    case GameCreate( data, creatorSessionId ) =>

      val cntx = context
      val appActor = context.self
      val creatorUserSession = creatorSessionId.flatMap( users.get )

      val newGameData = data.copy(
        _id = BSONObjectID.generate,
        applicationId = application._id,
        creatorGameProfileId = creatorUserSession.map( _._1.user._id )
      )

      val responseMsg =
        models.Games.
          insert( newGameData ).
          map { case lastError =>
            if( lastError.ok ){

              val gameActor = cntx.actorOf( getGameActorProps(newGameData, appActor), "Game_"+newGameData._id.stringify )

              // auto-join the game creator
              creatorUserSession.foreach{ case( userSession, userAppProfile) =>
                gameActor ! Game.UserJoin( userSession, userAppProfile )
              }

              games.synchronized{
                games = games + ( newGameData._id -> ( newGameData, gameActor ) )
              }

              Application.GameCreatedSuccessfully(newGameData, gameActor)

            } else {
              Application.GameCreateFailed( lastError.errMsg.getOrElse("game_create_error") )
            }

          }

      responseMsg pipeTo sender

    case Application.GameFinished( gameId ) =>

      games.get(gameId).foreach{ case (game, gameActor) =>

        gameActor ! PoisonPill

        games = games - gameId
      }


    // process an application-wide event - gameId is empty
    case actors.messages.GeneralRequest( event, sessionId, applicationId, None, date, data ) =>
      // process the request or send to the corresponding game
      // TODO - implement - none at the moment

    // pass the event to the corresponding game
    case r@actors.messages.GeneralRequest( _, sessionId, _, Some(gameId), _, _ )
      if games.contains( BSONObjectID( gameId )) && users.contains(sessionId) =>
        games( BSONObjectID( gameId ))._2 ! r

  }


  def getApplicationProfileForUser(dbUser: models.User): Future[ApplicationProfile] = {
    ApplicationProfiles.
      collection.
      find(Json.obj("userId" -> dbUser._id)).
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
              if( lastError.ok )
                newAppProfile
              else
                throw new Application.ApplicationProfileCreateFailed(lastError.errMsg.getOrElse("application_profile_create_failed"))
        }
    }
  }

  // TODO override this method for specific games
  def getGameActorProps( gameProfile:models.Game, app:ActorRef = context.self ) =
    Props(classOf[actors.Game], app, gameProfile)

}

object Application {

  import models.ApplicationProfiles.{ jsonFormat => f0 }
  import models.Applications.{ format => f1 }

  // it's sent from the Gateway to the Application
  case class UserJoin( id:SessionId, dbUser:models.User, channel: Concurrent.Channel[JsValue]) extends InternalMessage
  case class UserJoinedSuccessfully( userSession:UserSession, applicationProfile: ApplicationProfile ) extends InternalMessage

  case class GameCreate( data:models.Game, creator:Option[SessionId] = None ) extends InternalMessage
  case class GameCreatedSuccessfully( game:models.Game, gameActor:ActorRef ) extends InternalMessage
  case class GameCreateFailed( reason:String ) extends InternalMessage

  case class GameFinished( gameId: BSONObjectID ) extends InternalMessage

  class ApplicationProfileCreateFailed(msg:String) extends Throwable

  case class UserJoinedSuccessfullyResponse(sessionId:SessionId, application: models.Application, applicationProfile: models.ApplicationProfile) extends
    actors.messages.Response("application.user_joined_successfully", SingleRecipient(sessionId), Json.toJson( Json.obj( "application" -> application, "applicationProfile" -> applicationProfile ) ) )


}
