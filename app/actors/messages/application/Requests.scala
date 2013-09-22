package actors.messages.application

import models.GameProfile
import actors.messages.global.Id._
import java.util.Date

/**
 * User: aloise
 * Date: 9/22/13
 * Time: 10:56 PM
 */

case class ApplicationRequestInfo(sessionId:String, user: UserId, date:Date)

abstract class Request(request:ApplicationRequestInfo)

case class CreateGame(request:ApplicationRequestInfo) extends Request(request)
case class JoinGame(request:ApplicationRequestInfo) extends Request(request)
case class GameList(request:ApplicationRequestInfo)  extends Request(request)

/**
 *
 * @param request
 * @param minKarma - Minimum Karma that is required to join the game. -1 is empty
 */
case class AutoJoinGame(request:ApplicationRequestInfo, minKarma:Int)  extends Request(request)

