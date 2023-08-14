package com.cmartin.learn

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.*
import Utils.runProgram

import ServiceAccessorPill.*
import ServiceAccessorPill.Model.*
import ServiceAccessorPill.RepositoryDefinitionModule.*
import ServiceAccessorPill.ServiceDefinitionModule.*

class ServiceAccessorPillSpec
    extends AnyFlatSpec
    with Matchers:

  behavior of "ServiceAccessorPill"

  val expectedCountry: Country = Country("CountryName", "es")
  val countryCode: String      = "es"

  val env = ZLayer.make[CountryRepository & CountryService](
    Neo4jCountryRepository.live,
    Neo4jCountryService.live
  )

  it should "access to the service via zio accessor" in {
    val program =
      (for
        srv     <- ZIO.service[CountryService]
        country <- srv.searchByCode(countryCode)
      yield country)
        .provide(env)

    val country: Country = runProgram(program)

    country shouldBe expectedCountry
  }

  it should "access to the service via accessor helper" in {
    val program =
      (for
        country <- Neo4jCountryService.searchByCode(countryCode)
      yield country)
        .provide(env)

    val country: Country = runProgram(program)

    country shouldBe expectedCountry
  }
