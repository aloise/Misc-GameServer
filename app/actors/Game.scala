package actors


import akka.actor._
import org.joda.time.DateTime
import scala.collection.immutable.ListMap
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
  import scala.collection.immutable.Queue
  import Game.GameMessage

  protected var users = ListMap[SessionId, (UserSession, models.ApplicationProfile, models.GameProfile ) ]()

  protected var status:String = game.status

  protected var creator:Option[UserSession] = None

  protected var messages:Queue[GameMessage] = Queue()


  def receive = {

    case Game.UserJoin( userSession, appProfile, isCreator ) =>
      gameUserJoin( userSession, appProfile, isCreator)

      if( isCreator ){
        creator = Some(userSession)
      }


    case c@ChatMessage( _,_, _, _, _, _, _, recipient, _ ) =>

      recipient match {
          case GameChatMessageRecipient( gameId ) if game._id == BSONObjectID( gameId ) =>
            users.foreach{ case ( _, ( userSession, _, _ ) ) =>
              userSession.userActor ! c
            }


            enqueueMessage( c, users.map{ case ( _, ( _, appProfile, _ ) ) =>  appProfile._id  } )

          case _ =>
            // not a valid point
      }


    case Game.UserLeave( user ) =>
      gameUserLeave( user )


    case Gateway.UserDisconnected( sessionId ) =>
      users.get( sessionId ).foreach { case ( u, _, _ ) =>
        self ! Game.UserLeave( u )
      }
      application ! Application.GameUserLeaved( game._id, sessionId )



    case actors.messages.GeneralRequest( Game.Message.gameStart, fromSessionId, applicationId, Some( gameId ), date, data ) if BSONObjectID( gameId ) == game._id =>
      if( status == models.Games.Status.Waiting ){

        status =  models.Games.Status.Active

        // TODO - update the DB, notify users etc
        application ! getGameDataMessage

      } else {
        val msg = "game_is_not_in_waiting_state"
        users.get(fromSessionId).foreach { case (userSession, _, _) =>
          userSession.userActor ! ErrorResponse( Game.Message.gameJoin, userSession.sessionId, msg )
        }
      }

    case actors.messages.GeneralRequest( event, fromSessionId, applicationId, Some( gameId ), date, data ) if BSONObjectID( gameId ) == game._id =>
      if( users.contains( fromSessionId ) ){
        // basically just broadcast it

        val msg = new Game.GameSpecificResponse( event, EmptyRecipient , data, Some( Json.obj( "user" -> users.get( fromSessionId ).map( _._1.user ) ) ) )

        users.values.
          filter( _._1.sessionId != fromSessionId ).
          foreach{ case ( userSession, _, _ ) =>
            userSession.userActor ! msg.copy( recipients = SingleRecipient( userSession.sessionId )  )
          }

          enqueueMessage( msg, users.map{ case ( _, ( _, appProfile, _ ) ) => appProfile._id  } )

      } else {
        // it's not allowed to send messages before the game join
      }


  }

  def enqueueMessage( message: Any, recipientApplicationProfileIds:Iterable[BSONObjectID] = Seq() ) = {
    messages = messages.enqueue( GameMessage( message, recipientApplicationProfileIds ) )
  }

  def gameUserLeave(user: UserSession):Unit = {
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

      // notify all users about the event
      users.values.foreach { case ( notifySession, _, _ ) =>
        notifySession.userActor ! Response( Game.Message.gameLeave, notifySession.sessionId, dataToNotify )
      }

      application ! Application.GameUserLeaved( game._id, user.sessionId )
      application ! getGameDataMessage

      users = users - user.sessionId

    }
  }


  def getGameDataMessage = {

    val creatorOpt = creator.flatMap( creatorId => users.get( creatorId.sessionId ) )

    val creatorGameProfileIdOpt = creatorOpt.map( _._3._id )

    Application.GameDataUpdated( game.copy( status = status, creatorGameProfileId = creatorGameProfileIdOpt ), creatorOpt, users )
  }

  def gameUserJoin(userSession: UserSession, appProfile: ApplicationProfile, isCreator: Boolean): Unit = {
    // TODO - Implement a check. We may decline a user join attempt excluding the creator

    status match {

      case models.Games.Status.Waiting =>

          val gameProfileF = getGameProfileForUser(appProfile)

          gameProfileF onComplete {
            case Success( gameProfile ) =>

              users = users + ( userSession.sessionId -> ( userSession, appProfile, gameProfile ) )

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



              application ! Application.GameUserJoined( game._id, userSession.sessionId )
              application ! getGameDataMessage

              // userSession.userActor ! Response( Game.Message.gameJoin, userSession.sessionId, jsonData )


            case Failure( t ) =>
              userSession.userActor ! ErrorResponse( Game.Message.gameJoin, userSession.sessionId, t.getMessage )
          }

      case _ =>
        val msg = "game_is_not_in_waiting_state"
        application ! Application.GameUserJoinFailed( game._id, userSession.sessionId, new Game.GameJoinException( msg ) )
        userSession.userActor ! ErrorResponse( Game.Message.gameJoin, userSession.sessionId, msg )
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

  class GameJoinException(m:String) extends Exception( m )

  case class GameSpecificResponse( override val event:String, override val recipients: Recipients, override val data:JsValue, fromData:Option[JsValue] = None, override val isSuccess:Boolean = true ) extends Response( event, recipients, data, isSuccess ) {

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

  case class GameMessage( message: Any, recipientApplicationProfileIds: Iterable[BSONObjectID] = Seq() )

  // it's sent to user
/*
  case class UserJoinedSuccessfully(sessionId:SessionId, game:models.Game, gameProfile:models.GameProfile) extends
    actors.messages.Response("game.user_joined_successfully", SingleRecipient(sessionId), Json.toJson( Json.obj( "game" -> game, "gameProfile" -> gameProfile ) ) )
*/

  class GameProfileCreateFailed(msg:String) extends Throwable
}