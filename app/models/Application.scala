package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._
import play.api.libs.json.{Writes, Reads, JsObject, Json}
import reactivemongo.api.collections.GenericQueryBuilder
import play.modules.reactivemongo.json.BSONFormats._

case class Application(
  id: Option[BSONObjectID],
  name:String
)

object Applications extends Collection[Application]("applications") {



  def format = Json.format[Application]


}