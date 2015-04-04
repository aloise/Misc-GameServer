//import sbt._
//import Keys._
//import play.Play.autoImport._
//import PlayKeys._

name := "GameServer"

version := "1.3"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",
  "io.backchat.inflector" %% "scala-inflector" % "1.3.5",
  "org.julienrf" %% "play-json-variants" % "1.1.0"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)