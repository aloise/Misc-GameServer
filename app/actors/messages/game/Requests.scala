package actors.messages.game

import models.GameProfile
import play.api.libs.json.JsValue
import java.util.Date
import org.bson.types.ObjectId
import actors.messages.global.Id.UserId
import actors.messages.global.UserRequestInfo

/**
 * User: aloise
 * Date: 9/4/13
 * Time: 11:52 PM
 */
// Game Actor will receive following messages

case class UserRequestInfo(sessionId:String, game:GameProfile, user: UserId, date:Date)

// case class Auth(request:RequestInfo  ) - it's process internally
// case class CreateGame

abstract class Request(val request:UserRequestInfo)

case class Join(request:UserRequestInfo) extends Request(request)

/**
 * Start the game - admin or creator only
 * @param request
 */
case class Start(request:UserRequestInfo) extends Request(request)

/**
 * Stop the game - admin or creator only
 * @param request
 */
case class Stop(request:UserRequestInfo) extends Request(request)

/**
 * Send a message to the in-game chat
 * @param request
 * @param recipients - a list of recipients of the message.
 * @param message - text message
 */
case class ChatMessage(request:UserRequestInfo, recipients:List[UserId], message:String) extends Request(request)


case class Action(request:UserRequestInfo, `type`:String, data:JsValue) extends Request(request)
case class KickUser(request:UserRequestInfo, user:UserId) extends Request(request)
case class Quit(request:UserRequestInfo) extends Request(request)
