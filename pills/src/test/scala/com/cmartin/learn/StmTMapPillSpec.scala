package com.cmartin.learn

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.Runtime.{default => runtime}
import zio.stm._
import zio._
import com.cmartin.learn.StmTMapPill.Country

class StmTMapPillSpec extends AnyFlatSpec with Matchers:
  import StmTMapPillSpec.*

  behavior of "StmTMapPill"

  it should "add a tuple into InMemoryRepo" in {
    // given
    val program = 
      for 
      map <-   emptyMap
      repo <- buildRepository(map)
      a <- repo.add(key1,country1)
      r <- repo.get(key1)
    yield r 

    // when
    val result = runtime.unsafeRun(program)

    // then
    result shouldBe Some(country1)
  }

  it should "delete a tuple from InMemoryRepo" in {
    // given
    val program = 
      for 
      map <-   buildMap(key1,country1)
      repo <- buildRepository(map)
      a <- repo.delete(key1)
      r <- repo.get(key1)
    yield r 

    // when
    val result = runtime.unsafeRun(program)

    // then
    result shouldBe None
  }

  it should "update a tuple from InMemoryRepo" in {
    // given
    val program = 
      for 
      map <-   buildMap(key1,country1)
      repo <- buildRepository(map)
      a <- repo.update(key1, country2)
      r <- repo.get(key1)
    yield r 

    // when
    val result = runtime.unsafeRun(program)

    // then
    result shouldBe Some(country2)
  }


object StmTMapPillSpec:
  val key1 = "es"
  val country1 = Country("es", "Spain")
  val country2 = Country("pt", "Portugal")

  val emptyMap = TMap.empty[String,Country].commit

  def buildMap(key:String, value:Country) = 
    TMap.make((key,value)).commit

  def buildRepository(map: TMap[String, Country]) =
    ZIO.succeed( new StmTMapPill.InMemoryCountryRepository(map))
