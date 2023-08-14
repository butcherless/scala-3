package com.cmartin.learn

import zio.UIO
import zio.ZLayer
import zio.stm.TMap

object StmTMapPill:

  case class Country(code: String, name: String)

  case class InMemoryCountryRepository(map: TMap[String, Country]):

    // [C]rud
    def add(key: String, value: Country): UIO[Unit] =
      for
        r <- map.put(key, value).commit
      yield r

    // c[R]ud
    def get(key: String): UIO[Option[Country]] =
      for
        r <- map.get(key).commit
      yield r

    // cru[D]
    def delete(key: String): UIO[Unit] =
      for
        r <- map.delete(key).commit
      yield r

    // cr[U]d
    def update(key: String, value: Country): UIO[Option[Country]] =
      for
        r <- map.updateWith(key)(_ => Some(value)).commit
      yield r

    def size(): UIO[Int] =
      for
        r <- map.size.commit
      yield r

  object InMemoryCountryRepository:
    val layer = ZLayer {
      for
        emtpyMap <- TMap.empty[String, Country].commit
      yield InMemoryCountryRepository(emtpyMap)
    }
