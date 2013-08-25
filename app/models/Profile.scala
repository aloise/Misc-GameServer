package models

import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.MongoContext._

case class Profile (
  id: ObjectId = new ObjectId,
  user:User,
  application:Application,
  created:Date = new Date,
  lastSeen:Date,
  platform:Int,
  rating:Int,
  karma:Int
)

object Profile extends ModelCompanion[Profile, ObjectId] {
  val dao = new SalatDAO[Profile, ObjectId](collection = mongoCollection("profiles")) {}
}
