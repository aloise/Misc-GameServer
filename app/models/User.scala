package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._

case class User(
  id: Option[BSONObjectID],
  username: String,
  created: DateTime,
  password:String
//  @Key("company_id")company: Option[ObjectId] = None
)

object User extends Model{

}