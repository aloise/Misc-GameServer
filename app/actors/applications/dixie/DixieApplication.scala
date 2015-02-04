package actors.applications.dixie

import actors.Application
import akka.actor.{Props, ActorRef}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import reactivemongo.bson._


/**
 * User: aloise
 * Date: 22.11.14
 * Time: 19:44
 */
class DixieApplication( application:models.Application ) extends Application( application ) {

  import actors.applications.dixie.DixieApplication._

  implicit val cardToJson = Json.format[GameCard]

  val cards:Map[String,GameCard] = application.data.validate[Seq[GameCard]] match {
    case JsSuccess( list, _ ) =>
      list.map( c => c.id -> c ).toMap
    case _ =>
      Map()
  }



  override def receive:Receive = receiveNormal orElse super.receive

  def receiveNormal:Receive = {
    case _ =>
  }

  override def getGameActorProps( gameProfile:models.Game, app:ActorRef = context.self ):Option[Props] = {
    Some( Props(classOf[actors.applications.dixie.DixieGame], app, gameProfile) )
  }

}

object DixieApplication {


  case class GameCard(id:String, name:String, image:String)


}