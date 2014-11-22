package actors.applications.dixie

import akka.actor.ActorRef
import akka.actor._
import org.joda.time.DateTime
import scala.collection.mutable
import actors.messages.UserSession._
import actors.messages._
import models.ApplicationProfile
import play.api.libs.json.{JsObject, JsValue, JsString, Json}
import scala.concurrent.Future
import reactivemongo.bson.BSONObjectID
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.json.BSONFormats._
import scala.util._
import actors.Game

/**
 * User: aloise
 * Date: 22.11.14
 * Time: 22:08
 */
class DixieGame(application:ActorRef, game:models.Game) extends Game(application, game) {


  override def receive = gameMessageReceive orElse super.receive


  def gameMessageReceive:Receive = {
    case _ =>
  }
}
