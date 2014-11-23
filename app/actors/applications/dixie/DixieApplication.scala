package actors.applications.dixie

import actors.Application
import akka.actor.{Props, ActorRef}
import reactivemongo.bson._
/**
 * User: aloise
 * Date: 22.11.14
 * Time: 19:44
 */
class DixieApplication( application:models.Application ) extends Application( application ) {

  import actors.applications.dixie.DixieApplication._


  override def receive:Receive = receiveNormal orElse super.receive

  def receiveNormal:Receive = {
    case _ =>
  }

  override def getGameActorProps( gameProfile:models.Game, app:ActorRef = context.self ):Option[Props] = {
    Some( Props(classOf[actors.applications.dixie.DixieGame], app, gameProfile) )
  }

}

object DixieApplication {
  type GameCard = String
}