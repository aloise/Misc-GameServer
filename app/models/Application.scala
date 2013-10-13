package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._

case class Application(
  id: Option[BSONObjectID],
  name:String
)

object Application extends Model {

}