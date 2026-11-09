#!/usr/bin/env python3
"""Reports the latest available version for each SBT plugin in project/plugins.sbt.

Queries the repo1.maven.org directory listing directly rather than Maven Central's
search API (which doesn't index the SBT-plugin artifact suffixes reliably).

SBT plugins are published under a cross-version suffix that depends on the SBT major:
`<artifact>_2.12_1.0` for SBT 1.x and `<artifact>_sbt2_3` for SBT 2.x. The suffix is
picked from `project/build.properties`'s `sbt.version`, so the report reflects what this
build can actually resolve — never the other SBT line's version.

Exits non-zero if any lookup failed (network error, or the artifact directory doesn't
exist), so a broken check can't pass as "up to date".
"""
import re
import subprocess
import sys
from pathlib import Path
from typing import List, Optional, Tuple

# Anchored to the start of the line so commented-out `//addSbtPlugin(...)` lines aren't reported.
PLUGIN_RE = re.compile(r'^\s*addSbtPlugin\("([^"]+)"\s*%\s*"([^"]+)"\s*%\s*"([^"]+)"\)', re.MULTILINE)
# A Maven listing entry that looks like a version: dotted numbers plus an optional qualifier.
# Rejects hash-like directories (e.g. `1032048a` under sbt-bloop_2.12_1.0).
VERSION_RE = re.compile(r'href="(\d+(?:\.\d+)+(?:[-+][\w.-]+)?)/"')
PRERELEASE_RE = re.compile(r"(?i)(rc|m\d|alpha|beta|snapshot)")

SUFFIX_BY_SBT_MAJOR = {"1": "_2.12_1.0", "2": "_sbt2_3"}

# Plugins allowed to be reported on a pre-release version when no stable release exists yet.
# Mirrors the ZIO Prelude RC exception in CLAUDE.md's versioning policy for regular dependencies.
# Empty today — every pinned plugin has a stable release. Add an artifact id here only with a
# comment explaining why (e.g. "no stable build for this SBT major version yet").
STABLE_ONLY_EXCEPTIONS: set = set()


def version_key(v: str) -> Tuple[int, ...]:
    return tuple(int(n) for n in re.findall(r"\d+", v))


def sbt_suffix(build_properties: Path) -> str:
    match = re.search(r"^\s*sbt\.version\s*=\s*(\d+)\.", build_properties.read_text(), re.MULTILINE)
    if not match or match.group(1) not in SUFFIX_BY_SBT_MAJOR:
        print(f"Can't read a supported sbt.version from {build_properties}", file=sys.stderr)
        sys.exit(1)
    return SUFFIX_BY_SBT_MAJOR[match.group(1)]


def maven_versions(group: str, artifact: str) -> Optional[List[str]]:
    """Versions listed for the artifact, or None if the listing couldn't be fetched."""
    url = f"https://repo1.maven.org/maven2/{group.replace('.', '/')}/{artifact}/"
    # -f: an HTTP error (e.g. 404) is a non-zero exit, not an HTML error page parsed as empty.
    result = subprocess.run(["curl", "-sfL", url], capture_output=True, text=True, timeout=20)
    if result.returncode != 0:
        return None
    return VERSION_RE.findall(result.stdout)


def main() -> None:
    plugins_file = Path(sys.argv[1] if len(sys.argv) > 1 else "project/plugins.sbt")
    if not plugins_file.exists():
        print(f"No plugins.sbt found at {plugins_file}", file=sys.stderr)
        sys.exit(1)
    suffix = sbt_suffix(plugins_file.parent / "build.properties")

    matches = PLUGIN_RE.findall(plugins_file.read_text())
    if not matches:
        print("No addSbtPlugin declarations found.", file=sys.stderr)
        sys.exit(1)

    print(f"{'PLUGIN':<25} {'CURRENT':<15} {'LATEST':<15}")
    failed = 0
    for group, artifact, current in matches:
        versions = maven_versions(group, artifact + suffix)
        if versions is None:
            failed += 1
            print(f"{artifact:<25} {current:<15} {'?':<15}  <-- lookup failed ({artifact}{suffix})")
            continue
        stable = [v for v in versions if not PRERELEASE_RE.search(v)]
        if stable:
            latest = max(stable, key=version_key)
            marker = "  <-- update available" if latest != current else ""
            print(f"{artifact:<25} {current:<15} {latest:<15}{marker}")
        elif versions and artifact in STABLE_ONLY_EXCEPTIONS:
            latest = max(versions, key=version_key)
            print(f"{artifact:<25} {current:<15} {latest:<15}  <-- pre-release exception")
        else:
            print(f"{artifact:<25} {current:<15} {'no stable release':<15}  <-- needs policy review")

    if failed:
        print(f"{failed} plugin lookup(s) failed — results are incomplete.", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
