package models

import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.MongoContext._

case class Application(
  id: ObjectId = new ObjectId,
  name:String
)

object Application extends ModelCompanion[Application, ObjectId] {
  val dao = new SalatDAO[Application, ObjectId](collection = mongoCollection("applications")) {}
}