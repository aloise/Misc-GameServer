import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "GameServer"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    // jdbc,
    // anorm,
     "se.radley" %% "play-plugins-salat" % "1.2",
     "com.novus" %% "salat" % "1.9.2",
     "com.imaginea" % "socket.io.play_2.9.1" % "0.0.5-SNAPSHOT"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    routesImport += "se.radley.plugin.salat.Binders._",
    templatesImport += "org.bson.types.ObjectId",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += "OSS Repo" at "https://oss.sonatype.org/content/repositories/snapshots"
  )

}
