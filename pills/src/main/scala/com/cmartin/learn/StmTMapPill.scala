package com.cmartin.learn

import zio.UIO
import zio.ZIO
import zio.stm.STM
import zio.stm.TMap
import zio.stm.TRef
import zio.stm.USTM
import zio.stm.ZSTM

object StmTMapPill:

  case class Country(code: String, name: String)

  class InMemoryCountryRepository(map: TMap[String, Country]):

    // [C]rud
    def add(key: String, value: Country) =
      for
        r <- map.put(key, value).commit
      yield r

    // c[R]ud
    def get(key: String) =
      for
        r <- map.get(key).commit
      yield r

    // cru[D]
    def delete(key: String) =
      for
        r <- map.delete(key).commit
      yield r

    // cr[U]d
    def update(key: String, value: Country) =
      for
        r <- map.updateWith(key)(_ => Some(value)).commit
      yield r
