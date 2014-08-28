package actors

import akka.actor.{PoisonPill, Props, ActorRef, Actor}
import actors.messages._
import actors.messages.UserSession.SessionId
import play.api.libs.iteratee.Concurrent
import play.api.libs.json._
import actors.Application.{GameCreate, UserJoinedSuccessfully, UserJoin}
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.bson._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import models.{GameProfile, ApplicationProfiles, ApplicationProfile}
import scala.collection.immutable.Iterable
import scala.concurrent.Future
import akka.pattern._
import play.api.libs.concurrent.Execution.Implicits._


// Gateway is the parent of the Application
class Application( application:models.Application) extends Actor {

  import models.Users.{ jsonFormat => userJsonFormat }
  import models.Games.{ format => gameJsonFormat }
  import models.GameProfiles.{ format => f2 }
  import models.ApplicationProfiles.{ jsonFormat => f1 }

  protected var games = Map[BSONObjectID, ( models.Game, ActorRef )]()  // Game._id
  protected var users = Map[SessionId, (UserSession, ApplicationProfile)]()
  protected var gameUsers = Map[ BSONObjectID, Set[SessionId] ]() // a mapping from Game._id -> User.SessionId

  private def getUserActorProps(channel:Concurrent.Channel[JsValue], applicationActor:ActorRef, sessionId:SessionId, dbUser:models.User, appProfile:models.ApplicationProfile ) =
    Props(classOf[UserActor], channel, applicationActor, sessionId, dbUser, appProfile)

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

        val actor = cntx.actorOf( getUserActorProps(channel, applicationActor, sessionId, dbUser, appProfile), actorName )

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

    case GameCreate( rawGameData, creatorSessionId, autojoinCreator ) =>

      val cntx = context
      val appActor = context.self
      val creatorUserSession = creatorSessionId.flatMap( users.get )

      val newGameData = rawGameData.copy(
        _id = BSONObjectID.generate,
        applicationId = application._id
//        creatorGameProfileId = creatorUserSession.map( _._1.user._id )
      )

      val responseMsg =
        models.Games.
          insert( newGameData ).
          map { case lastError =>
            if( lastError.ok ){

              val gameActor = cntx.actorOf( getGameActorProps(newGameData, appActor), "Game_"+newGameData._id.stringify )

              games = games + ( newGameData._id -> ( newGameData, gameActor ) )

              if( autojoinCreator ) {

                // auto-join the game creator
                creatorUserSession.foreach { case (userSession, userAppProfile) =>
                  gameUsers = gameUsers + ( newGameData._id -> Set( userSession.sessionId ) )
                  gameActor ! Game.UserJoin(userSession, userAppProfile)

                }
              }

              val responseJson = Json.obj(
                "game" -> newGameData,
                "users" -> Json.arr()

              )

              val creatorResponseData = creatorUserSession.fold( Json.obj() ){ case (userSession, userAppProfile) =>
                Json.obj(
                  "creator" -> Json.obj(
                    "user" -> userSession.user,
                    "applicationProfile" -> userAppProfile
                  )
                )
              }

              // send the game create action back
              creatorUserSession.foreach { case (userSession, userAppProfile) =>
                userSession.userActor ! Response( Application.Message.gameCreate, userSession.sessionId, responseJson )
              }

              users.values.foreach{ case ( userSession, _ ) =>

                val alreadyJoined = gameUsers.values.exists( _.contains( userSession.sessionId ) )
                if( !alreadyJoined ) {
                  userSession.userActor ! Response( Application.Message.gameNew, userSession.sessionId, responseJson ++ creatorResponseData )
                }
              }


              Application.GameCreatedSuccessfully(newGameData, creatorSessionId, gameActor)

            } else {
              Application.GameCreateFailed( lastError.errMsg.getOrElse("game_create_error") )
            }

          }

      responseMsg pipeTo sender



    case Application.GameDataUpdated( game, creator, gameUserMap ) =>
      games.get(game._id).foreach{ case (oldGameData, actor ) =>
        // TODO - we may send some game status updates here
        games = games.updated( game._id, (game, actor) )
      }

      broadcastGameDataUpdate( game, creator, gameUserMap )





    case Application.GameFinished( gameId ) =>

