# scala-3

> A new era in functional programming begins

| Continuous Integration                                                       | GitPod                                                                                                                                                     |
| ---------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| ![CI](https://github.com/butcherless/scala-3/workflows/Scala%20CI/badge.svg) | [![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/butcherless/scala-3) |


## ZIO Module Pattern

1. Service Definition - Trait

   - Define service operations returning ZIO effects

   ```scala
   def searchByCode(code: String): IO[ServiceError, Country]
   ```

2. Service Implementation - Case Class

   - Implements the trait define in step 1.
   - Declare service dependencies in the class constructor

   ```scala
   case class Neo4jCountryService(repo: CountryRepository)
   ```

3. Dependency Injection - ZLayer in the companion object

   - Build the service layer providing the dependencies

   ```scala
   ZLayer.fromFunction(repo => Neo4jCountryService(repo))
   ```

4. How to use it - Service accessors

   - Type A: ZIO accessor
   ```scala
   service <- ZIO.service[CountryService]
   ```

   - Type B: Helper function in the companion object 
   ```scala
   def searchByCode(code: String) =
     ZIO.serviceWithZIO[CountryService](_.searchByCode(code)) // companion object
     ...
     country <- Neo4jCountryService.searchByCode(countyCode) // program
   ```

   - Type C - Accesible macro
   ```scala
     object Neo4jCountryService extends Accessible[CountryService] // companion object
     ...
     country <- Neo4jCountryService(_.searchByCode(countyCode)) // program
   ```

## Links

- https://docs.scala-lang.org/scala3
- https://zio.dev
- https://www.scala-sbt.org
