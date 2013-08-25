package models

import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.MongoContext._

case class GameProfile (
  id: ObjectId = new ObjectId,
  game:Game,
  profile:Profile,
  status:String,
  rating:Int,
  karma:Int,
  created:Date = new Date,
  completed:Date
)

object GameProfile extends ModelCompanion[GameProfile, ObjectId] {
  val dao = new SalatDAO[GameProfile, ObjectId](collection = mongoCollection("game_profiles")) {}
}