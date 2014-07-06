package actors

import akka.actor._
import scala.collection.mutable
import actors.messages.UserSession._
import actors.messages.{SingleRecipient, UserSession}
import models.ApplicationProfile
import play.api.libs.json.Json
import scala.concurrent.Future
import reactivemongo.bson.BSONObjectID
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.json.BSONFormats._

/**
 * User: aloise
 * Date: 9/4/13
 * Time: 11:08 PM
 */
class Game(application:ActorRef, game:models.Game) extends Actor {

  import models.GameProfiles.{format => f0}

  protected var users = mutable.Map[SessionId, (UserSession, models.ApplicationProfile, models.GameProfile ) ]()

  def receive = {

    case Game.UserJoin( userSession, appProfile ) =>
      val profile = getGameProfileForUser(appProfile)

      profile.foreach{ gameProfile =>
        users.synchronized{
          users = users + ( userSession.sessionId -> ( userSession, appProfile, gameProfile ) )
        }

        userSession.userActor ! Game.UserJoinedSuccessfully( userSession.sessionId, game, gameProfile )
      }

//      users = users + ( userSession.sessionId -> userSession )

    case r:messages.Response =>
//      application ! r
  }


  def getGameProfileForUser(appProfile: ApplicationProfile): Future[models.GameProfile] = {
    models.GameProfiles.
      collection.
      find(Json.obj("applicationProfileId" -> appProfile._id)).
      one[models.GameProfile].
      flatMap {
      case Some(gameProfile) =>
        Future.successful(gameProfile)
      case None =>

        val newGameProfile = models.GameProfile( BSONObjectID.generate, appProfile._id )

        models.GameProfiles.
          insert(newGameProfile).
          map {
          lastError =>
            if (lastError.ok) {
              newGameProfile
            } else {
              throw new Game.GameProfileCreateFailed(lastError.errMsg.getOrElse("game_profile_create_failed"))
            }

        }
    }
  }
}


object Game {

  import models.Games.{ format => f0 }
  import models.GameProfiles.{ format => f1 }

  case class UserJoin( userSession:UserSession, applicationProfile: models.ApplicationProfile )

  // it's sent to user
  case class UserJoinedSuccessfully(sessionId:SessionId, game:models.Game, gameProfile:models.GameProfile) extends
    actors.messages.Response("game.user_joined_successfully", SingleRecipient(sessionId), Json.toJson( Json.obj( "game" -> game, "gameProfile" -> gameProfile ) ) )

  class GameProfileCreateFailed(msg:String) extends Throwable
}