package com.cmartin.learn

import zio.*

object ServiceAccessorPill:

  object Model:
    /** Country object
      * @param name
      *   country name
      * @param code
      *   business identifier
      */
    case class Country(
        name: String,
        code: String
    )

    enum DatabaseError:
      case DatabaseObjectNotFound(message: String)
      case DefaultDatabaseError(message: String)

    enum ServiceError:
      case ResourceNotFound(message: String)
      case DefaultServiceError(message: String)

  object RepositoryDefinitionModule:
    import Model.*

    trait CountryRepository:
      def findByCode(code: String): IO[DatabaseError, Country]

    case class Neo4jCountryRepository() extends CountryRepository:
      override def findByCode(code: String): IO[DatabaseError, Country] =
        ZIO.succeed(Country("CountryName", code))

    object Neo4jCountryRepository:
      val live = ZLayer.succeed(Neo4jCountryRepository())

      def findByCode(code: String) =
        ZIO.serviceWithZIO[CountryRepository](_.findByCode(code))

  object ServiceDefinitionModule:
    import Model.*
    import Model.DatabaseError.*
    import Model.ServiceError.*
    import RepositoryDefinitionModule.CountryRepository

    trait CountryService:
      def searchByCode(code: String): IO[ServiceError, Country]

    case class Neo4jCountryService(repo: CountryRepository)
        extends CountryService:

      override def searchByCode(code: String): IO[ServiceError, Country] =
        for
          country <- repo.findByCode(code).mapError(Neo4jCountryService.manageError)
        yield country

    object Neo4jCountryService:
      val live: URLayer[CountryRepository, CountryService] =
        ZLayer.fromFunction(repo => Neo4jCountryService(repo))

      def searchByCode(code: String): ZIO[CountryService, ServiceError, Country] =
        ZIO.serviceWithZIO[CountryService](_.searchByCode(code))

      def manageError(dbError: DatabaseError): ServiceError =
        dbError match
          case DatabaseObjectNotFound(m) => ResourceNotFound(m)
          case DefaultDatabaseError(m)   => DefaultServiceError(m)
