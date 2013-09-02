package models

import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.MongoContext._

case class Message (
  id: ObjectId = new ObjectId,
  sender:ObjectId, // GameProfile
  recipient:List[ObjectId], // GameProfile
  game: ObjectId, // Game
  messageType: String,
  created:Date = new Date,
  data: DBObject
)

object Message extends ModelCompanion[Message, ObjectId] {
  val dao = new SalatDAO[Message, ObjectId](collection = mongoCollection("messages")) {}
}