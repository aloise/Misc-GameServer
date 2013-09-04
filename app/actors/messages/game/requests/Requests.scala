package actors.messages.game.requests

import models.GameProfile
import play.api.libs.json.JsValue

/**
 * User: aloise
 * Date: 9/4/13
 * Time: 11:52 PM
 */
// Game Actor will receive following messages

abstract class BasicRequest(val sessionId:String, val from:GameProfile)

case class Join(sessionId:String, from:GameProfile) extends BasicRequest(sessionId, from)
case class Leave(sessionId:String, from:GameProfile) extends BasicRequest

// empty list means Game-Global Message
case class Request(to:List[GameProfile], data:JsValue ) extends BasicRequest