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
import play.api.libs.concurrent.Execution.Implicits._


case class User(
  _id: BSONObjectID = BSONObjectID.generate,
  uid:Int, // external user id
  name: String,
  avatar: Option[String] = None,
  created: DateTime = new DateTime()

//  password:String
//  @Key("company_id")company: Option[ObjectId] = None
)

object Users extends Collection[User]("users"){

  private val secret = Play.configuration.getString("users.secret").getOrElse("aloise")

  implicit val jsonFormat = Json.format[User]

  def authenticateOrCreate(uid:Int, signature:String, username:Option[String] = None, avatar:Option[String] = None):Future[Option[User]] = {

    if( Crypt.sha1( uid.toString + secret ).toLowerCase == signature.trim.toLowerCase ){

      val found = collection.find(Json.obj( "uid" -> uid )).one[JsValue]

      found.flatMap {
        case Some(doc) =>
            jsonFormat.reads(doc) match {
              case JsSuccess(u:User,_) => Future.successful( Some(u) )
              case _ => Future.successful( None )
            }
        case None =>

          // create a user
          val newUser = User( BSONObjectID.generate, uid, username.getOrElse("user_"+uid), avatar )
          collection.
            insert(  jsonFormat.writes( newUser ).as[JsObject] ).
            map{ lastError => if( lastError.ok ) Some(newUser) else throw lastError }
      }


    } else {
      Future.successful(None)
    }

  }

}