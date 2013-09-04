package actors.messages.game.replies

import play.api.libs.json.JsValue

/**
 * User: aloise
 * Date: 9/4/13
 * Time: 11:51 PM
 */
// Game Actor will reply with following messages

abstract case class BasicReply(request:BasicRequest, reply: JsValue)
case class Reply() extends BasicReply

case class GameStart() extends BasicReply