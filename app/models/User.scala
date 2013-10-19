package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._
import play.api.Play
import akka.util.Crypt
import play.api.libs.json._
import reactivemongo.api.Cursor
import play.api.libs.iteratee.Iteratee
import scala.concurrent.Future
import play.modules.reactivemongo.json.BSONFormats._


case class User(
  id: Option[BSONObjectID],
  uid:Int, // external user id
  username: String,
  created: DateTime,
  password:String
//  @Key("company_id")company: Option[ObjectId] = None
)

object Users extends Collection[User]("users"){

  private val secret = Play.configuration.getString("users.secret")

  val jsonFormat = Json.format[User]

  def authenticate(uid:Int, signature:String):Future[Option[User]] = {

    if( Crypt.sha1( uid.toString + secret ) == signature ){

      val found = collection.find(Json.obj( "uid" -> uid )).one[JsValue]

      found.map{
        case Some(doc) =>
            jsonFormat.reads(doc) match {
              case JsSuccess(u:User,_) => Some(u)
              case _ => None
            }
        case None => None
       }


    } else {
      Future.successful(None)
    }

  }

}