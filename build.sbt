import sbt._

name          := "pub-trans-fetch-analyze"

version       := "0.1"

scalaVersion  := Version.scala

resolvers ++= Seq(
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)

libraryDependencies ++= Dependencies.spraySlick

releaseSettings

scalariformSettings

Revolver.settings