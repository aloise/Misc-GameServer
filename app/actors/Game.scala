package actors

import akka.actor._
import scala.collection.mutable

/**
 * User: aloise
 * Date: 9/4/13
 * Time: 11:08 PM
 */
class Game(application:ActorRef, game:models.Game) extends Actor {

  var users = mutable.Map[Int,ActorRef]()

  def receive = {
    case r:messages.Response => application ! r
  }

}
