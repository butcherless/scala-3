package com.cmartin.learn

import com.cmartin.learn.AviationModel.*
import org.slf4j.LoggerFactory
import zio.*
import zio.internal.stacktracer.Tracer

object ZLayerPill {

  // domain names
  type MyError = String

  // TODO add zio.logging.Logger or ZIO.2.x Logger

  object Repositories {

    trait MyCountryRepository {
      def insert(country: Country): IO[MyError, Long]
      def findByCode(code: CountryCode): IO[MyError, Option[Country]]
      def existsByCode(code: CountryCode): IO[MyError, Boolean]
    }
    object MyCountryRepository extends Accessible[MyCountryRepository]

    trait MyAirportRepository {
      def insert(airport: Airport): IO[MyError, Long]
    }
    object MyAirportRepository extends Accessible[MyAirportRepository]
  }

  object SimpleLayer {
    trait MyService {
      def f1(): IO[String, Int]
    }
    case class MyServiceLive() extends MyService {
      override def f1(): IO[String, Int] = IO.succeed(0)
    }

    object MyServiceLive {
      val layer: ULayer[MyService] =
        ZLayer.fromFunction(_ => MyServiceLive())
    }
  }

  object RepositoryImplementations {
    import Repositories.*

    case class MyCountryRepositoryLive()
        extends MyCountryRepository {

      override def existsByCode(code: CountryCode): IO[MyError, Boolean] = {
        for {
          _      <- ZIO.logDebug(s"existsByCode: $code")
          exists <- UIO.succeed(true) // simulation
        } yield exists
      }

      override def insert(country: Country): IO[MyError, Long]                 = {
        for {
          _  <- ZIO.logDebug(s"insert: $country")
          id <- IO.succeed(1L)
        } yield id
      }
      override def findByCode(code: CountryCode): IO[MyError, Option[Country]] = {
        for {
          _ <- ZIO.logDebug(s"findByCode: $code")
        } yield Some(Country(code, s"Country-name-for-$code"))
      }
    }

    object MyCountryRepositoryLive {
      val layer: ULayer[MyCountryRepository] =
        ZLayer.fromFunction(_ => MyCountryRepositoryLive())
    }

    case class MyAirportRepositoryLive()
        extends MyAirportRepository {

      override def insert(airport: Airport): IO[MyError, Long] = {
        for {
          _  <- ZIO.logDebug(s"insert: $airport")
          id <- IO.succeed(1L)
        } yield id
      }

    }
    object MyAirportRepositoryLive {
      val layer: ULayer[MyAirportRepository] =
        ZLayer.fromFunction(_ => MyAirportRepositoryLive())
    }

    object Services {
      trait MyCountryService {
        def create(country: Country): IO[MyError, Country]
      }

      object MyCountryService extends Accessible[MyCountryService]

      trait MyAirportService {
        def create(airport: Airport): IO[MyError, Airport]
      }

      object MyAirportService extends Accessible[MyAirportService]
    }

    object ServiceImplementations {
      import Repositories.*
      import Services.*

      case class MyCountryServiceLive(countryRepository: MyCountryRepository)
          extends MyCountryService {

        override def create(country: Country): IO[MyError, Country] = {
          for {
            _ <- ZIO.logDebug(s"create: $country")
            _ <- countryRepository.insert(country)
          } yield country
        }

      }

      object MyCountryServiceLive {
        val layer: URLayer[MyCountryRepository, MyCountryService] =
          ZLayer(
            ZIO.service[MyCountryRepository]
              .map(MyCountryServiceLive(_))
          )
      }

      case class MyAirportServiceLive(countryRepository: MyCountryRepository, airportRepository: MyAirportRepository)
          extends MyAirportService {

        override def create(airport: Airport): IO[MyError, Airport] = {
          for {
            _ <- ZIO.logDebug(s"create: $airport")
            _ <- ZIO.ifZIO(countryRepository.existsByCode(airport.country.code))(
                   airportRepository.insert(airport),
                   IO.fail(s"Country not found for code: ${airport.country.code}")
                 )
          } yield airport
        }
      }

      object MyAirportServiceLive {
        val layer: URLayer[MyCountryRepository with MyAirportRepository, MyAirportService] =
          ZLayer {
            for {
              c <- ZIO.service[MyCountryRepository]
              a <- ZIO.service[MyAirportRepository]
            } yield MyAirportServiceLive(c, a)
          }
      }
    }

    /* common infrastructure */
    // val loggingEnv =    Slf4jLogger.make((_, message) => message)
    val runtime = zio.Runtime.default

    /* module use */
    object CountryRepositoryUse {
      import Repositories.*
      import RepositoryImplementations.*

      val country: Country = ???
      /* Repository Layer
     - requirement: LoggingService
     - output: Repository Layer
       */
      val countryRepoEnv   =
        MyCountryRepositoryLive.layer

      // insert computation 'has' a Repository dependency
      val repositoryProgram: ZIO[MyCountryRepository, String, Long] =
        MyCountryRepository(_.insert(country))
      // TODO remove macro error val repositoryResult = runtime.unsafeRun(
      //  repositoryProgram.provide(countryRepoEnv))
    }

    object CountryServiceUse {
      import RepositoryImplementations.*
      import ServiceImplementations.*
      import Services.*

      val country: Country = ???

      /* Service Layer
     - requirement: Logging + Repository
     - output: Service Layer
       */
      val countryServEnv =
        MyCountryRepositoryLive.layer >>> MyCountryServiceLive.layer

      val serviceProgram: ZIO[MyCountryService, String, Country] =
        MyCountryService(_.create(country))
      // TODO remove macro error val serviceResult: Country = runtime.unsafeRun(
      //  serviceProgram.provide(countryServEnv))
    }

    object AirportServiceUse {
      import RepositoryImplementations.*
      import ServiceImplementations.*
      import Services.*

      /* Repositories Layer
     - requirement: Logging
     - output: Logging + RepoA + RepoB Layer
       */
      // val repositoriesEnv = MyCountryRepositoryLive.layer ++ MyAirportRepositoryLive.layer

      /* Service Layer
     - requirement: Logging + RepoA + RepoB
     - output: Service Layer
       */
      // val airportServEnv = repositoriesEnv >>> MyAirportServiceLive.layer

      val country: Country                                       = ???
      val airport: Airport                                       = ???
      val airportSrvProg: ZIO[MyAirportService, String, Airport] =
        MyAirportService(_.create(airport))
      // TODO remove macro error val airportSrvRes: Airport = runtime.unsafeRun(
      //  airportSrvProg.provide(airportServEnv))

      val applicationLayer =
        ZLayer.make[MyCountryRepository with MyAirportRepository with MyCountryService with MyAirportService](
          MyCountryRepositoryLive.layer,
          MyAirportRepositoryLive.layer,
          MyCountryServiceLive.layer,
          MyAirportServiceLive.layer,
          ZLayer.Debug.mermaid
        )

      val fullProgram: ZIO[MyAirportService with MyCountryService, MyError, Unit] = for {
        x1 <- MyCountryService(_.create(country))
        x2 <- MyAirportService(_.create(airport))
      } yield ()

      val fullResult: Unit = runtime.unsafeRun(
        fullProgram.provide(applicationLayer)
      )
    }

  }
}
