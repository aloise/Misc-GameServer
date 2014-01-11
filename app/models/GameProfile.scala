package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json.Json

case class GameProfile (
  id: Option[BSONObjectID],
  profile:BSONObjectID,
  status:String,
  rating:Int,
  karma:Int,
  created:DateTime = new DateTime,
  completed:DateTime
)

object GameProfiles extends Collection[GameProfile]("game_profiles"){

  implicit val format = Json.format[GameProfile]

}