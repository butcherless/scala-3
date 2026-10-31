# CLAUDE.md

## Project

A personal Scala 3 reference repo ("pills" = small self-contained examples) for the ZIO ecosystem: effects, fibers, streams, STM, ZLayer, zio-http, zio-json. Package root: `com.cmartin.learn`.

Two builds are configured **in parallel** for the same code, `build.sbt` and `build.mill`, and CI runs both. See "Build tool parity".

## Modules

- `pills`: the main module, with runnable mains (e.g. `HelloWorldApp`, `FiberPillApp`, `SalesApp`, `StreamsPill`) and most tests.
- `pills-int`: integration tests.

Most specs use ScalaTest; `FiberPillSpec` uses ZIO Test. Mill's `TestModule` supports one framework per submodule, hence `pills.test` and `pills.zioTest`. sbt handles both in one `test` task.

`StreamsPill` reads `pills/file.txt` through a bare relative path, so the working directory must be the module root. sbt does this by default; Mill sets `forkWorkingDir` to match.

## Commands

```
sbt compile                 # main sources; sbt Test/compile for tests
sbt test                    # all tests, both modules and frameworks
sbt "testOnly *AdtPillSpec" # single spec
sbt scalafmtAll             # format; sbt scalafmtCheckAll is the CI check
sbt assembly                # pills fat jar
sbt xcoverage               # clean;coverage;test;coverageReport
sbt xdup                    # dependencyUpdates
sbt pills/stryker           # mutation testing
```

`xstart`/`xstop` are broken: `sbt-revolver` isn't in `project/plugins.sbt`.

```
./mill __.compile
./mill pills.test + pills.zioTest + pills-int.test    # full run, matches sbt test
./mill pills.test.testOnly com.cmartin.learn.AdtPillSpec
./mill mill.scalalib.scalafmt/checkFormatAll          # format check
./mill mill.scalalib.scalafmt/                        # format
./mill pills.assembly
./mill pills.scoverage.xmlReport + pills.scoverage.htmlReport
./mill mill.javalib.Dependency/showUpdates            # equivalent of xdup
./mill pills.runMain com.cmartin.learn.StreamsPill    # several mains exist, so name one
```

## CI

`.github/workflows/scala.yml` runs the `scala3` (sbt) and `mill` jobs in parallel with the same steps. They share JDK setup through `.github/actions/setup-jdk`. Only the sbt job passes `cache: sbt`, since `setup-java` has no Mill preset.

## Build tool parity

- `project/Versions.scala`/`project/Dependencies.scala` and the `Versions` object in `build.mill` have no shared source. **When changing a dependency, the Scala version, scalac options or module structure, update both.**
- Renovate only tracks the sbt side, so the `bump-versions` skill keeps Mill and `.mill-version` current.
- sbt's `assemblyMergeStrategy` uses `MergeStrategy.last` for some duplicate netty/module-info files. Mill has no such rule and uses `Rule.ExcludePattern`, which is equivalent for a runnable jar but not byte-identical.
- Stryker4s is sbt-only. Mill's plugin can only wire one test module, so it can't cover both `pills.test` and `pills.zioTest`.

## Versioning policy

- **Scala**: always the latest LTS or newer, never a mainline release.
- **Direct deps**: stable GA. Named exception: ZIO Prelude (no GA release yet); don't chase a newer RC without a GA release or a needed capability.
- **Transitive deps**: left to the resolver; override only for a known vulnerability or binary incompatibility.
- **sbt plugins, sbt, Mill, scalafmt**: stable GA only.
- **GitHub Actions**: floating major tags (`actions/checkout@v7`), not SHAs. Only a new major needs a manual bump.
- **Updates**: patch/minor are free. Major bumps need a migration-guide review and a passing compile and test run on **both** builds. See the `bump-versions` skill.

## Mutation testing (Stryker4s)

`stryker4s.conf` mutates `pills/src/main/scala/**`. `.github/workflows/mutation-testing.yml` runs it on Fridays at 15:00 UTC or manually, and isn't part of `scala.yml`. It's report-only (`break = 0`), posts the score to the step summary and uploads the HTML report. Scores stay modest because the demo mains have little test coverage by design.

`legacy-test-runner = true` is required: the default runner fires a spurious second initial run on this mixed-framework suite and aborts. The legacy runner forks `sbt test` per mutant and needs more heap, hence `.jvmopts` (`-Xmx4096m`), which applies to every sbt run here. Without it, `pills/stryker` dies with `OutOfMemoryError`.

## Formatting

`.scalafmt.conf` governs both builds. Non-default settings: `rewrite.imports.sort = scalastyle`, import groups `[javax?, scala, *]`, `newlines.source = keep`, `docstrings = JavaDoc`.
