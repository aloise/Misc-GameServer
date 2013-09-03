package controllers

import play.api._
import play.api.mvc._
import models.{Game, Profile, Message, User}
import org.bson.types.ObjectId
import java.util.Date
import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject

object TestController extends Controller {

  def index = Action {
    Ok(views.html.test.index())
  }

/*  def addUser = Action {

    // val user = User( username = "aloise")
//    User.insert(user)
    val app = models.application(new ObjectId(), "app" )

    val profile = Profile(
      new ObjectId(),
      User(username="test"),
      app,
      new Date,
      new Date,
      0,
      0,
      0
    )

    Message.insert(
      Message(
        new ObjectId(),
        profile,
        List( profile, profile, profile ),
        Game(new ObjectId(), app, 0, "pending", new Date(), None, None, List(profile, profile, profile)  ),
        "test",
        new Date(),
        MongoDBObject( "a" -> "b", "c" -> "d", "e" -> 100200 )
      )
    )

    Ok(views.html.test.addUser())
  }*/

  def listUsers = Action {

    Ok(views.html.test.listUsers())
  }

}