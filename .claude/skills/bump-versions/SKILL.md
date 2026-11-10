---
name: bump-versions
description: >
  Checks for and applies dependency, SBT plugin, SBT-itself, Mill-itself, scalafmt-formatter, and
  GitHub Actions version updates in this Scala 3 / ZIO project, following the versioning policy in
  CLAUDE.md (LTS-only Scala, stable-GA-only deps except ZIO Prelude's RC exception, free
  patch/minor bumps, reviewed major bumps) and its build-tool-parity rule (sbt and Mill must be
  bumped together). Use this whenever the user asks to bump, upgrade, or update
  dependencies/libraries/plugins/SBT/Mill/scalafmt/GitHub-Actions, runs `sbt xdup` /
  `./mill mill.javalib.Dependency/showUpdates` and wants to act on the results, asks "are we behind
  on any libraries", "check for outdated deps", "bump zio", "check latest scalafmt version",
  "check GitHub Actions versions", "are our workflow actions up to date", or anything else about
  keeping this project's toolchain current — even when only one dependency is named.
---

# Bump versions

This project runs **two build tools in parallel** for the same codebase (`build.sbt` and
`build.mill` — see CLAUDE.md's "Build tool parity"), so most version-tracking files exist in a
pair. Each row below is checked a different way:

| What | Lives in | How to check |
|---|---|---|
| Library dependencies (sbt) | `project/Versions.scala` | `sbt xdup` (alias for `dependencyUpdates`) |
| Library dependencies (Mill) | `build.mill`'s `Versions` object | must mirror `project/Versions.scala` — no independent check, just parity |
| SBT plugins | `project/plugins.sbt` | `scripts/check_plugin_updates.py` (bundled here) |
| SBT itself | `project/build.properties` | GitHub releases API |
| Mill itself | `.mill-version` | GitHub releases API |
| scalafmt formatter | `.scalafmt.conf` (`version =`) | GitHub releases API |
| GitHub Actions | `.github/workflows/*.yml`, `.github/actions/*/action.yml` | `scripts/check_actions_updates.py` (bundled here) — this repo does **not** SHA-pin |

`xdup` only sees the sbt build's main dependencies, not `build.mill`'s `Versions` object,
`plugins.sbt` (the sbt meta-build), `build.properties`, `.mill-version`, `.scalafmt.conf`, or
the files under `.github/` — all checks are needed for a full picture. `.scalafmt.conf`'s `version` pins the
standalone scalafmt formatter, which is separate from the `sbt-scalafmt` plugin version in
`plugins.sbt` — the two are checked independently and can be a version apart. If the user named
one specific dependency, skip straight to its check instead of running all of them.

Renovate only tracks the sbt side (see CLAUDE.md's "Build tool parity") — this skill is what keeps
Mill, `.mill-version`, and GitHub Actions current.

**GitHub Actions convention in this repo:** the workflows (`scala.yml`, `mutation-testing.yml`)
and the `setup-jdk` composite action pin actions to **floating major-version tags**
(`actions/checkout@v7`, `actions/setup-java@v6`, `sbt/setup-sbt@v1`), not commit SHAs. This is a deliberate difference from the SHA-pinning
convention some other projects use — don't "upgrade" this repo to SHA pins as a side effect of a
version-bump task; that's a separate, larger discussion about supply-chain hardening. Because the
tags float, every patch/minor release under the current major is already picked up automatically
with no file edit needed. The only actionable check is whether a **new major** exists:
```bash
python3 .claude/skills/bump-versions/scripts/check_actions_updates.py
```
It scans every `uses: owner/repo@vN` in `.github/workflows/*.yml` and `.github/actions/*/action.yml`
and reports the floating major tag against the latest release (via an authenticated `gh api`). An
`<-- INCONSISTENT ref across files` marker means the same action is on different tags in
different files — resolve that before treating it as a clean bump.

## Step 1 — Sync with the remote

Pull first so the branch reflects the latest merged state — a teammate (or a manual run of this
skill) may have already merged a bump, and re-proposing one that already landed wastes a review
cycle.

```bash
gh auth status   # must succeed — every GitHub check below depends on it
git status       # must be clean
git pull
```

If `gh auth status` fails, **abort the skill** and tell the user to run `! gh auth login`.

If `git status` shows local changes, **abort the skill** — don't pull or stash on the user's
behalf. Tell them to commit or stash first; a version bump assumes a clean starting diff.

If the pull brings in changes to any of the version-tracking files above, note that in Step 6's
report instead of re-reporting it as this run's work.

## Step 2 — Check what's outdated

**Library deps (sbt side):**
```bash
sbt xdup
```
Ignore any `org.scala-lang:scala3-library` line — that's the SBT meta-build's own Scala version,
not this project's. The output also includes updates for excluded deps (e.g. ZIO Prelude
`1.0.0-RC48 -> 1.0.0-RCxx`) — leave those alone; filtering is Step 3's job, not this one's.

If this fails with `Not a valid command: xdup`, the sbt server is stuck in the meta-build context
from an earlier `reload plugins` command. Run `sbt shutdown` and retry.

**Library deps (Mill side):** don't check independently — `build.mill`'s `Versions` object must
always mirror `project/Versions.scala` exactly (CLAUDE.md's build-tool-parity rule). Any bump
approved on the sbt side gets mirrored here in Step 5; `./mill mill.javalib.Dependency/showUpdates`
can be used as a cross-check but should never disagree with `xdup` once the two files are in sync.

**Scala language version** (the `Versions.scala`/`build.mill` `scala` value — `xdup` can't tell an
LTS release from a mainline one, so check GitHub directly):
```bash
gh api 'repos/scala/scala3/releases?per_page=100' \
  --jq '[.[] | select(.prerelease | not) | select((.name // "") | test("LTS")) | .tag_name | select(test("^[0-9]+\\.[0-9]+\\.[0-9]+$"))] | max_by(split(".") | map(tonumber))'
```
Compare against `Versions.scala`'s `scala` value, numerically and using Step 3's rule (and confirm
`build.mill` still matches it). The query takes the *highest* LTS version, never the first one
listed: GitHub orders releases by date, and a patch on an older LTS line (`3.3.9`, published after
`3.9.0`) can come first.

**SBT plugins:**
```bash
python3 .claude/skills/bump-versions/scripts/check_plugin_updates.py
```
Reports current vs. latest per plugin in `project/plugins.sbt`. Already applies the stable-only
policy and its own exception list (Step 3) — a plugin with no stable release prints
`no stable release` rather than guessing. It reads the artifact for *this build's* SBT major
(`<artifact>_2.12_1.0` on SBT 1.x, `<artifact>_sbt2_3` on 2.x, picked from
`project/build.properties`), so it never proposes a version only published for the other SBT line.
A failed lookup prints `lookup failed` and exits non-zero (as does `check_actions_updates.py`,
also when `gh` isn't logged in) — treat a non-zero exit as "this check didn't complete" and report
it in Step 6, never as "up to date".

**SBT itself, Mill itself, and the scalafmt formatter** are each a single pinned version read
from GitHub releases with `gh api` (authenticated, so no 60-requests/hour limit; never plain
`curl https://api.github.com/...`). Pre-releases are excluded. **SBT is filtered to the 1.x
line on purpose**: `sbt/sbt`'s `/releases/latest` now points at 2.x, and a 2.x jump is a
deliberate migration, never a routine bump:
```bash
grep sbt.version project/build.properties
gh api 'repos/sbt/sbt/releases?per_page=100' \
  --jq '[.[] | select(.prerelease | not) | .tag_name | select(test("^v1\\.[0-9]+\\.[0-9]+$"))] | max_by(ltrimstr("v") | split(".") | map(tonumber))'

cat .mill-version
gh api repos/com-lihaoyi/mill/releases/latest --jq .tag_name

grep "^version" .scalafmt.conf
gh api repos/scalameta/scalafmt/releases/latest --jq .tag_name
```
The SBT query takes the *highest* `v1.x.y`, not the first listed: a late patch on an older 1.x
line (e.g. `1.12.16` after `1.13.1`) is listed first by date. If it prints nothing (`gh --jq`
renders a `null` result as an empty line), the 2.x releases have pushed every 1.x one off the
first 100 — add `&page=2` and re-run rather than reporting SBT as up to date.

Compare each pair. (The `.scalafmt.conf` value is the standalone formatter, not the `sbt-scalafmt`
plugin checked above — see the intro table note on why those two can differ.)

**GitHub Actions:** run `check_actions_updates.py` (see the floating-tag note above) — only a new
major needs a manual bump.

## Step 3 — Apply the project's versioning policy

Read CLAUDE.md's "Versioning policy" for the full rationale; in short:

- **Scala** → always the latest LTS (from Step 2's GitHub check) or a newer LTS, never a mainline
  release. A project may stay off LTS for tests or PoCs if it says so, in CLAUDE.md's "Versioning
  policy" or in a comment on the `Versions.scala` Scala value (none in this project today). Check
  for an exception first, then:
  - **Exception recorded** → leave the Scala version alone apart from patch bumps on its own line.
    Report it as an exception, not a deviation.
  - **Older than the latest LTS** → apply the latest LTS. No scope confirmation needed in Step 4;
    Step 5's verify gate still applies.
  - **Equal to the latest LTS, or a newer LTS** → nothing to do.
  - **On a non-LTS release with no recorded exception** → report it as a policy deviation and ask.
    Don't downgrade on your own.
- **Direct deps** → stable GA by default. ZIO Prelude (no GA release yet — see CLAUDE.md for the
  current pin) stays untouched entirely — don't chase a newer RC for it either, unless there's a
  deliberate reason (a GA release or a needed capability).
- **SBT plugins** → stable GA by default; exceptions live in the script's
  `STABLE_ONLY_EXCEPTIONS` (empty today), not duplicated here.
- **SBT itself, Mill itself, scalafmt formatter** → always stable, no exception mechanism. A
  scalafmt reformat can change files even with no rule changes in `.scalafmt.conf` (new default
  heuristics between releases) — treat any resulting diff as expected, not a sign something's
  wrong.
- **GitHub Actions** → bump only on a new major (see the intro note); never introduce SHA-pinning
  as part of this skill.
- **Patch/minor bumps** are safe to apply directly. **Major bumps** need a migration-guide read —
  flag separately, don't bundle them with the safe ones. For a GitHub Action major bump, "migration
  guide" means that action's own release notes/changelog, same diligence as a major library bump.
- **Transitive deps** — leave to SBT's/Mill's resolver; override only for a known vulnerability or
  binary-incompatibility.

## Step 4 — Confirm scope with the user

If the user named a specific dependency, do just that one. Otherwise present Step 2's findings
split into "safe to apply" (patch/minor, GA) and "needs review" (major bumps, or touching ZIO
Prelude's RC pin), and ask which to apply.

## Step 5 — Apply and verify each bump, one at a time

Don't batch edits. Apply exactly one version bump, then run the full verify gate against **both**
build tools, before touching the next approved one — that way, if something breaks, it's obvious
which bump did it, and which build tool it broke, instead of being buried in a combined diff.

For each approved **library dependency** bump:

1. Edit `project/Versions.scala` (or `project/Dependencies.scala` if the coordinate itself
   changed), then make the identical edit in `build.mill`'s `Versions` object — CLAUDE.md requires
   these stay in sync manually, there's no shared source between the two builds.
2. Verify both builds:
   ```bash
   sbt scalafmtAll
   sbt compile               # zero errors, zero warnings
   sbt test                  # full suite green (pills + pills-int, both frameworks)
   ./mill __.compile
   ./mill pills.test + pills.zioTest + pills-int.test
   ```
   This repo is on sbt 1, where `test` runs the whole suite. If it ever moves to sbt 2, use
   `testFull` instead: sbt 2's `test` is `testQuick`, its disk cache survives `clean`, and it can
   report zero tests run as `[success]`.
3. If both pass, move on to the next approved bump. If either fails, stop — don't layer further
   bumps on top of a broken one. Investigate before deciding whether to revert this one, fix the
   fallout, or tell the user it needs more review than expected.

For each approved **SBT plugin**, **SBT itself**, **Mill itself**, or **scalafmt formatter** bump:

1. Edit only that one file — `project/plugins.sbt`, `project/build.properties`, `.mill-version`,
   or `.scalafmt.conf` respectively. (`.mill-version` and `.scalafmt.conf` are read by both build
   tools already, so there's no second file to touch for those two.)
2. Run the same verify gate as above (both `sbt` and `./mill` commands) — a plugin or toolchain
   bump can just as easily break one build and not the other.
3. Same stop-on-failure rule as above.

For an approved **GitHub Actions** major-version bump:

1. Edit the tag in every file the script listed under "used in:" (`grep -rn "owner/repo@"
   .github/` first to confirm every occurrence — workflows and the `setup-jdk` composite action).
2. Verify: `sbt`/`mill` commands don't touch `.github/`, so they verify nothing about this bump.
   Instead confirm the YAML still parses:
   `ruby -ryaml -e 'ARGV.each { |f| YAML.safe_load(File.read(f)) }' .github/workflows/*.yml .github/actions/*/action.yml`
   (macOS ships Ruby's YAML parser; PyYAML usually isn't installed).
   The only real confirmation that the new major behaves correctly is CI actually running it —
   flag this to the user as the one bump category that needs a post-push CI check, an intentional
   exception to CLAUDE.md's default of not monitoring CI unless asked.

After the last **sbt-side** bump passes (library dep, sbt plugin, or sbt itself), run
`sbt bloopInstall` once to refresh `.bloop/` for Metals/IDE import — sbt-bloop is one of the
pinned plugins, and one regenerate at the end covers every bump in the run. Skip this if the only
bumps were Mill-only (`.mill-version`), scalafmt, or GitHub Actions (nothing sbt-dependency-related
changed).

## Step 5c — Commit, then ask before pushing

Commit each bump on its own once its verify gate has passed — one commit per bump, so a later
revert or `git bisect` lands on exactly one version change. Commit on the current branch.
Message shape: `chore: bump <dependency> to <version>`, with a body for anything the diff doesn't
say on its own (a major bump's migration notes, fallout fixed alongside it, which verify layers
ran). Stage only the files that bump touched; an unrelated problem the verify gate exposed gets its
own commit.

**Never push without asking.** Once the user says yes, push, and after the push don't watch CI
unless they ask, with the one exception from Step 5: a GitHub Actions major bump needs its first CI run checked. A dependency bump must land in **both** builds (sbt and Mill) in the same commit — see CLAUDE.md's "Build tool parity".

## Step 6 — Report

Cover both of these, not just the first:

- **What changed** — a short table: dependency, old version → new version, whether it touched one
  build file or both, and whether its individual verify pass (Step 5) was clean on both `sbt` and
  `./mill`.
- **What came up during the run** — anything unexpected, even if the run otherwise succeeded: a
  stuck sbt server needing `shutdown`, a plugin reporting `no stable release`, the skill aborting
  on local changes, ZIO Prelude or a major bump left untouched, a bump that failed verify on one
  build tool but not the other and was reverted. This is often more useful to the user than the
  version diff itself, so don't bury it or leave it out just because the bump succeeded.
