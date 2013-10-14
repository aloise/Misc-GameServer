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
//     "com.jolbox" % "bonecp" % "0.8.0-rc1",
//     "org.reflections" % "reflections" % "0.9.8",
//     "se.radley" %% "play-plugins-salat" % "1.2",
//     "com.novus" %% "salat" % "1.9.2",
     "org.reactivemongo" %% "play2-reactivemongo" % "0.9",
     "io.backchat.inflector" %% "scala-inflector" % "1.3.5"
     // "com.imaginea" % "socket.io.play_2.9.1" % "0.0.5-SNAPSHOT"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
//    routesImport += "se.radley.plugin.salat.Binders._",
//    templatesImport += "org.bson.types.ObjectId",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += "OSS Repo" at "https://oss.sonatype.org/content/repositories/snapshots"
  )

}
