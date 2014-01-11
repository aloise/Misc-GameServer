package models


import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._


case class Profile (
  id: Option[BSONObjectID],
  user:BSONObjectID, // User
  application: BSONObjectID, // application
  created:DateTime,
  lastSeen:DateTime,
  platform:Int = 0,
  rating:Int = 0,
  karma:Int = 0
)

object Profiles extends Collection[Profile]("profiles"){

}