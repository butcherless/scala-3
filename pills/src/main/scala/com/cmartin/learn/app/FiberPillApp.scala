package com.cmartin.learn.app

import com.cmartin.learn.FiberPill.*
import zio.{UIO, ZIO, ZIOAppDefault}

import java.util.UUID


object FiberPillApp
  extends ZIOAppDefault:

  private val FIBER_COUNT_MAX = 10000

  /**
   * Fiber creation demo.
   * The main entry point of the application.
   * It creates a list of ProcessInput instances,
   * forks fibers to process them
   * and waits for all fibers to complete.
   */
  def run: UIO[Unit] =
    val inputs = List
      .fill(FIBER_COUNT_MAX)(UUID.randomUUID())
      .zipWithIndex.map {
      case (uuid, index) => ProcessInput(uuid, s"process input $index")
    }
    // create fibers
    val fibers = inputs.map(input => process(input).fork)
    for
      fiberResults  <- ZIO.collectAll(fibers)
      results <- ZIO.collectAll(fiberResults.map(_.join))
    yield ()
