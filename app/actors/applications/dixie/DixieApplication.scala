package actors.applications.dixie

import actors.Application
import play.modules.reactivemongo.json.BSONFormats._
import actors.messages.{DecodedApplicationRequest, Response, Request}
import akka.actor.{Props, ActorRef}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import reactivemongo.bson._
import julienrf.variants.Variants

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
    case "test" =>
      println("DixieApplication::test")
  }

  override def getGameActorProps( gameProfile:models.Game, app:ActorRef = context.self ):Option[Props] = {
    Some( Props(classOf[actors.applications.dixie.DixieGame], app, gameProfile, cards ) )
  }

  override def decodeGameRequest( r:Request ):DecodedApplicationRequest = {
    val data = ( r.data match {
      case j@JsObject( _ ) =>
        j
      case _ =>
        Json.obj()
    } ) ++ Json.obj( "event" -> r.event )

    Messages.requestMessageFormat.reads( data ) match {
      case JsSuccess( any, _ ) =>
        DecodedApplicationRequest( r.event, r.sessionId, r.applicationId, r.gameId, r.date, r.data, any )
      case _ =>
        super.decodeGameRequest(r)
    }

  }

}

object DixieApplication {


  trait GeneralRequest

  case class GameCard(id:String, name:String, image:String)


  object Messages {

    sealed trait GeneralRequest
    sealed trait GeneralResponse


    case class UserGameCard( cards : GameCard )  extends GeneralResponse // to all

    case class UserGameTurn( userId: BSONObjectID ) extends GeneralResponse // how's turn is it currently - sent to all

    case class UserGameStep( cardId:String, text:String ) extends GeneralRequest

    case class UserGameStepMessageRequest( userId: BSONObjectID, text:String ) extends GeneralResponse // card text is sent to all

    case class UserGameStepMessageResponse( cardId:String ) extends GeneralRequest // sent from every use, cards are hidden.

    case class GameTurnVoteStart( text:String, cardsChoosen:Seq[String] ) extends GeneralResponse // sent to all users

    case class GameTurnVoteForCard( cardId:String ) extends GeneralRequest // user chooses a best card

    // how much votes summary for every cards,  who voted for what
    case class GameTurnVoteResults( voteResults: Map[String,Int], userVotes:Map[String,BSONObjectID], userPoints: Map[String,Int] ) extends GeneralRequest

    // next game turn starts

    implicit val gameCardFormat = Json.format[GameCard]
    implicit val requestMessageFormat: Format[GeneralRequest] = Variants.format[GeneralRequest]("event")
    implicit val responseMessageFormat: Format[GeneralResponse] = Variants.format[GeneralResponse]("event")


  }
}