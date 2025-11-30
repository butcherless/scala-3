package com.cmartin.learn

import java.net.URI
import scala.util.matching.Regex

object CollectionsPill:

  // Global set of valid names
  enum UriType:
    case ALPHA
    case BRAVO
    case CHARLIE

  // Regex pattern with groups to extract NAME and UUID
  private val URN_PATTERN: Regex = """^urn:([A-Z]+):([a-f0-9\-]{36})$""".r

  private case class MatchResult(uri: URI, uriType: UriType)

  /** Classifies a list of URIs by their NAME component.
    *
    * @param uris
    *   List of URIs to classify
    * @return
    *   Map where keys are NAMEs and values are Lists of URIs
    */
  def classify(uris: Seq[URI]): Map[UriType, Seq[URI]] =
    uris
      .flatMap(matchUri)               // Keep only valid matches
      .map(mr => (mr.uriType, mr.uri)) // Safe to call .get after isValid filter
      .groupMap(_._1)(_._2)            // Classify by name, extracting URI

  // Match URI string against pattern and extract name from group(1)
  private def matchUri(uri: URI): Option[MatchResult] =
    uri.toString match
      case URN_PATTERN(name, uuid) if isValidType(name) =>
        Some(MatchResult(uri, UriType.valueOf(name)))
      case _                                            =>
        None // Invalid, will be empty

  private def isValidType(name: String): Boolean =
    UriType.values.exists(_.toString == name)
