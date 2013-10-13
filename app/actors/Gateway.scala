package actors

import socketio.SocketIOActor
import play.api.libs.json.{Json, JsValue}
import actors.messages.{Session, Recipient, Request}
import scala.collection.mutable
import akka.actor.ActorRef

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
    case (event:String, (sessionId: String, namespace: String, eventData: JsValue)) => {
       // translate the JSON message into the Akka Message

//      println(sessionId + " handling Event in my Socket --- " + Json.stringify(eventData))
/*
      emit(sessionId, namespace,
        Json.stringify(
          Json.toJson(Map(
            "name" -> Json.toJson("someEvt"),
            "args" -> eventData
          )
          )
        )
      )
*/

    }

    case ("connected", (sessionId: String, namespace: String, msg: String)) =>{
//      println("New session created . .  .")
//      send(sessionId, "welcome");

    }

    // process the response - route the message


  }


  def processResponse(request: Request, recipients: Recipient, value: JsValue) = {

  }

}
