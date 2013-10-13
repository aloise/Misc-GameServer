package actors.messages

import play.api.libs.json.JsValue

/**
 * User: aloise
 * Date: 10/12/13
 * Time: 10:54 PM
 */

case class Recipient(recipient:Session)


case class Response(
  request: Request,
  recipient: Recipient,
  data:JsValue
)
