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
   [Service trait](https://github.com/butcherless/scala-3/blob/ecdc362726fbec9665495b39d83328e2e7a6baf7/pills/src/main/scala/com/cmartin/learn/ServiceAccessorPill.scala#L49)

2. Service Implementation - Case Class

   - Implements the trait define in step 1.
   - Declare service dependencies in the class constructor

   ```scala
      case class Neo4jCountryService(repo: CountryRepository)
   ```
   [Service implementation](https://github.com/butcherless/scala-3/blob/ecdc362726fbec9665495b39d83328e2e7a6baf7/pills/src/main/scala/com/cmartin/learn/ServiceAccessorPill.scala#L52)

3. Dependency Injection - ZLayer in the companion object

   - Build the service layer providing the dependencies

   ```scala
      ZLayer.fromFunction(repo => Neo4jCountryService(repo))
   ```
   [Service layer](https://github.com/butcherless/scala-3/blob/ecdc362726fbec9665495b39d83328e2e7a6baf7/pills/src/main/scala/com/cmartin/learn/ServiceAccessorPill.scala#L62)

4. How to use it - Service accessors

   - Type A: ZIO accessor
   ```scala
      for {
        service <- ZIO.service[CountryService]
        ...
   ```
   [Type A example](https://github.com/butcherless/scala-3/blob/ecdc362726fbec9665495b39d83328e2e7a6baf7/pills/src/test/scala/com/cmartin/learn/ServiceAccessorPillSpec.scala#L30)

   - Type B: Helper function in the companion object 
   ```scala
      def searchByCode(code: String) =
        ZIO.serviceWithZIO[CountryService](_.searchByCode(code)) // companion object
        ...
        country <- Neo4jCountryService.searchByCode(countyCode) // program
   ```
   [Type B example](https://github.com/butcherless/scala-3/blob/ecdc362726fbec9665495b39d83328e2e7a6baf7/pills/src/test/scala/com/cmartin/learn/ServiceAccessorPillSpec.scala#L43)


## Links

- https://docs.scala-lang.org/scala3
- https://zio.dev
- https://www.scala-sbt.org
