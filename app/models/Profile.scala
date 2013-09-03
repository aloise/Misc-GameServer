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
  user:ObjectId, // User
  application: ObjectId, // application
  created:Date = new Date,
  lastSeen:Date = new Date,
  platform:Int = 0,
  rating:Int = 0,
  karma:Int = 0
)

object Profile extends ModelCompanion[Profile, ObjectId] {
  val dao = new SalatDAO[Profile, ObjectId](collection = mongoCollection("profiles")) {}
}
