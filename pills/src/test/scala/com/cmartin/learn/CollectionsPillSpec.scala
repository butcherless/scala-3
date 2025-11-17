package com.cmartin.learn

import com.cmartin.learn.CollectionsPill.classify
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI

// word list file: https://github.com/JorgeDuenasLerin/diccionario-espanol-txt/blob/master/0_palabras_todas_no_conjugaciones.txt
class CollectionsPillSpec
    extends AnyFlatSpec
    with Matchers:

  behavior of "CollectionsPill"

  val alphaOneURI: URI   = URI.create("urn:ALPHA:104f9b76-25ef-4603-812c-440b622f4f65")
  val alphaTwoURI: URI   = URI.create("urn:ALPHA:931ddab3-a267-4a92-98ce-1f4e1933dd18")
  val bravoOneURI: URI   = URI.create("urn:BRAVO:4d711456-0af7-47c9-a738-1fff7ec2561c")
  val bravoTwoURI: URI   = URI.create("urn:BRAVO:7b4a2cec-b627-4ac6-965e-d948b3b27c9e")
  val charlieOneURI: URI = URI.create("urn:CHARLIE:27dcd15b-6ebf-4685-bf29-6ddcac8ed7b1")
  //
  val xrayURI: URI       = URI.create("urn:XRAY:d712e040-8e15-405e-964c-ec8158e9e29d") // Not in VALID_NAMES
  val yankeeURI: URI     = URI.create("https://example.com")                           // Wrong format
  val zuluURI: URI       = URI.create("urn:ZULU:not-a-uuid")                           // Invalid UUID format

  it should "classify URIs by name" in {
    // given
    lazy val uris = Seq(
      alphaOneURI,
      alphaTwoURI,
      bravoOneURI,
      bravoTwoURI,
      charlieOneURI
    ) ++ invalidUris

    lazy val invalidUris = Seq(
      xrayURI,
      yankeeURI,
      zuluURI
    )

    // when
    val resultMap = classify(uris)

    resultMap.nonEmpty shouldBe true
    resultMap("ALPHA").toSet shouldBe Set(alphaOneURI, alphaTwoURI)
    resultMap("BRAVO").toSet shouldBe Set(bravoOneURI, bravoTwoURI)
    resultMap("CHARLIE").toSet shouldBe Set(charlieOneURI)

    val classifiedUris = resultMap.values.flatten.toSeq
    classifiedUris.intersect(invalidUris) shouldBe empty

  }
