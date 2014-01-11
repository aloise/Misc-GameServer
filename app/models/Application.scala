package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._
import play.api.libs.json.{Writes, Reads, JsObject, Json}
import reactivemongo.api.collections.GenericQueryBuilder
import play.modules.reactivemongo.json.BSONFormats._
import akka.actor.Props

case class Application(
  id: Option[BSONObjectID],
  name:String,
  gid:String
) {



}

object Applications extends Collection[Application]("applications") {



  implicit val format = Json.format[Application]


}


