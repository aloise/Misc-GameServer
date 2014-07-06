package models

import play.api.Play.current
import org.joda.time.DateTime
import reactivemongo.bson._

case class Message (
  _id: BSONObjectID = BSONObjectID.generate,
  sender:BSONObjectID, // GameProfile
  recipient:List[BSONObjectID], // GameProfile
  game: BSONObjectID, // Game
  messageType: String,
  created:DateTime,
  data: BSONDocument
)

object Messages extends Collection[Message]("messages"){

}