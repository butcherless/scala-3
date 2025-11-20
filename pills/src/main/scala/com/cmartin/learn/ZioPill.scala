package com.cmartin.learn

import zio.Runtime

import java.time.Instant
import java.util.UUID

object ZioPill:
  val runtime: Runtime[Any] = Runtime.default

  sealed trait BaseError:
    val message: String

  sealed trait RepositoryError extends BaseError

  case class MissingEntityError(message: String) extends RepositoryError

  case class DuplicateEntityError(message: String) extends RepositoryError

  sealed trait ViewError extends BaseError

  case class NotFoundError(message: String) extends ViewError

  case class BadRequestError(message: String) extends BaseError

  case class UnknownError(message: String) extends BaseError

  def manageError(e: BaseError): String =
    e match
      case MissingEntityError(_) => "missing"
      case NotFoundError(_)      => "not-found"
      case UnknownError(_)       => "unknown"
      case _                     => "default"

  def manageRepositoryError(e: RepositoryError): String =
    e match
      case MissingEntityError(_)   => "missing"
      case DuplicateEntityError(_) => "duplicate"

  case class Location(
      lon: Double,
      lat: Double
  )

  case class Address(
      name: String,
      number: Option[Int]
  )

  case class MessageDbo(
      id: UUID,
      date: Instant,
      data: String,
      location: Location
  )
