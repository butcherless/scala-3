package com.cmartin.learn

import zio.Runtime.{default => runtime}
import zio.{Unsafe, ZIO}

object Utils {
// val runtime = Runtime.default
  def runProgram[E, A](program: ZIO[Any, E, A]) =
    Unsafe.unsafe {
      runtime.unsafe.run(
        program
      ).getOrThrowFiberFailure()
    }
}
