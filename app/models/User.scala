package models

import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.MongoContext._



case class User(
  id: ObjectId = new ObjectId,
  username: String,
  created: Date = new Date()
//  @Key("company_id")company: Option[ObjectId] = None
)

object User extends ModelCompanion[User, ObjectId] {
  val dao = new SalatDAO[User, ObjectId](collection = mongoCollection("users")) {}

  def findOneByUsername(username: String): Option[User] = dao.findOne(MongoDBObject("username" -> username))
  def findById(id:ObjectId):Option[User] = dao.findOneById(id)

}