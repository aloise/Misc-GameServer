package actors


import akka.actor._
import scala.collection.mutable
import actors.messages.UserSession._
import actors.messages._
import models.ApplicationProfile
import play.api.libs.json.Json
import scala.concurrent.Future
import reactivemongo.bson.BSONObjectID
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.json.BSONFormats._
import scala.util._

/**
 * User: aloise
 * Date: 9/4/13
 * Time: 11:08 PM
 */
class Game(application:ActorRef, game:models.Game) extends Actor {

  import models.GameProfiles.{format => f0}
  import models.Games.{ format => f1 }

  protected var users = Map[SessionId, (UserSession, models.ApplicationProfile, models.GameProfile ) ]()


  def receive = {

    case Game.UserJoin( userSession, appProfile ) =>

      // TODO - Implement a check. We may decline a user join attempt

      val gameProfileF = getGameProfileForUser(appProfile)

      gameProfileF onComplete {
        case Success( gameProfile ) =>
          users.synchronized{
            users = users + ( userSession.sessionId -> ( userSession, appProfile, gameProfile ) )
          }

          // userSession.userActor ! Game.UserJoinedSuccessfully( userSession.sessionId, game, gameProfile )
          val jsonData = Json.obj(
            "game" -> game,
            "gameProfile" -> gameProfile
          )

          userSession.userActor ! new Response( Game.Message.gameJoin, SingleRecipient( userSession.sessionId ), jsonData )


        case Failure( t ) =>
          userSession.userActor ! ErrorResponse( Game.Message.gameJoin, userSession.sessionId, t.getMessage )
      }


    case c@ChatMessage( _,_, _, _, _, _, _, recipient, _ ) =>

      recipient match {

          case GameChatMessageRecipient( gameId ) if game._id == BSONObjectID( gameId ) =>
            users.foreach{ case ( _, ( userSession, _, _ ) ) =>
              userSession.userActor ! c
            }

          case _ =>
            // not a valid point

      }

//      users = users + ( userSession.sessionId -> userSession )

    case r:messages.Response =>
//      application ! r
  }


  def getGameProfileForUser(appProfile: ApplicationProfile): Future[models.GameProfile] =
    models.Games.collection.
      find( Json.obj( "gameProfiles.applicationProfileId" -> appProfile._id ) ).
      one[models.Game].
      flatMap{
        case Some(gameDb) =>
          gameDb.gameProfiles.find( _.applicationProfileId == appProfile._id ).
            fold[Future[models.GameProfile]]{
              Future.failed(new Game.GameProfileCreateFailed("inconsistent_db_data"))
            } { gameProfile =>
              Future.successful( gameProfile )
            }

        case None =>
          val newGameProfile = models.GameProfile( BSONObjectID.generate, appProfile._id, appProfile.userId )

          models.Games.collection.update( Json.obj("_id" -> game._id ), Json.obj( "$push" -> Json.obj( "gameProfiles" -> newGameProfile ) )  )

          Future.successful( newGameProfile )

      }

   /*{
    models.GameProfiles.
      collection.
      find(Json.obj("applicationProfileId" -> appProfile._id)).
      one[models.GameProfile].
      flatMap {
        case Some(gameProfile) =>
          Future.successful(gameProfile)
        case None =>

          val newGameProfile = models.GameProfile( BSONObjectID.generate, appProfile._id, appProfile.userId )

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
  }*/


}


object Game {

  import models.Games.{ format => f0 }
  import models.GameProfiles.{ format => f1 }

  object Message {
    val gameJoin = "game-join"
  }


  case class UserJoin( userSession:UserSession, applicationProfile: models.ApplicationProfile )

  // it's sent to user
  case class UserJoinedSuccessfully(sessionId:SessionId, game:models.Game, gameProfile:models.GameProfile) extends
    actors.messages.Response("game.user_joined_successfully", SingleRecipient(sessionId), Json.toJson( Json.obj( "game" -> game, "gameProfile" -> gameProfile ) ) )

  class GameProfileCreateFailed(msg:String) extends Throwable
}