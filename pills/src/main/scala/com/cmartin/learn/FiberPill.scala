package com.cmartin.learn

import zio.UIO
import zio.Random
import zio.ZIO
import zio.durationInt

import java.util.UUID

object FiberPill:
  private val MIN_DELAY_MILLIS = 10
  val MAX_DELAY_MILLIS         = 1000

  case class ProcessInput(id: UUID, name: String)

  def process(input: ProcessInput): UIO[UUID] =
    for
      threadId <- ZIO.succeed(Thread.currentThread().getName)
      delay    <- Random.nextIntBetween(MIN_DELAY_MILLIS, MAX_DELAY_MILLIS)
      _        <- ZIO.sleep(delay.milliseconds)
      _        <- ZIO.logInfo(s"processed $input with delay $delay milliseconds by thread $threadId")
    yield input.id
