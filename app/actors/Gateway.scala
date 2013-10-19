package actors

import socketio.SocketIOActor
import play.api.libs.json._
import actors.messages._
import scala.collection.mutable
import java.util.Date
import akka.actor.ActorRef
import models.{Users, User}
import actors.messages.Session
import actors.messages.Recipient
import actors.messages.GeneralRequest
import play.api.libs.functional.syntax._

/**
 * User: aloise
 * Date: 9/28/13
 * Time: 10:04 PM
 */
class Gateway extends SocketIOActor {


  // map all users to sessionIds
  val users = mutable.HashMap[Session, models.User]()

  val applications = mutable.HashMap[Int, ActorRef]()

  override def receive = {
    // write a response
    case actors.messages.Response( event:String, recipients: Recipient, data:JsValue ) => processResponse(event, recipients, data)
    // process an input message
    case message => super.receive(message)

  }




  def processMessage: PartialFunction[(String, (String, String, Any)), Unit] = {

    //Handle event
    case ("message", (sessionId: String, namespace: String, eventData: JsValue)) =>

      processRequest( GeneralRequest(
        namespace,
        Session(sessionId, (eventData \ "userId").asOpt[Int] ),
        (eventData \ "applicationId").asOpt[Int],
        (eventData \ "gameId").asOpt[Int],
        new Date(),
        eventData
      ))
/*
    case ("connected", (sessionId: String, namespace: String, msg: String)) =>{
//      println("New session created . .  .")
//      send(sessionId, "welcome");

    }
*/

  }

  def processRequest(request: Request):Unit = request match {
    // process a special case - logout
    case GeneralRequest("logout",_,_, _, _, _) => processLogoutRequest(request)
    case GeneralRequest("login",_,_,_,_,_) => processLoginRequest(request)
    // route all other requests
    case _ => request.applicationId.map( applications.get( _ ).map( _ ! request ) )
  }

  def processResponse(event:String, recipients: Recipient, value: JsValue) = {

  }



  def processLoginRequest(request: Request) = {
    // log the user in, retrieve and id and broadcast the message

    val reader = (( __ \ "id").read[Int] and  ( __ \ "signature").read[String]).tupled

    request.data.validate[(Int,String)](reader).map{
      case (id, signature) => Users.authenticate(id,signature)
    }.recoverTotal( _ => self ! ErrorResponse( "login", SessionRecipient(request.session) ) )

  }

  def processLogoutRequest(request: Request) = {
    users.get(request.session).map( u => {
        // notify the app about the user logout
        request.applicationId.map( applications.get( _ ).map( _ ! request ) )
        // remove user from the list
        users - request.session
      }
    )

  }

}
