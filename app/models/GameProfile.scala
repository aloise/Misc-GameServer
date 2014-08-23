package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json.Json


// There is no intention to keep it as separate collection - It's included inside the Game
case class GameProfile (
  _id: BSONObjectID = BSONObjectID.generate,
  applicationProfileId:BSONObjectID,
  userId: BSONObjectID,
  status:String = GameProfiles.Status.pending,
  rating:Int = 0, // rating delta from this game
  karma:Int = 0, // rating karma delta from this game
  created:DateTime = new DateTime,
  completed:Option[DateTime] = None
)

object GameProfiles extends Collection[GameProfile]("game_profiles"){

  object Status {
    val pending = "pending"
    val inProgress = "in_progress"
    val completed = "completed"
  }

  implicit val format = Json.format[GameProfile]



}