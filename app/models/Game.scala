package models

import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.MongoContext._


case class Game (
  id: ObjectId = new ObjectId,
  application:ObjectId,
  subtypeId:Int,
  status:String,
  created:Date = new Date(),
  started:Option[Date],
  finished:Option[Date],
  gameProfiles:List[GameProfile] = Nil
)

object Game extends ModelCompanion[Game, ObjectId] {
  val dao = new SalatDAO[Game, ObjectId](collection = mongoCollection("games")) {}
}