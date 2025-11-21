package com.cmartin.learn

import zio.stream.{Stream, ZSink, ZStream}
import zio.{Task, ZIO, ZIOAppDefault}

import java.text.Normalizer
import scala.collection.immutable.TreeSet
import scala.io.Source

object StreamsPill
    extends ZIOAppDefault:

  private val FILE_PATH       = "file.txt"
  private val WORD_LENGTH_MIN = 3
  private val fixedChar       = 'f'
  private val allowedChars    = TreeSet('o', 'r', 'i', 'c', 'p', 'a') + fixedChar

  private val lines =
    getFileLines
      .map(removeDiacritics)
      .filter(_.length >= WORD_LENGTH_MIN)
      .filter(_.contains(fixedChar))
      .filter(isValidWord)

  // Function to remove diacritics from UTF-8 string
  private def removeDiacritics(s: String): String =
    Normalizer
      .normalize(s, Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "")

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
    word.toSet subsetOf allowedChars

  def run: Task[Unit] =
    for
      // _     <- lines.foreach(word => ZIO.logInfo(s"word: $word"))
      chunk <- lines.run(ZSink.collectAll[String]).map(_.sortBy(_.length))
      list  <- ZIO.succeed(chunk.mkString("(\n", ",\n", "\n)"))
      _     <- ZIO.logInfo(s"Words list: $list")
      count <- lines.run(ZSink.count)
      _     <- ZIO.logInfo(s"Set of allowedChars: $allowedChars")
      _     <- ZIO.logInfo(s"Total words found: $count")
    yield ()
