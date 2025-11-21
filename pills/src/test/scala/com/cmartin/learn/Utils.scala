package com.cmartin.learn

import zio.Runtime.default as runtime
import zio.{Unsafe, ZIO}

object Utils:

  def runProgram[E, A](program: ZIO[Any, E, A]) =
    Unsafe.unsafe { implicit us =>
      runtime.unsafe.run(
        program
      ).getOrThrowFiberFailure()
    }