      games.get(gameId).foreach{ case (game, gameActor) =>

        gameActor ! PoisonPill

        games = games - gameId

        gameUsers = gameUsers - gameId
      }


    // external game create request
    case r@GeneralRequest( Application.Message.gameCreate, sessionId, _, _, _, data ) =>

      users.get( sessionId ).fold {
        // user was not found

      } { case ( userSession, appUserProfile ) =>

        data.validate( Application.Validators.gameCreate ).fold(
        invalid => {
          userSession.userActor ! actors.messages.ErrorResponse ( Application.Message.gameCreate, sessionId, "invalid_format"  )

        },
        { case ( gameType, speed, karmaRestrict, ratingRestrict, playersMaxCount, welcomeMessage, dataOpt ) =>

          val userGameProfile = models.GameProfile(
            applicationProfileId = appUserProfile._id,
            status = models.GameProfiles.Status.inProgress,
            userId = userSession.user._id
          )

          // TODO - perform some checks
          val gameData = models.Game(
            applicationId = application._id,
            speed = speed,
            `type` = gameType,
            creatorGameProfileId = Some( userGameProfile._id ),
            gameProfiles = List( userGameProfile ),
            karmaRestrict = karmaRestrict,
            ratingRestrict = ratingRestrict,
            playersMaxCount = playersMaxCount,
            welcomeMessage = welcomeMessage.getOrElse(""),
            data = dataOpt.getOrElse(JsNull)
          )

          self ! GameCreate( gameData, Some(sessionId), autojoinCreator = true )

        }

        )

      }


    case Application.GameUserJoined( gameId, userSessionId ) =>

      val existingSet = gameUsers.getOrElse( gameId, Set() )

      gameUsers = gameUsers.updated( gameId, existingSet + userSessionId )


    case Application.GameUserJoinFailed( gameId, userSessionId, error ) =>


    case Application.GameUserLeaved( gameId, userSessionId ) =>

      if( gameUsers.contains(gameId)){

        val set = gameUsers.getOrElse(gameId, Set()) - userSessionId

        gameUsers =
          if( set.isEmpty )
            gameUsers - gameId
          else
            gameUsers.updated( gameId, set )
      }


    case actors.messages.GeneralRequest( Game.Message.gameJoin, fromSessionId, applicationId, Some( gameId ), date, data ) =>
      games.get( BSONObjectID( gameId ) ).foreach{ case ( game, gameActor ) =>

        users.get( fromSessionId ).foreach{ case ( userSession, appProfile ) =>
          gameActor ! Game.UserJoin( userSession, appProfile )
        }


      }



    case r@GeneralRequest( Application.Message.gamesGetList, sessionId, _, _, _, data ) =>
      val maxItems = 100
      val gameTypeOpt = ( data \ "type" ).asOpt[String] //
      val limit = ( data \ "limit" ).asOpt[Int].map( i => Math.min( Math.max( maxItems, i ), 1 ) )


      users.get( sessionId ).foreach{ case (userSession, _ ) =>


        val gamesList = games.
          filter{
          case ( gameId, ( game, actor ) ) =>
            gameTypeOpt.fold( true ){ gameType =>
              game.status == gameType
            }
        }.
          take(limit.getOrElse(maxItems)).
          map { case ( gameId, ( game, actor ) ) =>
          Json.obj(
            "game" -> Json.toJson( game ),
            "users" -> gameUsers.getOrElse( gameId, Set() ).flatMap{
              case userSessionId =>
                users.get( userSessionId ).map{
                  case ( us, appProfile) =>
                    Json.toJson( us.user )
                }
            }
          )
        }
        val data = Json.obj(
          "gamesList" -> gamesList,
          "limit" -> limit,
          "gameType" -> gameTypeOpt
        )

        userSession.userActor ! Response( Application.Message.gamesGetList,sessionId, data  )

      }

    case c@ChatMessage( _,_, _, _, _, _, _, recipient, _ ) =>
      recipient match {
        case ApplicationChatMessageRecipient( appGid ) if appGid == application.gid =>
          users.foreach{ case (_, ( userSession, _ )) =>
            userSession.userActor ! c
          }

        case GameChatMessageRecipient( gameId ) =>
          games.get( BSONObjectID( gameId ) ).foreach{ case ( game, gameActor ) =>
            gameActor ! c
          }

        case _ =>


      }

