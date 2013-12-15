package actors


import play.api.libs.json._
import actors.messages._
import java.util.Date
import akka.actor.{Props, Actor, ActorRef}
import models.{Users, User}
import play.api.libs.functional.syntax._
import scala.concurrent.duration._
import actors.messages.UserSession.Session
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
  var users = Map[Session, UserSession]()

  var applications = Map[Int, ActorRef]()

  override def receive = {
    // write a response
    case UserConnect(sessionId) =>

      if (users.contains(sessionId)) {
          // imho - it's an impossible case within a socket connection
          sender ! UserConnectFailed(sessionId, "already_connected")
      } else {

        implicit val timeout = Timeout(30 seconds)

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

    case request@GeneralRequest("logout",_,_, _, _, _) => processLogoutRequest(request)
    case request@GeneralRequest("login",_,_,_,_,_) => processLoginOrUserCreateRequest(request)
    // route all other requests to the app by the applicationId in the request
    case request:actors.messages.Request => request.applicationId.flatMap( applications.get ).foreach{
      _ ! request
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
    val reader = (( __ \ "id").read[Int] and  ( __ \ "signature").read[String]).tupled

    request.data.validate[(Int,String)](reader).map{

      case (id, signature) =>
        Users.authenticateOrCreate(id,signature).foreach {
          case Some(u:models.User) =>
            // update the user entry
            users.get( request.sessionId ) match {
              case Some(x) => users = users + ( request.sessionId -> x.copy(user = Some(u)) )
              case None => self ! ErrorResponse( msg, SingleRecipient(request.sessionId), "user_session_is_missing" )
            }

          case None => self ! ErrorResponse( msg, SingleRecipient(request.sessionId), "user_not_found" )

        }

    }.recoverTotal( _ => self ! ErrorResponse( msg, SingleRecipient(request.sessionId), "invalid_format" ) )

  }

  def processLogoutRequest(request: Request) =
    users.get(request.sessionId).map( u => {
        // notify the app about the user logout
        request.applicationId.map( applications.get( _ ).map( _ ! request ) )
        // remove user from the list
        users = users - request.sessionId
      }
    )


}
