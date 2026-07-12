# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A personal Scala 3 learning/reference repo ("pills" = small self-contained examples) demonstrating the ZIO ecosystem: effects, fibers, streams, STM, ZLayer dependency injection, zio-http, zio-json. Package root: `com.cmartin.learn`.

Two build tools are configured **in parallel** for the same codebase:
- `build.sbt` — the original, currently what CI and Renovate track.
- `build.mill` — a newer equivalent build, added for evaluation. Not yet wired into CI's dependency-update automation.

Both must be kept in sync when dependencies, scalac options, or module structure change. See "Build tool parity" below.

## Modules

- `pills` — the main module. Contains ZIO examples, a few runnable `App`/`ZIOAppDefault` mains (e.g. `HelloWorldApp`, `FiberPillApp`, `SalesApp`, `StreamsPill`), and most of the test suite.
- `pills-int` — integration tests; depends on `pills`, not published.

Test specs are split across **two frameworks in the same `pills` module**: most specs use ScalaTest (`AnyFlatSpec`), but `FiberPillSpec` uses ZIO Test (`ZIOSpecDefault`). This is why `pills` has two Mill test submodules (`pills.test` for ScalaTest, `pills.zioTest` for ZIO Test) instead of one — Mill's `TestModule` only supports a single framework per submodule, unlike sbt which dispatches per-class across multiple registered frameworks in one `test` task.

`pills/file.txt` is a large word-list resource read via a bare relative path (`Source.fromFile("file.txt")`) in `StreamsPill`, not as a classpath resource — it depends on the process's working directory being the module root. sbt does this by default; the Mill build sets `forkWorkingDir` explicitly to match.

## Commands (sbt)

```
sbt compile              # compile main sources
sbt Test/compile         # compile test sources
sbt test                 # run all tests (both pills and pills-int, both frameworks)
sbt "testOnly *AdtPillSpec"   # run a single spec
sbt scalafmtCheckAll      # format check (CI-enforced)
sbt scalafmtAll           # reformat in place
sbt assembly               # build the pills fat jar (custom merge strategy in build.sbt)
sbt xcoverage              # alias: clean;coverage;test;coverageReport
sbt xdup                   # alias: dependencyUpdates
```

`xstart`/`xstop` aliases exist in `build.sbt` (`reStart`/`reStop`) but are **broken** — `sbt-revolver` isn't in `project/plugins.sbt`. Don't rely on them.

## Commands (Mill)

```
./mill pills.compile
./mill __.compile                                   # compile everything
./mill pills.test                                   # ScalaTest specs
./mill pills.zioTest                                 # the one ZIO Test spec (FiberPillSpec)
./mill pills.test + pills.zioTest + pills-int.test    # full test run, matches `sbt test`
./mill pills.test.testOnly com.cmartin.learn.AdtPillSpec   # single spec
./mill mill.scalalib.scalafmt/checkFormatAll          # format check
./mill mill.scalalib.scalafmt/                        # reformat in place
./mill pills.assembly
./mill pills.scoverage.xmlReport + pills.scoverage.htmlReport
./mill mill.javalib.Dependency/showUpdates             # equivalent of sbt's xdup, built into Mill core
./mill pills.runMain com.cmartin.learn.StreamsPill     # run a specific main (several App objects exist; ambiguous without a class name)
```

CI (`.github/workflows/scala.yml`) runs both the `scala3` (sbt) and `mill` jobs in parallel, mirroring the same steps: compile → test → scalafmtCheckAll → assembly → coverage.

## Build tool parity

`project/Versions.scala`/`project/Dependencies.scala` (sbt) and the `Versions` object in `build.mill` must be kept in sync manually — there's no shared source of dependency versions between the two builds. When bumping a dependency or Scala version, update both.

sbt's `assemblyMergeStrategy` (`build.sbt`) uses `MergeStrategy.last` for a few duplicate netty/module-info metadata files; Mill's `assemblyRules` has no literal "keep last" rule, so the equivalent entries in `build.mill` use `Rule.ExcludePattern` instead — behaviorally equivalent for a runnable jar but not byte-identical.

Renovate (`renovate.json`) currently only has `"sbt": { "enabled": true }` — it does not track `build.mill` dependency versions.

## Formatting

`.scalafmt.conf` governs both builds identically (Mill reads the same root config). Notable non-default settings: `rewrite.imports.sort = scalastyle` with import groups `[javax?, scala, *]`, `newlines.source = keep`, `docstrings = JavaDoc`.
