package actors.applications.dixie

import actors.applications.dixie.DixieApplication.GameCard
import akka.actor.ActorRef
import akka.actor._
import org.joda.time.DateTime
import scala.collection.mutable
import actors.messages.UserSession._
import actors.messages._
import models.{GameProfile, ApplicationProfile}
import play.api.libs.json.{JsObject, JsValue, JsString, Json}
import scala.concurrent.Future
import reactivemongo.bson.BSONObjectID
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.json.BSONFormats._
import scala.util._
import actors.Game
import scala.concurrent.duration._

/**
 * User: aloise
 * Date: 22.11.14
 * Time: 22:08
 */
class DixieGame(application:ActorRef, game:models.Game, cards: Map[String,GameCard]) extends Game(application, game) {

  import DixieApplication._
  import DixieApplication.Messages._

  val userTurnAwaitTime = 300.seconds// seconds - read from config - system select for himself randomly
  var currentTurn:Byte = 0


  override def receive = beforeGameStart orElse super.receive

  def beforeGameStart:Receive = {
    case _ =>
  }


  def gameMessageReceive:Receive = {
    case _ =>
  }

  def isUserAllowedToJoin(session: UserSession, applicationProfile: ApplicationProfile, gameProfile: GameProfile): Future[Boolean] = {
    Future.successful(true)
  }
}