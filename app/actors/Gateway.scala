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
import actors.messages.UserConnect
import actors.messages.UserConnectAccepted
import scala.Some
import actors.messages.SingleRecipient
import actors.messages.GeneralRequest
import actors.messages.UserConnectFailed
import actors.messages.UserSenderActorInit
import play.api.libs.iteratee.{Enumerator, Concurrent}
import scala.concurrent.ExecutionContext


class Gateway extends Actor {

//  private val receiverProps = Props[WebSocketReceiver]
//  private val senderProps = Props[WebSocketSender]

  // map all users to sessionIds
  var users = Map[SessionId, UserSession]()

  var applications = Map[String, ActorRef]()

  var userSockets = Map[SessionId, ( Enumerator[JsValue], Concurrent.Channel[JsValue])]()

  override def receive = {

    // process a user connection, create actors and writes the session.. user db info will be empty till the login
    case UserConnect(sessionId) =>

      if (users.contains(sessionId)) {
          // imho - it's an impossible case within a socket connection
          sender ! UserConnectFailed(sessionId, "already_connected")
      } else {

         // process user connect
        processUserConnect( sessionId )
      }

    case UserDisconnected(sessionId) =>
      // remove the user from the list and notify the app
      disconnectUser(sessionId)


    case request@GeneralRequest("logout",_,_, _, _, _) => processLogoutRequest(request)

    case request@GeneralRequest("login",_,_,_,_,_) => processLoginOrUserCreateRequest(request)

    // route all other requests to the app by the applicationId in the request
    case request:actors.messages.Request => request.applicationId.flatMap( applications.get ).foreach{
        _ ! request
      }
    case response:Response =>
      // bypass directly to recipient sender actors, normally a gateway response message is just an error ( see processLoginOrUserCreateRequest )
      response.recipients.foreach { sessionId =>
        users.get(sessionId).foreach{ _.sender ! response }
      }

  }



  def processLoginOrUserCreateRequest(request: Request) = {
    // log the user in, retrieve and id and broadcast the message

    def error( msg:String ) = self ! ErrorResponse( msg, SingleRecipient(request.sessionId), msg )

    def processUserCreationByApplication( sessionId:SessionId, dbUser:models.User ) = {

      // check the userSocket existence and the  existence

      /*    implicit val timeout = Timeout(60 seconds)

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


    val msg = "login"
    val reader = (
      ( __ \ "id").read[Int] and
      ( __ \ "signature").read[String] and
      ( __ \ "username").readNullable[String]
    ).tupled

    request.data.validate[(Int,String,Option[String] )](reader).map{

      case (id, signature, maybeUsername, applicationId) =>

        request.applicationId.flatMap{ applications.get } match {
          case Some(applicationActor) =>
             Users.authenticateOrCreate( id, signature, maybeUsername).foreach {
                case Some(dbUser) => processUserCreationByApplication(request.sessionId, dbUser)
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
    request.applicationId.map( applications.get( _ ).map( _ ! UserDisconnected(request.sessionId) ) )
    // I don't like to disconnect here... It will be really disconnected on network event ( UserDisconnected )

  }


  def processUserConnect(id: SessionId) = {

    // create an enumerator and wait until the login message would be passed
    val enumerator:Enumerator[JsValue] = Concurrent.unicast[JsValue]{ channel =>
      userSockets = userSockets + ( id -> ( enumerator, channel ) )
    }

  }

  def disconnectUser( userSessionId:SessionId ) =
    users.get(userSessionId).map( u => {

      // notify all apps about the user disconnect
      applications.foreach( _._2 ! UserDisconnected( userSessionId ) )

      // remove user from the list
      users = users - userSessionId
      userSockets = userSockets - userSessionId
    })



}
