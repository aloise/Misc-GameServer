package actors

import akka.actor._
import messages._

/**
 * User: aloise
 * Date: 9/4/13
 * Time: 11:08 PM
 */
class Game( val game:models.Game) extends Actor {

  var users:Map[Int,ActorRef] = Map()

  def receive = {
    case messages.Join(_) => sender ! Unit
  }

}
