package actors


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

  // map all users to sessionIds
  var users = Map[SessionId, UserSession]()

  var applications = Map[String, ( models.Application, ActorRef ) ]()

  var userSockets = Map[SessionId, Concurrent.Channel[JsValue] ]()



  override def receive = {

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
    case request@GeneralRequest("logout",_,_, _, _, _) => processLogoutRequest(request)

    case request@GeneralRequest("login",_,_,_,_,_) => processLoginOrUserCreateRequest(request)

    case Gateway.ApplicationCreate(appId) =>

      models.Applications.find( Json.obj("gid" -> appId ) ).map { apps =>
        apps.headOption.map { app =>
            val applicationActor = context.actorOf( getApplicationActorProps(app) )
            applications = applications + ( app.gid -> ( app, applicationActor ) )
        }
      }

    // route all other requests to the app by the applicationId in the request
    case request:actors.messages.Request => request.applicationId.flatMap( applications.get ).foreach{
        _._2 ! request
      }


    case response:Response =>
      // bypass directly to recipient sender actors, normally a gateway response message is just an error ( see processLoginOrUserCreateRequest )
      response.recipients.foreach { sessionId =>
        users.get(sessionId).foreach{ _.userActor ! response }
      }

  }

  def getApplicationActorProps(dbApplication:models.Application) = Props(classOf[actors.Application], dbApplication)

  def processLoginOrUserCreateRequest(request: Request) = {
    // log the user in, retrieve and id and broadcast the message

    val me = self

    def error( msg:String ) = me ! ErrorResponse( msg, SingleRecipient(request.sessionId), msg )

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
        case None => error("user_socket_session_was_not_found")
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
      ( __ \ "username").readNullable[String]
    ).tupled

    request.data.validate[(Int,String,Option[String] )](reader).map{

      case (id, signature, maybeUsername ) =>

        request.applicationId.flatMap{ applications.get } match {
          case Some( (_, applicationActor )) =>
             Users.authenticateOrCreate( id, signature, maybeUsername).foreach {
                case Some(dbUser) => processUserCreationByApplication(applicationActor, request.sessionId, dbUser)
                case None => error( "user_not_found" )
              }
          case None => error( "application_not_found" )
        }

    }.recoverTotal( _ => error( "invalid_format" ) )

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

  case class ApplicationCreate( id:String )

}