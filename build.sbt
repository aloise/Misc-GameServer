import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._

name := "GameServer"

version := "1.1-SNAPSHOT"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.0-SNAPSHOT",
  "io.backchat.inflector" %% "scala-inflector" % "1.3.5"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)