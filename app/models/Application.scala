package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._
import play.api.libs.json._
import reactivemongo.api.collections.GenericQueryBuilder
import play.modules.reactivemongo.json.BSONFormats._
import akka.actor.Props

case class Application(
  _id: BSONObjectID = BSONObjectID.generate,
  name:String,
  gid:String,
  data:JsValue
) {



}

object Applications extends Collection[Application]("applications") {

  implicit val format = Json.format[Application]


}


