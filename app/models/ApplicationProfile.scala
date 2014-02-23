package models


import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json.Json


case class ApplicationProfile (
  id: Option[BSONObjectID],
  userId:BSONObjectID, // User
  applicationId: BSONObjectID, // application
  created:DateTime,
  lastSeen:DateTime,
  platform:Int = 0,
  rating:Int = 0,
  karma:Int = 0
)

object ApplicationProfiles extends Collection[ApplicationProfile]("application_profiles"){
  implicit val jsonFormat = Json.format[ApplicationProfile]

  def getForUser( app:Application, user:User ) =
    ApplicationProfile( Some(BSONObjectID.generate), user.id.get, app.id.get, new DateTime(), new DateTime, 0, 0, 0 )

}