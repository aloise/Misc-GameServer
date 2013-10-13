package actors

import socketio.SocketIOActor
import play.api.libs.json.{Json, JsValue}
import actors.messages.{GeneralRequest, Session, Recipient, Request}
import scala.collection.mutable
import java.util.Date
import akka.actor.ActorRef
import models.User

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
    case actors.messages.Response( request: Request, recipients: Recipient, data:JsValue ) => processResponse(request, recipients, data)
    // process an input message
    case message => super.receive(message)

  }




  def processMessage: PartialFunction[(String, (String, String, Any)), Unit] = {

    //Handle event
    case (event:String, (sessionId: String, namespace: String, eventData: JsValue)) =>

      processRequest( GeneralRequest(
        event,
        Session(sessionId, (eventData \ "userId").asOpt[Int] ),
        (eventData \ "applicationId").asOpt[Int],
        (eventData \ "gameId").asOpt[Int],
        namespace,
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
    case GeneralRequest("logout",_,_, _,"global", _, _) => processLogoutRequest(request)
    case GeneralRequest("login",_,_,_,"global",_,_) => processLogoutRequest(request)
    // route all other requests
    case _ => request.applicationId.map( applications.get( _ ).map( _ ! request ) )
  }

  def processResponse(request: Request, recipients: Recipient, value: JsValue) = {

  }



  def processLoginRequest(request: Request) = {
    // log the user in, retrieve and id and broadcast the message
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
