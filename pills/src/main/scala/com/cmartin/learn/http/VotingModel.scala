package com.cmartin.learn.http

import zio.{Console, Queue, Scope, UIO, URIO, ZIO, durationInt}

import java.util.UUID

/** https://es.wikipedia.org/wiki/Elecciones_generales_de_Espa%C3%B1a_de_2023
  */
object VotingModel {

  enum Party {
    case PP, PSOE, Vox, Sumar, Podemos, ERC, Junts, PNV, BNG, Salf
  }

  case class Vote(id: UUID, party: Party)

  case class Box(
      code: String,
      votes: Set[Vote],
      queue: Queue[Vote]
  ) {
    // Send all votes to the queue with a delay between each
    def emitVotes(delaySeconds: Int): UIO[Unit] =
      ZIO.foreachDiscard(votes.toList) { vote =>
        for
          _ <- queue.offer(vote)
          _ <- Console.printLine(s"Box $code: Emitted vote ${vote.id} for ${vote.party}").orDie
          _ <- ZIO.sleep(delaySeconds.seconds)
        yield ()
      }
  }
  object Box {
    // Create a box with a reference to the center's queue
    def make(code: String, votes: Set[Vote], centerQueue: Queue[Vote]): UIO[Box] =
      ZIO.succeed(Box(code, votes, centerQueue))
  }

  // Center receives votes from all boxes
  case class Center(queue: Queue[Vote]) {

    def processVotes: ZIO[Any, Nothing, Unit] =
      queue.take.flatMap { vote =>
        Console.printLine(s"Center: Received vote ${vote.id} for ${vote.party}").orDie
      }.forever

    def getTotalVotes: UIO[Int] =
      queue.size
  }

  object Center {
    def make(queueSize: Int = 1000): URIO[Scope, Center] =
      Queue
        .bounded[Vote](queueSize)
        .map(Center(_))
  }
}
