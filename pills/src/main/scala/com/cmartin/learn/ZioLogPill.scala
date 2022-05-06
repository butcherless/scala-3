package com.cmartin.learn

import zio.*
import zio.logging.LogAnnotation
import zio.logging.LogFormat
import zio.logging.backend.SLF4J

import java.util.UUID

object ZioLogPill
    extends ZIOAppDefault:

  val logLayer =
    SLF4J.slf4j(
      logLevel = LogLevel.Debug,
      format = LogFormat.line,
      rootLoggerName = _ => "com.cmartin.learn"
    )

  val correlationIdAspect = ZIOAspect.annotated(("request-id", UUID.randomUUID.toString))

  override val bootstrap = logLayer

  def run =
    for
      _ <- ZIO.logDebug(s"this is a ${LogLevel.Debug.label} level trace")
      _ <- ZIO.logInfo(s"this is an ${LogLevel.Info.label} level trace") @@ correlationIdAspect
      _ <- ZIO.logError(s"this is an ${LogLevel.Error.label} level trace")
    yield ()
