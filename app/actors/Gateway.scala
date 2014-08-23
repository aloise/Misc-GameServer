package actors


import play.api.Logger
import play.api.libs.json._
import actors.messages._
import java.util.Date
import akka.actor.{PoisonPill, Props, Actor, ActorRef}
import models.{Users, User}
import play.api.libs.functional.syntax._
import scala.concurrent.duration._
import actors.messages.UserSession.SessionId
import akka.util.Timeout
import akka.pattern.{ ask, pipe }
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import scala.Some
import play.api.libs.iteratee.{Input, Enumerator, Concurrent}
import scala.concurrent.ExecutionContext
import actors.messages.Response
import scala.Some
import actors.messages.SingleRecipient
import actors.messages.GeneralRequest


class Gateway extends Actor {

  import models.Applications.format

//  private val receiverProps = Props[WebSocketReceiver]
//  private val senderProps = Props[WebSocketSender]

  // map all users to sessionIds  - authorized by application users
  var users = Map[SessionId, UserSession]()

  // A mapping from Application.gid ->
  var applications = Map[String, ( models.Application, ActorRef ) ]()

  // TODO - move it inside the "users" var. Those are unathorized users both with authorized
  var userSockets = Map[SessionId, Concurrent.Channel[JsValue] ]()

  override def receive:Receive = {

    // Socket Events
    // process a user connection, create actors and writes the session.. user db info will be empty till the login
    case Gateway.UserConnect(sessionId) =>

      if (users.contains(sessionId)) {
          // imho - it's an impossible case within a socket connection
          sender ! Gateway.UserConnectFailed(sessionId, "already_connected")
      } else {

         // process user connect
        val enumerator = processUserConnect( sessionId )
        sender ! Gateway.UserConnectAccepted(sessionId, enumerator )
      }

    case Gateway.UserDisconnected(sessionId) =>
      // remove the user from the list and notify the app
      disconnectUser(sessionId)

    // User request
    case request@GeneralRequest("logout",_,_, _, _, _) =>
      processLogoutRequest(request)

    case request@GeneralRequest("login",_,_,_,_,_) =>
      processLoginOrUserCreateRequest(request)

    case Gateway.ApplicationCreate(appId) =>

      models.Applications.find( Json.obj("gid" -> appId ) ).map { apps =>
        apps.headOption.map { app =>
            val applicationActor = context.actorOf( getApplicationActorProps(app) )
            applications = applications + ( app.gid -> ( app, applicationActor ) )
        }
      }


    case request@GeneralRequest( ChatMessages.eventName,_,_, _, _, _  ) =>
      processChatMessage( request )



    // route all other requests to the app by the applicationId in the request
    case request:actors.messages.Request =>
      request.applicationId.flatMap( applications.get ).foreach{
        _._2 ! request
      }


    case response:Response =>
      // bypass directly to recipient sender actors, usually a gateway response message is just an error ( see processLoginOrUserCreateRequest )
      response.recipients.foreach { sessionId =>

        if( users.contains(sessionId) ){
          users.get(sessionId).foreach{
            _.userActor ! response
          }
        } else {
          userSockets.get( sessionId ).foreach {
            _.push( response.toJson )
          }
        }


      }



  }

  def processChatMessage( request:GeneralRequest ) = {

    request match {
      case GeneralRequest( ChatMessages.eventName , sessionId,_, _, _, _) =>
          val user = users.get( sessionId )

          // let convert it from the general request
          val chatMessageOpt = ChatMessages.fromGeneralRequest( request, user.map( _.user ) )

          chatMessageOpt match {
            case Some( chatMessage@ChatMessage( _,_, _, _, _, _, _, recipient, _ ) ) =>

              Logger.debug( "I've got a chat message - " + chatMessage )

              recipient match {
                case UserListChatMessageRecipient( usernames ) =>
                  // TODO optimize this call for a large number of users
                  usernames flatMap { name =>
                    users.find{ case ( _, session ) => session.user.name == name }
                  } foreach { case ( _, session ) =>
                    session.userActor ! chatMessage
                  }

                case ApplicationChatMessageRecipient( appGid ) =>
                  // delegate it to app actor
                  val application = applications.get( appGid ) orElse request.applicationId.flatMap( applications.get )

                  application.foreach{ case ( _, appActor ) =>
                    appActor ! chatMessage
                  }

                case GameChatMessageRecipient( gameId ) =>
                  // TODO ! it requires the application it to be set
                  request.applicationId.flatMap( applications.get ).foreach{ case ( _, appActor ) =>
                    appActor ! chatMessage
                  }


                case r =>
                  Logger.debug( "I don't know how to route the chat message for " + r )
              }



            case None =>
              Logger.debug( "I've got a broken chat message - " + request )
          }
    }

  }

