import Dependencies.*

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := Versions.scala
ThisBuild / organization := "com.cmartin.learn"

lazy val basicScalacOptions = Seq(
  "-deprecation",
  "-encoding",
  "utf-8",
  "-explain",
  "-explaintypes",
  "-feature",
  "-unchecked",
//  "-language:postfixOps",
  "-language:higherKinds",
  "-Wunused:imports"
)

// The scoverage runtime writes measurement files directly into `scoverage-data`
// assuming the directory already exists (it never calls mkdirs itself). This task's
// only job is that filesystem side effect, so it must be Def.uncached itself too:
// otherwise sbt 2's CAS caches its (meaningless) Unit result and replays it without
// touching the filesystem, silently skipping the mkdir on a cache hit. Def.uncached
// only takes effect as the direct right-hand side of `key :=`, so this needs a real
// taskKey rather than a bare `lazy val = Def.task { ... }`.
lazy val ensureCoverageDataDir = taskKey[Unit]("Create <module>/scoverage-data before tests run")

lazy val commonSettings = Seq(
  scalacOptions ++= basicScalacOptions,
  libraryDependencies ++= Seq(scalaTest) ++ zioTest,
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  // resolvers += // temporal for ZIO snapshots
  //  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  ensureCoverageDataDir := Def.uncached {
    IO.createDirectory(coverageDataDir.value / "scoverage-data")
  },
  Test / executeTests := Def.uncached((Test / executeTests).dependsOn(ensureCoverageDataDir).value),
  // scoverage's compiler plugin also writes scoverage-data/scoverage.coverage as a side
  // effect of `compile`, with the same "assumes the dir exists" behavior as the runtime
  // Invoker above -- so the dir must exist before compile runs too, not just before
  // tests. Def.uncached here is load-bearing for two separate reasons: (1) overriding
  // Compile / compile requires it anyway, since CompileAnalysis has no JsonFormat and
  // sbt refuses to cache it without an explicit opt-out; (2) even so, verified
  // empirically that this does NOT stop CAS from replaying a cached compile -- that
  // happens below the facade key this wraps. The actual cache-bypass for CI/local runs
  // is the cacheVersion bump in the xcoverage alias.
  Compile / compile := Def.uncached((Compile / compile).dependsOn(ensureCoverageDataDir).value)
)

lazy val pills = (project in file("pills"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      zio,
      zioPrelude,
      zioLogging,
      zioHttp,
      zioJson
    ),
    assemblyStrategy
  )

lazy val `pills-int` = (project in file("pills-int"))
  .dependsOn(pills)
  .settings(
    name           := "pills-int",
    publish / skip := true,
    commonSettings
    // test dependencies
    // libraryDependencies += something % Test,
  )

lazy val assemblyStrategy = ThisBuild / assemblyMergeStrategy := {
  // specific cases
  case "META-INF/versions/9/OSGI-INF/MANIFEST.MF" => MergeStrategy.last
  case "META-INF/versions/9/module-info.class" => MergeStrategy.last
  case "META-INF/versions/11/module-info.class" => MergeStrategy.last
  case "META-INF/io.netty.versions.properties" => MergeStrategy.last
  // default case
  case default =>
    val oldStrategy = assemblyMergeStrategy.value
    oldStrategy(default)
}

// clear screen and banner
lazy val cls = taskKey[Unit]("Prints a separator")
LocalRootProject / cls := Def.uncached {
  val brs           = "\n".repeat(2)
  val message       = "BUILD BEGINS HERE"
  val spacedMessage = message.mkString("* ", " ", " *")
  val chars         = "*".repeat(spacedMessage.length())
  println(s"$brs$chars")
  println(spacedMessage)
  println(s"$chars$brs ")
}

// sbt 2's CAS caches `compile` below the level Def.uncached can reach on the compile
// key itself (verified empirically), so a cached compile silently skips scoverage's
// instrumentation-catalog write even with coverage freshly enabled. cacheVersion is
// sbt's documented escape hatch for exactly this ("invalidate all caches"); bumping it
// to a fresh value before every coverage run forces a genuine recompile, at the cost of
// caching for that one run -- an acceptable trade for a command that's meant to measure
// coverage accurately, not to be fast.
addCommandAlias("xcoverage", "set Global / cacheVersion := System.currentTimeMillis; clean;coverage;testFull;coverageReport")
addCommandAlias("xreload", "clean;reload")
addCommandAlias("xstart", "clean;reStart")
addCommandAlias("xstop", "reStop;clean")
addCommandAlias("xupdate", "clean;update")
addCommandAlias("xdup", "dependencyUpdates")
