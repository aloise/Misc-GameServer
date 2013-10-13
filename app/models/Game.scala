package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._

case class Game (
  id: Option[BSONObjectID],
  application:BSONObjectID,
  subtypeId:Int,
  status:String,
  created:DateTime,
  started:Option[DateTime],
  finished:Option[DateTime],
  gameProfiles:List[GameProfile] = Nil
)

object Game extends Model {

}