package common

import actors.Gateway
import akka.actor.Props
import play.api._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import models.Applications.format
import play.api.libs.json._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

object Global extends GlobalSettings {

  lazy val gateway = Akka.system.actorOf(Props[Gateway])

  override def onStart(app: Application) {
//    Logger.info("Application has started")

    // load all application in the gateway

    val apps = models.Applications.find(Json.obj())

    apps.foreach{ app =>
      println( app )
    }


  }

  override def onStop(app: Application) {
//    Logger.info("Application shutdown...")
  }

}
