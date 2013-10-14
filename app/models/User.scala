package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._
import play.api.Play
import akka.util.Crypt
import play.api.libs.json.{Writes, Reads, JsObject}

case class User(
  id: Option[BSONObjectID],
  uid:Int, // external user id
  username: String,
  created: DateTime,
  password:String
//  @Key("company_id")company: Option[ObjectId] = None
)

object Users extends Collection[User]("users"){


  def authenticate(uid:Int, signature:String) = {
    val secret = Play.configuration.getString("users.secret")
    if( Crypt.sha1( uid.toString + secret ) == signature ){
      val query = BSONDocument( "uid" -> uid )
      //val f: GenericQueryBuilder[JsObject, Reads, Writes] = collection.find(query)
//      f.toList.map( _ )
    } else {

    }
  }

}