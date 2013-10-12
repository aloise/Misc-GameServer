package actors

import akka.actor._
import messages._
import scala.collection.mutable

class Application extends Actor {

  var games:Map[Int, ActorRef] = mutable.Map[Int,ActorRef]

  def receive = {

  }


}
