package actors


import akka.actor._
import org.joda.time.DateTime
import scala.collection.mutable
import actors.messages.UserSession._
import actors.messages._
import models.ApplicationProfile
import play.api.libs.json.{JsObject, JsValue, JsString, Json}
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
  import models.Users.{ jsonFormat => f2 }
  import models.ApplicationProfiles.{ jsonFormat => f3 }

  protected var users = Map[SessionId, (UserSession, models.ApplicationProfile, models.GameProfile ) ]()

  private var status:String = game.status

  def receive = {

    case Game.UserJoin( userSession, appProfile, isCreator ) =>

      // TODO - Implement a check. We may decline a user join attempt exluding the creator

      val gameProfileF = getGameProfileForUser(appProfile)

      gameProfileF onComplete {
        case Success( gameProfile ) =>

          // userSession.userActor ! Game.UserJoinedSuccessfully( userSession.sessionId, game, gameProfile )
          val jsonData = Json.obj(
            "game" -> game,
            "gameProfile" -> gameProfile,
            "users" -> users.values.map {
              case ( sess2, appProfile2, gameProfile2 ) =>
                Json.obj(
                  "user" -> sess2.user,
                  "applicationProfile" -> appProfile2,
                  "gameProfile" -> gameProfile2
                )
            }
          )

          // notify all existing users
          users.values.foreach { case ( notifySession, _, _ ) =>
            notifySession.userActor ! Response( Game.Message.gameJoin, notifySession.sessionId, jsonData )
          }

          users = users + ( userSession.sessionId -> ( userSession, appProfile, gameProfile ) )

          application ! Application.GameUserJoined( game._id, userSession.sessionId )

          userSession.userActor ! Response( Game.Message.gameJoin, userSession.sessionId, jsonData )


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


    case Game.UserLeave( user ) =>
      users.get( user.sessionId ).foreach{ case ( session, appProfile, gameProfile ) =>

        val closedGameProfile = gameProfile.copy(
          status = models.GameProfiles.Status.completed,
          completed = Some( new DateTime() )
        )

        models.Games.collection.update( Json.obj("_id" -> game._id ), Json.obj(
          "$set" -> Json.obj(
            "gameProfiles.$" -> closedGameProfile
          )
        ) )

        val dataToNotify = Json.obj(
          "user" -> session.user,
          "applicationProfile" -> appProfile,
          "gameProfile" -> gameProfile
        )

        // notufy all users about the event
        users.values.foreach { case ( notifySession, _, _ ) =>
          notifySession.userActor ! Response( Game.Message.gameLeave, notifySession.sessionId, dataToNotify )
        }

        application ! Application.GameUserLeaved( game._id, user.sessionId )

        users = users - user.sessionId

      }

    case Gateway.UserDisconnected( sessionId ) =>
      users.get( sessionId ).foreach { case ( u, _, _ ) =>
        self ! Game.UserLeave( u )
      }

      application ! Application.GameUserLeaved( game._id, sessionId )


    case actors.messages.GeneralRequest( event, fromSessionId, applicationId, Some( gameId ), date, data ) if BSONObjectID( gameId ) == game._id =>
      // basically just broadcast it
      users.values.
        filter( _._1.sessionId != fromSessionId ).
        foreach{ case ( userSession, _, _ ) =>
          userSession.userActor ! new Game.GameSpecificResponse( event, SingleRecipient( userSession.sessionId ), data, Some( Json.obj( "user" -> userSession.user ) ) )
        }

  }


  def getGameProfileForUser(appProfile: ApplicationProfile): Future[models.GameProfile] =
    models.Games.collection.
      find( Json.obj(
        "gameProfiles" -> Json.obj(
            "$elemMatch" -> Json.obj(
              "applicationProfileId" -> appProfile._id,
              "status" -> Json.obj( "$ne" -> models.GameProfiles.Status.completed )
            )
          )
      ) ).
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

          models.Games.collection.update( Json.obj("_id" -> game._id ), Json.obj( "$push" -> Json.obj( "gameProfiles" -> newGameProfile ) )  ).
            map { lastError =>
              if (lastError.ok) {
                newGameProfile
              } else {
                throw new Game.GameProfileCreateFailed(lastError.errMsg.getOrElse("db_game_profile_create_failed"))
              }

           }

      }


}


object Game {

  import models.Games.{ format => f0 }
  import models.GameProfiles.{ format => f1 }

  object Message {
    val gameJoin = "game-join"
    val gameLeave = "game-leave"
    val gameStart = "game-start"
  }

  class GameSpecificResponse( event:String, recipients: Recipients, data:JsValue, fromData:Option[JsValue] = None, isSuccess:Boolean = true ) extends Response( event, recipients, data, isSuccess ) {

    override def toJson:JsValue = ( super.toJson match {
      case obj@JsObject( fields ) =>
        obj
      case _ =>
        Json.obj()
    } ) ++ fromData.fold( Json.obj() ){ from =>
      Json.obj( "from" -> from )
    }
  }


  case class UserJoin( userSession:UserSession, applicationProfile: models.ApplicationProfile, isCreator:Boolean = false )
  case class UserLeave( userSession:UserSession )

  // it's sent to user
/*
  case class UserJoinedSuccessfully(sessionId:SessionId, game:models.Game, gameProfile:models.GameProfile) extends
    actors.messages.Response("game.user_joined_successfully", SingleRecipient(sessionId), Json.toJson( Json.obj( "game" -> game, "gameProfile" -> gameProfile ) ) )
*/

  class GameProfileCreateFailed(msg:String) extends Throwable
}