package actors.applications.dixie

import actors.Application
import actors.messages.{Response, Request}
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
    Some( Props(classOf[actors.applications.dixie.DixieGame], app, gameProfile, cards ) )
  }

}

object DixieApplication {


  case class GameCard(id:String, name:String, image:String)


  object Messages {

    /*
    case class UserGameCard( cards : GameCard )  extends Response( ) // to all

    case class UserGameTurn( userId: BSONObjectID ) extends Response ( ) // how's turn is it currently - sent to all

    case class UserGameStep( cardId:String, text:String ) extends Request()

    case class UserGameStepMessageRequest( userId: BSONObjectID, text:String ) extends Response() // card text is sent to all

    case class UserGameStepMessageResponse( cardId:String ) extends Request() // sent from every use, cards are hidden.

    case class GameTurnVoteStart( text:String, cardsChoosen:Seq[String] ) extends Response() // sent to all users

    case class GameTurnVoteForCard( cardId:String ) extends Request() // user choses a best card

    case class GameTurnVoteResults( voteResults: Map[String,Int], userVotes:Map[String,BSONObjectID], userPoints: Seq[] ) // how much votes summary for every cards,  who voted for what

    // next game turn starts
    */

  }
}