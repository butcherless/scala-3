package com.cmartin.learn

import com.cmartin.learn.ConcurrencyPill.DomainError
import zio._

object ConcurrencyPill {
  enum DomainError:
    case ProcessingError(message: String)
  // case E2(message: String)

  def doProcess(text: String)(delay: Int) =
    ZIO.attempt(text)
      .delay(delay.milliseconds)

  def doFailProcess(text: String)(delay: Int) =
    ZIO.fail(DomainError.ProcessingError(text))
      .delay(delay.milliseconds)

}
