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
     "org.reactivemongo" %% "play2-reactivemongo" % "0.9" exclude("org.scala-stm", "scala-stm_2.10.0"),
     "io.backchat.inflector" %% "scala-inflector" % "1.3.5" exclude("org.scala-stm", "scala-stm_2.10.0")

  )


  val main = play.Project(appName, appVersion, appDependencies).settings(

    // Add your own project settings here
    scalaVersion := "2.10.3",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += "OSS Repo" at "https://oss.sonatype.org/content/repositories/snapshots"
  )

}
