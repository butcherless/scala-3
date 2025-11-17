import sbt.*

object Dependencies {

  // ZIO and ecosystem
  lazy val zio        = "dev.zio" %% "zio"               % Versions.zio
  lazy val zioStreams = "dev.zio" %% "zio-streams"       % Versions.zio
  lazy val zioLogging = "dev.zio" %% "zio-logging-slf4j" % Versions.zioLogging
  lazy val zioPrelude = "dev.zio" %% "zio-prelude"       % Versions.zioPrelude

  // testing code
  lazy val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalatest % Test

  lazy val zioTest: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-test"          % Versions.zio % "test",
    "dev.zio" %% "zio-test-sbt"      % Versions.zio % "test",
    "dev.zio" %% "zio-test-magnolia" % Versions.zio % "test" // optional
  )
}
