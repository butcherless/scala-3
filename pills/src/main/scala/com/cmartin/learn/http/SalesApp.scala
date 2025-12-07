package com.cmartin.learn.http

import zio.http.*
import zio.json.*
import zio.stream.{Stream, ZStream}
import zio.{Clock, Random, Task, ZIO, ZIOAppDefault, ZLayer, duration2DurationOps, durationInt}

object SalesApp
    extends ZIOAppDefault:

  case class SaleEvent(productId: String, amount: BigDecimal, timestamp: Long)
  case class SalesUpdate(totalSales: BigDecimal, eventCount: Int)

  private object SalesUpdate:
    given JsonEncoder[SalesUpdate] = DeriveJsonEncoder.gen[SalesUpdate]
    given JsonDecoder[SalesUpdate] = DeriveJsonDecoder.gen[SalesUpdate]

  // Simulated service layer
  trait SalesService:
    def eventStream: Stream[Nothing, SaleEvent]

    def processSalesStream: Stream[Nothing, SalesUpdate]

  private object SalesService:
    // Live implementation with simulated data
    val live: ZLayer[Any, Nothing, SalesService] = ZLayer.succeed:
      new SalesService:
        // Simulate sale events every 1-2 seconds
        override def eventStream: ZStream[Any, Nothing, SaleEvent] =
          ZStream
            .repeatZIO(
              for
                productId <- Random.nextIntBounded(5).map(i => s"PROD-${i + 1}")
                amount    <-
                  Random.nextDoubleBetween(10.0, 50.0).map(BigDecimal(_).setScale(2, BigDecimal.RoundingMode.HALF_UP))
                timestamp <- Clock.currentTime(java.util.concurrent.TimeUnit.MILLISECONDS)
                duration  <- Random.nextIntBounded(5000).map(_.millis).map(1.second + _)
                _         <- ZIO.sleep(duration)
              yield SaleEvent(productId, amount, timestamp)
            )

        // Process events and compute running total
        override def processSalesStream: ZStream[Any, Nothing, SalesUpdate] =
          eventStream
            .scan((BigDecimal(0), 0)) { case ((total, count), event) =>
              (total + event.amount, count + 1)
            }
            .map { case (total, count) => SalesUpdate(total, count) }

  private object SalesController:
    def routes = Routes(
      Method.GET / "api" / "sales" / "stream" -> handler {
        for
          service <- ZIO.service[SalesService]
          stream   = service
                       .processSalesStream
                       .map(update =>
                         ServerSentEvent(
                           data = update.toJson,
                           eventType = Some("sales-update")
                         )
                       )
        yield Response(
          status = Status.Ok,
          headers = Headers(
            Header.ContentType(MediaType.text.`event-stream`),
            Header.CacheControl.NoCache,
            Header.Custom("X-Accel-Buffering", "no")
          ),
          body = Body.fromStream(stream)
        )
      },
      // Static HTML page to view the stream
      Method.GET / ""                         -> handler {
        Response(
          status = Status.Ok,
          headers = Headers(Header.ContentType(MediaType.text.html)),
          body = Body.fromString(HtmlPage.index)
        )
      }
    )

  def run: Task[Nothing] =
    Server
      .serve(SalesController.routes)
      .provide(
        Server.default,
        SalesService.live
      )
