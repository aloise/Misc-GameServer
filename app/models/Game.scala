package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._
import play.api.libs.json.{JsValue, JsNull, Json}
import play.modules.reactivemongo.json.BSONFormats._



case class Game (
  _id: BSONObjectID = BSONObjectID.generate,
  applicationId:BSONObjectID,
  `type`:Int = 0 ,
  status:String = Games.Status.Waiting,
  created:DateTime = new DateTime(),
  started:Option[DateTime] = None,
  finished:Option[DateTime] = None,
  creatorGameProfileId: Option[BSONObjectID], // points on Game.gameProfiles record
  gameProfiles:List[GameProfile] = Nil,

  karmaRestrict:Int = 0,
  ratingRestrict:Int = 0,

  speed:Int,
  playersMaxCount:Int = 0,

  welcomeMessage:String = "",

  data:JsValue = JsNull
)

object Games extends Collection[Game]("games") {

  object Status {
    val Waiting = "WAITING"
    val Active = "ACTIVE"
    val Closed = "CLOSED"
    val Finished = "FINISHED"
  }

  implicit val gameProfileFormat = GameProfiles.format

  implicit val format = Json.format[Game]

}