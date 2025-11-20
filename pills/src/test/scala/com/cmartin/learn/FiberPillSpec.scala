package com.cmartin.learn

import com.cmartin.learn.FiberPill.MAX_DELAY_MILLIS
import zio.durationInt
import zio.test.*

import java.util.UUID

object FiberPillSpec extends ZIOSpecDefault:

  def spec = suite("FiberPillSpec")(
    test("process should return the same UUID as input") {
      val input = FiberPill.ProcessInput(UUID.randomUUID(), "test")
      for
        result <- FiberPill.process(input).fork
        _      <- TestClock.adjust(MAX_DELAY_MILLIS.milliseconds)
        result <- result.join
      yield assertTrue(result == input.id)
    }
  )
