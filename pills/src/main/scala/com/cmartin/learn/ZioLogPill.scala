package com.cmartin.learn

import zio.*
import zio.logging.LogAnnotation
import zio.logging.LogFormat
import zio.logging.backend.SLF4J

import java.util.UUID

object ZioLogPill
    extends ZIOAppDefault:

  val logger = Runtime.addLogger(
    SLF4J.slf4jLogger(SLF4J.logFormatDefault, SLF4J.getLoggerName())
  )

  val loggerLayer = zio.Runtime.removeDefaultLoggers >>> logger

  val correlationIdAspect = ZIOAspect.annotated(("request-id", UUID.randomUUID.toString))

  override val bootstrap = loggerLayer

  def run =
    for
      _ <- ZIO.logDebug(s"this is a ${LogLevel.Debug.label} level trace")
      _ <- ZIO.logInfo(s"this is an ${LogLevel.Info.label} level trace") @@ correlationIdAspect
      _ <- ZIO.logError(s"this is an ${LogLevel.Error.label} level trace")
    yield ()
