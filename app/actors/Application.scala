package actors

import akka.actor._
import messages._

class Application extends Actor {

  var games:Map[Int, ActorRef] = Map()

  def receive = {

  }


}
