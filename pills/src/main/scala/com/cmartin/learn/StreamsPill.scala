package com.cmartin.learn

import zio.stream.{ZSink, ZStream}
import zio.{Task, ZIO, ZIOAppDefault}

import scala.io.Source

object StreamsPill
    extends ZIOAppDefault {

  private val FILE_PATH       = "/tmp/file.txt"
  private val WORD_LENGTH_MIN = 3
  private val fixedChar       = 's'
  private val chars           = Set('a', 'b', 'r', 'd', 'e', 'g') + fixedChar

  private val lines =
    ZStream.fromIteratorScoped(
      ZIO.fromAutoCloseable(
        ZIO.attempt(Source.fromFile(FILE_PATH))
      ).map(_.getLines())
    ).filter(_.length >= WORD_LENGTH_MIN)
      .filter(word => word.contains(fixedChar) && word.forall(c => chars.contains(c)))

  def run: Task[Unit] = for {
    _     <- lines.foreach(word => ZIO.logInfo(s"word: $word"))
    count <- lines.run(ZSink.count)
    _     <- ZIO.logInfo(s"Set of chars: $chars")
    _     <- ZIO.logInfo(s"Total words found: $count")
  } yield ()

}
