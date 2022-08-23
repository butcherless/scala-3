package com.cmartin.learn

import zio.*
import zio.logging.backend.SLF4J

import java.util.UUID

object ZioLogPill
    extends ZIOAppDefault:

  case class User(id: Long, name: String)

  val loggerLayer = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  def getUser(): Task[User] =
    ZIO.attempt(User(12345678L, "cmartin"))
  def run                   =
    (
      for
        user <- getUser()
        _    <- ZIO.logDebug(s"this is an info log") @@ ZIOAspect.annotated("user", s"${user.id}")
      yield ()
    ).provide(loggerLayer)
