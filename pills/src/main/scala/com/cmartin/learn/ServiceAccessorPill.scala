package com.cmartin.learn

import zio.*

object ServiceAccessorPill {

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
      case FieldMappingError(message: String)
      case DefaultDatabaseError(message: String)

  object RepositoryDefinitionModule:
    import Model.*

    trait CountryRepository:
      def findByCode(code: String): IO[DatabaseError, Country]

    case class Neo4jCountryRepository() extends CountryRepository:
      override def findByCode(code: String): IO[DatabaseError, Country] =
        IO.succeed(Country("CountryName", code))

    object Neo4jCountryRepository extends Accessible[CountryRepository]:
      val live = ZLayer.succeed(Neo4jCountryRepository())

  object ServiceDefinitionModule:
    import Model.*

    import RepositoryDefinitionModule.CountryRepository
    trait CountryService:
      def searchByCode(code: String): IO[String, Country]

    case class Neo4jCountryService(repo: CountryRepository)
        extends CountryService:

      override def searchByCode(code: String): IO[String, Country] =
        for {
          country <- repo.findByCode(code).mapError(_.toString())
        } yield country

    object Neo4jCountryService:
      val live: URLayer[CountryRepository, CountryService] =
        ZLayer.fromFunction(repo => Neo4jCountryService(repo))

      def searchByCode(code: String) =
        ZIO.serviceWithZIO[CountryService](_.searchByCode(code))

}
