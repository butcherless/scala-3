package com.cmartin.learn

import zio.*
import zio.logging.{LogAnnotation, LogFormat}
import zio.logging.backend.SLF4J

object ZioLogPill
    extends ZIOAppDefault:

  val logAspect: RuntimeConfigAspect     =
    SLF4J.slf4j(
      logLevel = LogLevel.Debug,
      format = LogFormat.line,
      rootLoggerName = _ => "com.cmartin.learn"
    )
  override def hook: RuntimeConfigAspect =
    RuntimeConfigAspect(_ => runtime.runtimeConfig.copy(logger = ZLogger.none)) >>> logAspect

  override def run =
    for
      _ <- ZIO.logDebug("this is a DEBUG level trace")
      _ <- ZIO.logInfo("this is an INFO level trace") @@ ZIOAspect.annotated(("a", "b"))
      _ <- ZIO.logError("this is an ERROR level trace")
    yield ()
