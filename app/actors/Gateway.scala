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


class Gateway extends Actor {

  private val receiverProps = Props[WebSocketReceiver]
  private val senderProps = Props[WebSocketSender]

  // map all users to sessionIds
  var users = Map[SessionId, UserSession]()

  var applications = Map[String, ActorRef]()

  override def receive = {

    // process a user connection, create actors and writes the session.. user db info will be empty till the login
    case UserConnect(sessionId) =>

      if (users.contains(sessionId)) {
          // imho - it's an impossible case within a socket connection
          sender ! UserConnectFailed(sessionId, "already_connected")
      } else {

        implicit val timeout = Timeout(60 seconds)

        val receiveActor = context.actorOf(receiverProps, sessionId+"-receiver")
        val sendActor = context.actorOf(senderProps, sessionId+"-sender")

        val userConnectedMessage = (sendActor ? UserSenderActorInit(sessionId, receiveActor)).map{
          case c: UserConnectAccepted =>
            play.Logger.debug(s"Connected Member with ID:$sessionId")

            users = users + (sessionId -> UserSession(sessionId, None, receiveActor, sendActor) )

            c
        }

        userConnectedMessage pipeTo sender
      }

    case UserDisconnected(sessionId) => {
      // remove the user from the list and notify the app
      disconnectUser(sessionId)
    }

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


    // and error response to someone
    /*
    case req:actors.messages.Request =>
      users.get(req.sessionId).foreach {
        case UserSession(_,_,_, receiver) => receiver ! req
      }
    */

    // broadcast the message - Response is processed in User actor
    /*
    case response:actors.messages.Response =>
      response.recipients.get.foreach {
        users.get(_).foreach{
          case UserSession(_,_, sender, _ ) => sender ! response
        }
      }
    */

  }



  def processLoginOrUserCreateRequest(request: Request) = {
    // log the user in, retrieve and id and broadcast the message
    val msg = "login"
    val reader = (
      ( __ \ "id").read[Int] and
      ( __ \ "signature").read[String] and
      ( __ \ "username").readNullable[String] and
      ( __ \ "applicationId").read[String]
    ).tupled

    request.data.validate[(Int,String,Option[String], String)](reader).map{

      case (id, signature, maybeUsername, applicationId) =>

        applications.get(applicationId) match {
          case Some(applicationActor) =>
             Users.authenticateOrCreate( id, signature, maybeUsername).foreach {
                case Some(u:models.User) =>
                  // update the user entry
                  users.get( request.sessionId ) match {

                    case Some(x) =>
                      // update the user session data with user DB info
                      users = users + ( request.sessionId -> x.copy(user = Some(u)) )
                    case None =>
                      self ! ErrorResponse( msg, SingleRecipient(request.sessionId), "user_session_is_missing" )
                  }
                case None => self ! ErrorResponse( msg, SingleRecipient(request.sessionId), "user_not_found" )
              }
          case None => self ! ErrorResponse( msg, SingleRecipient(request.sessionId), "application_not_found" )
        }

    }.recoverTotal( _ => self ! ErrorResponse( msg, SingleRecipient(request.sessionId), "invalid_format" ) )

  }

  def processLogoutRequest(request: Request) = {
    // notify the app about the user logout
    // this event is translated into the UserDisconnected on the Application level.
    // User logout on Application level is an equivalent of the disconnect
    request.applicationId.map( applications.get( _ ).map( _ ! UserDisconnected(request.sessionId) ) )
    // I don't like to disconnect here... It will be really disconnected on network event ( UserDisconnected )

  }


  def disconnectUser( userSessionId:SessionId ) =
    users.get(userSessionId).map( u => {

      // notify all apps about the user disconnect
      applications.foreach( _._2 ! UserDisconnected( userSessionId ) )

      // terminate actores
      u.sender ! PoisonPill
      u.receiver ! PoisonPill
      // remove user from the list
      users = users - userSessionId
    })


}
