package com.cmartin.learn.collection

import java.net.URI
import scala.util.matching.Regex

object ChunkedCollectionPill {

  // Regex pattern with groups to extract NAME and UUID
  private val URN_PATTERN: Regex = """^urn:([A-Z]+):([a-f0-9\-]{36})$""".r

  // Set of valid type names
  enum Color:
    case RED, ORANGE, YELLOW, GREEN, BLUE, INDIGO, VIOLET

  val typeLimitMap: Map[Color, Int] = Map(
    Color.RED    -> 4,
    Color.ORANGE -> 3,
    Color.YELLOW -> 2,
    Color.GREEN  -> 4,
    Color.BLUE   -> 4,
    Color.INDIGO -> 2,
    Color.VIOLET -> 1
  )

  case class MatchResult(name: Color, uri: URI)

  def hasValidName(name: String): Boolean =
    Color.values.exists(_.toString == name)

  // TODO: return valid and invalid URIs
  case class Result(
      validUris: Map[Color, List[URI]],
      invalidUris: List[URI]
  )

  // Match URI string against a pattern and extract name from group(1)
  def matchUri(uri: URI): Option[MatchResult] =
    uri.toString match
      case URN_PATTERN(name, uuid) if hasValidName(name) =>
        Some(MatchResult(Color.valueOf(name), uri))
      case _                                             =>
        None // No match, invalid URI

  // Classify a list of URIs by their NAME component
  def classify(uris: Seq[URI]): Map[Color, Seq[URI]] =
    uris
      .flatMap(matchUri)
      .groupMap(a => a.name)(a => a.uri)

  // create a partition for each color based on the typeLimitMap
  def chunk(classifiedUris: Map[Color, Seq[URI]]): Map[Color, Seq[Seq[URI]]] =
    classifiedUris.map { case (color, uris) =>
      val limit  = typeLimitMap.getOrElse(color, 1) // Default to 1 if color not found
      val chunks = uris.grouped(limit).toSeq
      (color, chunks)
    }

  // create a sequence of partitions with the i-th index element of each color partition from 1 to N,
  // e.g., the first element of red plus the first element of orange,
  //  the second element of red plus the second element of orange, etc.
  // not all partitions will have the same size
  def groupByIndex(chunkMap: Map[Color, Seq[Seq[URI]]]): Seq[Seq[URI]] =
    val maxChunks = chunkMap.values.map(_.size).max
    (0 until maxChunks).map { index =>
      chunkMap
        .values
        .flatMap(chunks => chunks.lift(index))
        .flatten
        .toSeq
    }

}
