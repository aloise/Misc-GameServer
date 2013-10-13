package actors.messages

import play.api.libs.json.JsValue

/**
 * User: aloise
 * Date: 10/12/13
 * Time: 10:54 PM
 */

abstract class Recipient
case class RecipientList(recipients:Seq[Session]) extends Recipient


case class Response(
  request: Request,
  recipient: Recipient,
  data:JsValue
)
