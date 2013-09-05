package actors.messages.game.requests

import models.GameProfile
import play.api.libs.json.JsValue
import java.util.Date
import org.bson.types.ObjectId

/**
 * User: aloise
 * Date: 9/4/13
 * Time: 11:52 PM
 */
// Game Actor will receive following messages

case class RequestInfo(sessionId:String, from:GameProfile, date:Date)

abstract class Request(val request:RequestInfo)


case class Join(request:RequestInfo) extends Request(request)
case class Leave(request:RequestInfo) extends Request(request)

// empty list means Game-Global Message
case class Action(request:RequestInfo, data:JsValue ) extends Request(request)
case class Chat(request:RequestInfo, to:List[ObjectId], message:String ) extends Request(request)