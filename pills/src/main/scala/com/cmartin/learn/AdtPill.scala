package com.cmartin.learn

object AdtPill {
  enum ResponseError:
    case BadRequest(message: String)
    case NotFound(message: String)
    case Conflict(message: String)

  enum CommandResult(code: Int):
    case Succeed         extends CommandResult(0)
    case InvalidVariable extends CommandResult(1)
    case ServiceError    extends CommandResult(2)
    case WebClientError  extends CommandResult(3)
}
