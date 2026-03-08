package com.cmartin.learn.collecion

import com.cmartin.learn.collection.ChunkedCollectionPill.*
import com.cmartin.learn.collection.ChunkedCollectionPill.Color.{INDIGO, ORANGE, RED, YELLOW}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI
import java.util.UUID

// word list file: https://github.com/JorgeDuenasLerin/diccionario-espanol-txt/blob/master/0_palabras_todas_no_conjugaciones.txt
class ChunkedCollectionPillSpec
    extends AnyFlatSpec
    with Matchers {

  behavior of "ChunkedCollectionPill"

  // d91b8ece-ff18-491b-9c76-bd9231e25d1c
  // 26f613c8-2df1-42ac-ade8-19ae774495ca
  // 10f93893-04dd-416e-a957-ad3a37eff075
  // 8e44b40c-3fdf-496f-bd49-38bca4889909
  // 156882b9-abdb-4f68-a002-c0e7d0b7c93f
  // 5422008a-38c9-4f63-801c-96b5082efa8e
  // c850dceb-3d2f-4689-9ec2-afffafd0cee3
  // d5c7c423-283c-43f4-a47f-6a6c365ec145
  // 8fd16506-054f-4ee1-ac53-90ee8e44e024

  // 17884315-002a-4cd6-9591-f9c1a1192a1a
  // b29e1b0d-e936-4073-9ae7-5e19e91e73fb
  // d5c9f026-aef5-4a89-9843-0b20662c9f8e
  // ec21b093-c2bf-40f1-aee8-c4b6be2a9ea6
  // 57c5980a-ed98-4bfd-845e-dd04fc2a95a9
  // 35a0ca2c-4f0d-46c2-ae73-159c9f2ed843
  // f4c2f680-e5ca-4590-87e5-2d3d39bdc027
  // 69dd2205-c712-47f0-90f6-de8abab8f26f
  // 55351828-f481-4ec3-a002-f932d8d0563c
  // c38bc8e6-fe8b-4f9e-b691-000ee51f382c

  val redOneURI: URI   = URI.create("urn:RED:eb093186-52e3-4a5a-889f-eb5ae2988cc3")
  val redTwoURI: URI   = URI.create("urn:RED:26f613c8-2df1-42ac-ade8-19ae774495ca")
  val redThreeURI: URI = URI.create("urn:RED:10f93893-04dd-416e-a957-ad3a37eff075")
  val redFourURI: URI  = URI.create("urn:RED:8e44b40c-3fdf-496f-bd49-38bca4889909")
  val redFiveURI: URI  = URI.create("urn:RED:156882b9-abdb-4f68-a002-c0e7d0b7c93f")
  val redSixURI: URI   = URI.create("urn:RED:5422008a-38c9-4f63-801c-96b5082efa8e")
  val redSevenURI: URI = URI.create("urn:RED:c850dceb-3d2f-4689-9ec2-afffafd0cee3")

  val orangeOne   = URI.create("urn:ORANGE:d5c7c423-283c-43f4-a47f-6a6c365ec145")
  val orangeTwo   = URI.create("urn:ORANGE:8fd16506-054f-4ee1-ac53-90ee8e44e024")
  val orangeThree = URI.create("urn:ORANGE:17884315-002a-4cd6-9591-f9c1a1192a1a")

  val yellowOne = URI.create("urn:YELLOW:b29e1b0d-e936-4073-9ae7-5e19e91e73fb")

  val indigoOne   = URI.create("urn:INDIGO:d5c9f026-aef5-4a89-9843-0b20662c9f8e")
  val indigoTwo   = URI.create("urn:INDIGO:ec21b093-c2bf-40f1-aee8-c4b6be2a9ea6")
  val indigoThree = URI.create("urn:INDIGO:57c5980a-ed98-4bfd-845e-dd04fc2a95a9")
  val indigoFour  = URI.create("urn:INDIGO:35a0ca2c-4f0d-46c2-ae73-159c9f2ed843")
  val indigoFive  = URI.create("urn:INDIGO:f4c2f680-e5ca-4590-87e5-2d3d39bdc027")

  val redUris    = List(redOneURI, redTwoURI, redThreeURI, redFourURI, redFiveURI, redSixURI, redSevenURI)
  val orangeUris = List(orangeOne, orangeTwo, orangeThree)
  val yellowUris = List(yellowOne)
  val indigoUris = List(indigoOne, indigoTwo, indigoThree, indigoFour, indigoFive)

  val allUris = List(redUris, orangeUris, yellowUris, indigoUris).flatten

  val invalidColorURI = URI.create("urn:WHITE:12345678-1234-1234-1234-123456789012")
  val invalidURI      = URI.create("prefix:INVALID:12345678")

  it should "return true for valid name" in {
    val result = hasValidName(RED.toString)

    result shouldBe true
  }

  it should "return false for invalid name" in {
    val result = hasValidName("invalid")
  }

  it should "match a valid color" in {
    // given
    val expected = MatchResult(RED, redOneURI)
    // when
    val result   = matchUri(redOneURI)
    // then
    result shouldBe Some(expected)
  }

  it should "fail to match an invalid color" in {
    // given
    val expected = None
    // when
    val result   = matchUri(invalidColorURI)
    // then
    result shouldBe expected
  }

  it should "classify URIs by name" in {
    // given

    // when
    val resultMap = classify(allUris)

    // then
    resultMap.nonEmpty shouldBe true
    resultMap(RED).toSet shouldBe redUris.toSet
    resultMap(ORANGE).toSet shouldBe orangeUris.toSet
    resultMap(YELLOW).toSet shouldBe yellowUris.toSet
    resultMap(INDIGO).toSet shouldBe indigoUris.toSet
  }

  it should "chunk each color list into sublists based on the typeLimitMap" in {
    // given
    val classified = classify(allUris)

    // when
    val chunked: Map[Color, Seq[Seq[URI]]] = chunk(classified)

    // then

    chunked(RED).size shouldBe 2
    chunked(RED).flatten.toSet shouldBe redUris.toSet

    chunked(ORANGE).size shouldBe 1
    chunked(ORANGE).flatten.toSet shouldBe orangeUris.toSet

    chunked(YELLOW).size shouldBe 1
    chunked(YELLOW).flatten.toSet shouldBe yellowUris.toSet

    chunked(INDIGO).size shouldBe 3
    chunked(INDIGO).flatten.toSet shouldBe indigoUris.toSet

    def generate(n: Int): Seq[UUID] =
      Seq.fill(n)(UUID.randomUUID())
  }

  it should "generate UUIDs" in {
    // generate(10).foreach(println)
  }

}
