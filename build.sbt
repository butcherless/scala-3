import Dependencies._

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "3.3.1"
ThisBuild / organization := "com.cmartin.learn"

lazy val basicScalacOptions = Seq(
  "-deprecation",
  "-encoding",
  "utf-8",
  "-explain",
  "-explaintypes",
  "-feature",
  "-unchecked",
//  "-language:postfixOps",
  "-language:higherKinds",
  "-Wunused:imports"
)

lazy val commonSettings = Seq(
  scalacOptions ++= basicScalacOptions,
  libraryDependencies ++= Seq(scalaTest)
  // resolvers += // temporal for ZIO snapshots
  //  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
)

lazy val pills = (project in file("pills"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      zio,
      zioPrelude,
      zioLogging,
      scalaTest
    ),
    coverageEnabled := false
  )

  lazy val `pills-int` = (project in file("pills-int"))
  .dependsOn(pills)
  .settings(
    name           := "pills-int",
    publish / skip := true,
    commonSettings
    // test dependencies
    // libraryDependencies += something % Test,
  )

// clear screen and banner
lazy val cls = taskKey[Unit]("Prints a separator")
cls := {
  val brs     = "\n".repeat(2)
  val message = "* B U I L D   B E G I N S   H E R E *"
  val chars   = "*".repeat(message.length())
  println(s"$brs$chars")
  println("* B U I L D   B E G I N S   H E R E *")
  println(s"$chars$brs ")
}

addCommandAlias("xcoverage", "clean;coverage;test;coverageReport")
addCommandAlias("xreload", "clean;reload")
addCommandAlias("xstart", "clean;reStart")
addCommandAlias("xstop", "reStop;clean")
addCommandAlias("xupdate", "clean;update")
addCommandAlias("xdup", "dependencyUpdates")
