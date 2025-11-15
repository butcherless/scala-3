package com.cmartin.learn

import java.net.URI
import scala.util.matching.Regex

object CollectionsPill {

  // Global set of valid names
  private val VALID_NAMES: Set[String] = Set(
    "ALPHA",
    "BRAVO",
    "CHARLIE"
  )

  // Regex pattern with groups to extract NAME and UUID
  private val URN_PATTERN: Regex = """^urn:([A-Z]+):([a-f0-9\-]{36})$""".r

  private case class MatchResult(uri: URI, name: String = "") {
    def isValid: Boolean = name.nonEmpty
  }

  /** Classifies a list of URIs by their NAME component.
    *
    * @param uris
    *   List of URIs to classify
    * @return
    *   Map where keys are NAMEs and values are Lists of URIs
    */
  def classify(uris: Seq[URI]): Map[String, Seq[URI]] = {
    uris
      .map(matchUri)
      .filter(_.isValid) // Keep only valid matches
      .groupMap(_.name)(_.uri) // Classify by name, extracting URI
  }

  // Match URI string against pattern and extract name from group(1)
  private def matchUri(uri: URI): MatchResult = {
    uri.toString match {
      case URN_PATTERN(name, uuid) if VALID_NAMES.contains(name) =>
        MatchResult(uri, name)
      case _                                                     =>
        MatchResult(uri) // Invalid, name will be empty
    }
  }

}
