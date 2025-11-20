package com.cmartin.learn

import zio.stream.{Stream, ZSink, ZStream}
import zio.{Task, ZIO, ZIOAppDefault}

import scala.collection.immutable.TreeSet
import scala.io.Source

object StreamsPill
    extends ZIOAppDefault:

  private val FILE_PATH       = "file.txt"
  private val WORD_LENGTH_MIN = 3
  private val fixedChar       = 'v'
  private val allowedChars    = TreeSet('a', 'e', 'i', 'p', 'r', 's')

  private val lines =
    getFileLines
      .filter(_.length >= WORD_LENGTH_MIN)
      .filter(_.contains(fixedChar))
      .filter(isValidWord)

  private def getFileLines: Stream[Throwable, String] =
    ZStream.fromIteratorScoped(
      ZIO.fromAutoCloseable(
        ZIO.attempt(Source.fromFile(FILE_PATH))
      ).map(_.getLines())
    )

  /* drop fixed char from the word
   * convert the remaining word chars to set
   * check if the remaining chars are subset of allowedChars
   */
  private def isValidWord(word: String): Boolean =
    word.filterNot(_ == fixedChar).toSet subsetOf allowedChars

  def run: Task[Unit] =
    for
      _     <- lines.foreach(word => ZIO.logInfo(s"word: $word"))
      count <- lines.run(ZSink.count)
      _     <- ZIO.logInfo(s"Set of allowedChars: $allowedChars")
      _     <- ZIO.logInfo(s"Total words found: $count")
    yield ()
