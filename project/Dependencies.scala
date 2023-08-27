import sbt._

object Dependencies {

  // ZIO and ecosystem
  lazy val zio        = "dev.zio" %% "zio"               % Versions.zio
  lazy val zioLogging = "dev.zio" %% "zio-logging-slf4j" % Versions.zioLogging
  lazy val zioPrelude = "dev.zio" %% "zio-prelude"       % Versions.zioPrelude

  // testing code
  lazy val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalatest % Test
}
