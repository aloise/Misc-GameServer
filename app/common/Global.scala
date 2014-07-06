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
    models.Applications.find(Json.obj()).map{ apps =>
      apps.foreach{ app =>
        gateway ! Gateway.ApplicationCreate( app.gid )
      }

    }


  }

  override def onStop(app: Application) {
//    Logger.info("Application shutdown...")
  }

}