  def getApplicationActorProps(dbApplication:models.Application) =
    Props(classOf[actors.Application], dbApplication)

  def processLoginOrUserCreateRequest(request: Request) = {
    // log the user in, retrieve and id and broadcast the message

    val me = self

    def sendError( msg:String ) =
      me ! ErrorResponse( "login", SingleRecipient(request.sessionId), msg )

    def processUserCreationByApplication( applicationActor:ActorRef, sessionId:SessionId, dbUser:models.User ) = {

      implicit val timeout = Timeout(60 seconds)

      // check the userSocket existence and the absence in already logged users list
      userSockets.get(sessionId) match {
        // it will trigger an user actor creation
        case Some( channel ) =>
          ( applicationActor ? Application.UserJoin( sessionId, dbUser, channel ) ).map{

            case Application.UserJoinedSuccessfully(userSession, appProfile) =>
              // update the user session

              users = users + ( userSession.sessionId -> userSession )

            case _ => disconnectUser( sessionId )
          }.onFailure{
            // timeout or whatever
            case _ => disconnectUser( sessionId )
          }
        case None =>
          sendError("user_socket_session_was_not_found")
      }


      /*

          val receiveActor = context.actorOf(receiverProps, sessionId+"-receiver")
          val sendActor = context.actorOf(senderProps, sessionId+"-sender")

          val userConnectedMessage = (sendActor ? UserActorInit(sessionId )).map{
            case c: UserConnectAccepted =>
              play.Logger.debug(s"Connected Member with ID:$sessionId")

              users = users + (sessionId -> UserSession(sessionId, None, receiveActor, sendActor) )

              c
          }

          userConnectedMessage pipeTo sender*/
    }


//    val msg = "login"
    val reader = (
      ( __ \ "id").read[Int] and
      ( __ \ "signature").read[String] and
      ( __ \ "name").readNullable[String] and
      ( __ \ "avatar").readNullable[String]
    ).tupled

    request.data.validate(reader).map{

      case (id, signature, maybeUsername, avatar ) =>

        request.applicationId.flatMap{ applications.get } match {
          case Some( (_, applicationActor )) =>
             Users.authenticateOrCreate( id, signature, maybeUsername, avatar).foreach {
                case Some(dbUser) =>
                  processUserCreationByApplication(applicationActor, request.sessionId, dbUser)
                case None =>
                  sendError( "user_not_found" )
              }
          case None =>
            sendError( "application_not_found" )
        }

    }.recoverTotal{ e =>
      println(e)
      sendError( "invalid_format" )
    }

  }




  def processLogoutRequest(request: Request) = {
    // notify the app about the user logout
    // this event is translated into the UserDisconnected on the Application level.
    // User logout on Application level is an equivalent of the disconnect
    request.applicationId.map( applications.get( _ ).map( _._2 ! Gateway.UserDisconnected(request.sessionId) ) )
    // I don't like to disconnect here... It will be really disconnected on network event ( UserDisconnected )

  }


  def processUserConnect(id: SessionId) = {

    // create an enumerator and wait until the login message would be passed
    val enumerator:Enumerator[JsValue] = Concurrent.unicast[JsValue]{ channel =>
      userSockets = userSockets + ( id -> channel )
    }

    enumerator
  }

  def disconnectUser( userSessionId:SessionId ) = {

    userSockets.get(userSessionId).map { case channel  =>

      if( !users.contains(userSessionId)){
        // we need to close the channel. This socket was not connected to an Application
        channel.push(Input.EOF)
      }
      userSockets = userSockets - userSessionId
    }


    users.get(userSessionId).foreach( u => {

      // notify all apps about the user disconnect
      // application will kill the actor and close the channel
      applications.find( _._2._2 == u.application ).foreach( _._2._2 ! Gateway.UserDisconnected( userSessionId ) )

      // remove user from the list
      users = users - userSessionId

    })


  }

}

object Gateway {
  case class UserConnect(id: SessionId) extends InternalMessage
  case class UserConnectAccepted(id: SessionId, enumerator: Enumerator[JsValue]) extends InternalMessage
  case class UserConnectFailed(id: SessionId, error: String) extends InternalMessage

  // it's sent to gateway and forwarded to the Application and the game
  case class UserDisconnected(id:SessionId) extends InternalMessage

  case class ApplicationCreate( applicationGid:String )

}