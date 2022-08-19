package com.cmartin.learn

import zio.*
import zio.logging.{LogAnnotation, LogFormat}
import zio.logging.backend.SLF4J

import java.util.UUID

object ZioLogPill
    extends ZIOAppDefault:

  val loggerLayer = zio.Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  val correlationIdAspect = ZIOAspect.annotated(("request-id", UUID.randomUUID.toString))

  override val bootstrap = loggerLayer

  def run =
    for
      _ <- ZIO.logDebug(s"this is a ${LogLevel.Debug.label} level trace")
      _ <- ZIO.logInfo(s"this is an ${LogLevel.Info.label} level trace")
      _ <- ZIO.logError(s"this is an ${LogLevel.Error.label} level trace")
    yield ()