    // pass the event to the corresponding game
    case r@actors.messages.GeneralRequest( _, sessionId, _, Some(gameId), _, _ )
      if games.contains( BSONObjectID( gameId )) && users.contains(sessionId) =>
        games( BSONObjectID( gameId ))._2 ! r



      // process an application-wide event - gameId is empty
    case actors.messages.GeneralRequest( event, sessionId, applicationId, None, date, data ) =>
      // process the request or send to the corresponding game
      // TODO - implement - none at the moment


  }

  def broadcastGameDataUpdate( game: models.Game, creator: Option[(UserSession, models.ApplicationProfile, models.GameProfile)], gameUsers:Map[SessionId, (UserSession, models.ApplicationProfile, models.GameProfile ) ] ) = {

      implicit val jsonGameUser = new Writes[(UserSession, ApplicationProfile, GameProfile)] {
        override def writes(o: (UserSession, ApplicationProfile, GameProfile)): JsValue =
            Json.obj(
              "user" -> o._1.user,
              "applicationProfile" -> o._2,
              "gameProfile" -> o._3
          )
      }

      val responseJson = Json.obj(
        "game" -> game,
        "users" -> gameUsers.values,
        "creator" -> creator
      )
      // push update to all awaiting users
      users.values.foreach{ case ( userSession, _ ) =>

        val alreadyInGame = this.gameUsers.exists{ case ( _, set ) => set.contains( userSession.sessionId ) }

        if( !alreadyInGame ){
          userSession.userActor ! Response( Application.Message.gameNew, userSession.sessionId, responseJson  )
        }

      }


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

  // Standard messages

  object Message {
    val gameCreate = "game-create"

    val gamesGetList = "games-get-list"

    // server responses
    val gameNew = "game-new"
  }

  object Validators {
    val gameCreate =
      (
        ( __ \ "type").read[Int] and
        ( __ \ "speed").read[Int] and
        ( __ \ "karmaRestrict").read[Int] and
        ( __ \ "ratingRestrict").read[Int] and
        ( __ \ "playersMaxCount").read[Int] and
        ( __ \ "welcomeMessage").readNullable[String] and
        ( __ \ "data").readNullable[JsValue]
      ).tupled
  }

  import models.ApplicationProfiles.{ jsonFormat => f0 }
  import models.Applications.{ format => f1 }

  // it's sent from the Gateway to the Application
  case class UserJoin( id:SessionId, dbUser:models.User, channel: Concurrent.Channel[JsValue]) extends InternalMessage
  case class UserJoinedSuccessfully( userSession:UserSession, applicationProfile: ApplicationProfile ) extends InternalMessage

  case class GameCreate( data:models.Game, creator:Option[SessionId] = None, autojoinCreator: Boolean = true ) extends InternalMessage
  case class GameCreatedSuccessfully( game:models.Game, gameCreatorSessionId:Option[SessionId], gameActor:ActorRef ) extends InternalMessage
  case class GameCreateFailed( reason:String ) extends InternalMessage

  case class GameUserJoined( gameId: BSONObjectID, userSessionId:SessionId )
  case class GameUserJoinFailed( gameId: BSONObjectID, userSessionId:SessionId, reason: Throwable )
  case class GameUserLeaved( gameId: BSONObjectID, userSessionId:SessionId )

  case class GameDataUpdated( game: models.Game, creator: Option[(UserSession, models.ApplicationProfile, models.GameProfile)], gameUsers:Map[SessionId, (UserSession, models.ApplicationProfile, models.GameProfile ) ] )

  case class GameFinished( game: BSONObjectID ) extends InternalMessage

  class ApplicationProfileCreateFailed(msg:String) extends Throwable

  case class UserJoinedSuccessfullyResponse(sessionId:SessionId, application: models.Application, applicationProfile: models.ApplicationProfile) extends
    actors.messages.Response("application.user_joined_successfully", SingleRecipient(sessionId), Json.toJson( Json.obj( "application" -> application, "applicationProfile" -> applicationProfile ) ) )


}
