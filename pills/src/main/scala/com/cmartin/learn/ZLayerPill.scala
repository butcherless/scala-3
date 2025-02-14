package com.cmartin.learn

import com.cmartin.learn.AviationModel.*
import zio.Runtime.{default => runtime}
import zio.*
import zio.internal.stacktracer.Tracer

object ZLayerPill:

  // domain names
  private type MyError = String

  // TODO add zio.logging.Logger or ZIO.2.x Logger

  object Repositories:

    trait MyCountryRepository:
      def insert(country: Country): IO[MyError, Long]
      def findByCode(code: CountryCode): IO[MyError, Option[Country]]
      def existsByCode(code: CountryCode): IO[MyError, Boolean]

    object MyCountryRepository:
      def insert(country: Country): ZIO[MyCountryRepository, MyError, Long] =
        ZIO.serviceWithZIO[MyCountryRepository](_.insert(country))

    trait MyAirportRepository:
      def insert(airport: Airport): IO[MyError, Long]

    object MyAirportRepository

  object SimpleLayer:
    trait MyService:
      def f1(): IO[String, Int]

    case class MyServiceLive() extends MyService:
      override def f1(): IO[String, Int] = ZIO.succeed(0)

    object MyServiceLive:
      val layer: ULayer[MyService] =
        ZLayer.succeed(MyServiceLive())

  object RepositoryImplementations:
    import Repositories.*

    case class MyCountryRepositoryLive()
        extends MyCountryRepository:

      override def existsByCode(code: CountryCode): IO[MyError, Boolean] =
        for
          _      <- ZIO.logDebug(s"existsByCode: $code")
          exists <- ZIO.succeed(true) // simulation
        yield exists

      override def insert(country: Country): IO[MyError, Long] =
        for
          _  <- ZIO.logDebug(s"insert: $country")
          id <- ZIO.succeed(1L)
        yield id

      override def findByCode(code: CountryCode): IO[MyError, Option[Country]] =
        for
          _ <- ZIO.logDebug(s"findByCode: $code")
        yield Some(Country(code, s"Country-name-for-$code"))

    object MyCountryRepositoryLive:
      val layer: ULayer[MyCountryRepository] =
        ZLayer.succeed(MyCountryRepositoryLive())

    case class MyAirportRepositoryLive()
        extends MyAirportRepository:

      override def insert(airport: Airport): IO[MyError, Long] =
        for
          _  <- ZIO.logDebug(s"insert: $airport")
          id <- ZIO.succeed(1L)
        yield id

    object MyAirportRepositoryLive:
      val layer: ULayer[MyAirportRepository] =
        ZLayer.succeed(MyAirportRepositoryLive())

    object Services:
      trait MyCountryService:
        def create(country: Country): IO[MyError, Country]

      object MyCountryService:
        def create(country: Country): ZIO[MyCountryService, String, Country] =
          ZIO.serviceWithZIO[MyCountryService](_.create(country))

      trait MyAirportService:
        def create(airport: Airport): IO[MyError, Airport]

      object MyAirportService:
        def create(airport: Airport): ZIO[MyAirportService, String, Airport] =
          ZIO.serviceWithZIO[MyAirportService](_.create(airport))

    object ServiceImplementations:
      import Repositories.*
      import Services.*

      case class MyCountryServiceLive(countryRepository: MyCountryRepository)
          extends MyCountryService:

        override def create(country: Country): IO[MyError, Country] =
          for
            _ <- ZIO.logDebug(s"create: $country")
            _ <- countryRepository.insert(country)
          yield country

      object MyCountryServiceLive:
        val layer: URLayer[MyCountryRepository, MyCountryService] =
          ZLayer.fromFunction(repo => MyCountryServiceLive(repo))

      case class MyAirportServiceLive(countryRepository: MyCountryRepository, airportRepository: MyAirportRepository)
          extends MyAirportService:

        override def create(airport: Airport): IO[MyError, Airport] =
          for
            _ <- ZIO.logDebug(s"create: $airport")
            _ <- ZIO.ifZIO(countryRepository.existsByCode(airport.country.code))(
                   airportRepository.insert(airport),
                   ZIO.fail(s"Country not found for code: ${airport.country.code}")
                 )
          yield airport

      object MyAirportServiceLive:
        val layer: URLayer[MyCountryRepository & MyAirportRepository, MyAirportService] =
          ZLayer {
            for
              cr <- ZIO.service[MyCountryRepository]
              ar <- ZIO.service[MyAirportRepository]
            yield MyAirportServiceLive(cr, ar)
          }

    /* common infrastructure */
    // val loggingEnv =    Slf4jLogger.make((_, message) => message)

    /* module use */
    object CountryRepositoryUse:
      import Repositories.*

      val country: Country = ???
      /* Repository Layer
     - requirement: LoggingService
     - output: Repository Layer
       */
      val countryRepoEnv   =
        MyCountryRepositoryLive.layer

      // insert computation 'has' a Repository dependency
      val repositoryProgram: ZIO[MyCountryRepository, String, Long] =
        MyCountryRepository.insert(country)
      // TODO remove macro error val repositoryResult = runtime.unsafeRun(
      //  repositoryProgram.provide(countryRepoEnv))

    object CountryServiceUse:
      import Services.*

      val country: Country = ???

      /* Service Layer
     - requirement: Logging + Repository
     - output: Service Layer
       */

      val serviceProgram: ZIO[MyCountryService, String, Country] =
        MyCountryService.create(country)
      // TODO remove macro error val serviceResult: Country = runtime.unsafeRun(
      //  serviceProgram.provide(countryServEnv))

    object AirportServiceUse:
      import ServiceImplementations.*
      import Services.*

      val country: Country                                       = ???
      val airport: Airport                                       = ???
      val airportSrvProg: ZIO[MyAirportService, String, Airport] =
        MyAirportService.create(airport)
      // TODO remove macro error val airportSrvRes: Airport = runtime.unsafeRun(
      //  airportSrvProg.provide(airportServEnv))

      val applicationLayer =
        ZLayer.make[MyCountryRepository & MyAirportRepository & MyCountryService & MyAirportService](
          MyCountryRepositoryLive.layer,
          MyAirportRepositoryLive.layer,
          MyCountryServiceLive.layer,
          MyAirportServiceLive.layer,
          ZLayer.Debug.mermaid
        )

      val fullProgram =
        for
          c <- MyCountryService.create(country)
          a <- MyAirportService.create(airport)
        yield (c, a)

      val fullResult = Unsafe.unsafe { implicit us =>
        runtime.unsafe.run(
          fullProgram.provide(applicationLayer)
        ).getOrThrowFiberFailure()
      }
