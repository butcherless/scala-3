package com.cmartin.learn

import zio.{Unsafe, ZIO}
import zio.Runtime.default as runtime

object Utils:

  def runProgram[E, A](program: ZIO[Any, E, A]) =
    Unsafe.unsafe { implicit us =>
      runtime.unsafe.run(
        program
      ).getOrThrowFiberFailure()
    }
