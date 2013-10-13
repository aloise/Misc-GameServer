package actors

import akka.actor.{ActorRef, Actor}
import messages._
import scala.collection.mutable

class Application(gateway:ActorRef, application:models.Application) extends Actor {

  val games = mutable.HashMap[Int, ActorRef]()
  val users = mutable.HashMap[Session, models.User]()

  def receive = {
    case r:messages.Response => gateway ! r
  }


}
