package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats._



case class Game (
  id: Option[BSONObjectID],
  applicationId:BSONObjectID,
  subtypeId:Int,
  status:String,
  created:DateTime,
  started:Option[DateTime],
  finished:Option[DateTime],
  gameProfiles:List[GameProfile] = Nil
)

object Games extends Collection[Game]("games") {

  implicit val gameProfileFormat = GameProfiles.format

  implicit val format = Json.format[Game]

}