package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._

case class GameProfile (
  id: Option[BSONObjectID],
  game:Game,
  profile:BSONObjectID,
  status:String,
  rating:Int,
  karma:Int,
  created:DateTime = new DateTime,
  completed:DateTime
)

object GameProfile extends Model{

}