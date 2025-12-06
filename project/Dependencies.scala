import sbt.*

object Dependencies {

  lazy val zio_group = "dev.zio"
  // ZIO and ecosystem
  lazy val zio        = zio_group %% "zio"               % Versions.zio
  lazy val zioStreams = zio_group %% "zio-streams"       % Versions.zio
  lazy val zioLogging = zio_group %% "zio-logging-slf4j" % Versions.zioLogging
  lazy val zioPrelude = zio_group %% "zio-prelude"       % Versions.zioPrelude

  // testing code
  lazy val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalatest % Test

  lazy val zioTest: Seq[ModuleID] = Seq(
    zio_group %% "zio-test"          % Versions.zio % "test",
    zio_group %% "zio-test-sbt"      % Versions.zio % "test",
    zio_group %% "zio-test-magnolia" % Versions.zio % "test" // optional
  )
}
