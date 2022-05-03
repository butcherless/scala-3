package com.cmartin.learn

import zio.*
import zio.logging.LogAnnotation
import zio.logging.LogFormat
import zio.logging.backend.SLF4J

object ZioLogPill
    extends ZIOAppDefault:

  /* TODO zio-logging with zio-rc6 dependency
  val logAspect = ???
     SLF4J.slf4j(
      logLevel = LogLevel.Debug,
      format = LogFormat.line,
      rootLoggerName = _ => "com.cmartin.learn"
    )
   */

  // TODO zio-logging with zio-rc6 dependency
  // def bootstrap = ???
  // RuntimeConfigAspect(_ => runtime.runtimeConfig.copy(logger = ZLogger.none)) >>> logAspect

  def run =
    for
      _ <- ZIO.logDebug("this is a DEBUG level trace")
      _ <- ZIO.logInfo("this is an INFO level trace") @@ ZIOAspect.annotated(("a", "b"))
      _ <- ZIO.logError("this is an ERROR level trace")
    yield ()
