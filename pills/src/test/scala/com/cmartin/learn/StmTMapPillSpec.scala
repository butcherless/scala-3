package com.cmartin.learn

import com.cmartin.learn.StmTMapPill.Country
import com.cmartin.learn.StmTMapPill.InMemoryCountryRepository
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio._
import zio.stm._
import Utils.runProgram

class StmTMapPillSpec
    extends AnyFlatSpec
    with Matchers:
  import StmTMapPillSpec.*

  behavior of "StmTMapPill"

  it should "WIP mutate the repository map" in {
    val prog1 =
      for
        repo  <- ZIO.succeed(myRepo)
        added <- repo.add(key1, country1)
      yield repo

    val repo1 = runProgram(prog1)

    val count1 = runProgram(repo1.size())

    info(s"element count: ${count1}")

    val prog2 =
      for
        repo  <- ZIO.succeed(myRepo)
        added <- repo.add(key2, country2)
      yield repo

    val repo2 = runProgram(prog2)

    val count2 = runProgram(repo1.size())

    info(s"element count: ${count2}")

    count1 shouldBe 1
    count2 shouldBe 2
  }

  it should "add a tuple into InMemoryRepo" in {
    // given
    val program =
      for
        map  <- emptyMap
        repo <- buildRepository(map)
        a    <- repo.add(key1, country1)
        r    <- repo.get(key1)
      yield r

    // when
    val result: Option[Country] = runProgram(program)

    // then
    result shouldBe Some(country1)
  }

  it should "delete a tuple from InMemoryRepo" in {
    // given
    val program =
      for
        map  <- buildMap(key1, country1)
        repo <- buildRepository(map)
        a    <- repo.delete(key1)
        r    <- repo.get(key1)
      yield r

    // when
    val result: Option[Country] = runProgram(program)

    // then
    result shouldBe None
  }

  it should "update a tuple from InMemoryRepo" in {
    // given
    val program =
      for
        map  <- buildMap(key1, country1)
        repo <- buildRepository(map)
        a    <- repo.update(key1, country2)
        r    <- repo.get(key1)
      yield r

    // when
    val result: Option[Country] = runProgram(program)

    // then
    result shouldBe Some(country2)
  }

object StmTMapPillSpec:
  val key1     = "es"
  val key2     = "pt"
  val country1 = Country("es", "Spain")
  val country2 = Country("pt", "Portugal")

  val emptyMap = TMap.empty[String, Country].commit

  def buildMap(key: String, value: Country) =
    TMap.make((key, value)).commit

  def buildRepository(map: TMap[String, Country]) =
    ZIO.succeed(StmTMapPill.InMemoryCountryRepository(map))

  val emptyRepoProg =
    for
      _  <- ZIO.log("initializing in memory repository")
      r0 <- ZIO.service[InMemoryCountryRepository]
    yield r0
  val myRepo        = runProgram(emptyRepoProg.provide(InMemoryCountryRepository.layer))
