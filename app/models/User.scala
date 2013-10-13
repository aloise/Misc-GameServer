package models

import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.MongoContext._
import akka.util.Crypt


case class User(
  id: ObjectId = new ObjectId,
  username: String,
  created: Date = new Date(),
  password:String
//  @Key("company_id")company: Option[ObjectId] = None
)

object User extends ModelCompanion[User, ObjectId] {
  val dao = new SalatDAO[User, ObjectId](collection = mongoCollection("users")) {}

  def findOneByUsername(username: String): Option[User] = dao.findOne(MongoDBObject("username" -> username))

  // TODO implement the real auth
  def logIn(username:String, password:String): Option[User] = dao.findOne(MongoDBObject("username" -> username, "password" -> Crypt.sha1( password)))

  def findById(id:ObjectId):Option[User] = dao.findOneById(id)

}