package actors.messages.game

import play.api.libs.json.JsValue
import actors.messages.game.Request

/**
 * User: aloise
 * Date: 9/4/13
 * Time: 11:51 PM
 */

// Game Actor will send following messages

abstract class BasicReply(request:Request)

// general replies
case class Ok(request:Request) extends BasicReply(request)
case class Error(request:Request, error: String) extends BasicReply(request)

// specific replies
case class GameStart(request:Request, reply: JsValue) extends BasicReply(request)
case class GameEnd(request:Request, reply: JsValue) extends BasicReply(request)

