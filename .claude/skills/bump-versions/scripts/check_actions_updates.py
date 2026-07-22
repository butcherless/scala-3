#!/usr/bin/env python3
"""Reports the latest available version for each SHA-pinned GitHub Action used in
this repo's workflows and composite actions.

Actions here are pinned by full commit SHA with a trailing `# vX.Y.Z` comment (a
supply-chain-security convention: a mutable tag can be repointed, a commit SHA
can't — never regress to an unpinned tag reference). This script never rewrites
files; it only reports current vs. latest, plus the new SHA to pin to when a bump
is available, so applying one is a mechanical two-part edit (SHA + comment) rather
than a second round of API lookups.
"""
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Optional, Tuple

# Matches `uses: owner/repo@<40-hex-char-sha> # vX.Y.Z` — local actions (`uses: ./...`)
# and Docker actions (`uses: docker://...`) don't match and are skipped.
USES_RE = re.compile(r"uses:\s*([\w.-]+/[\w.-]+)@([0-9a-f]{40})\s*#\s*(\S+)")


def version_key(v: str) -> Tuple[int, ...]:
    return tuple(int(n) for n in re.findall(r"\d+", v))


def gh_api(path: str) -> Optional[object]:
    result = subprocess.run(
        ["curl", "-s", f"https://api.github.com/repos/{path}"],
        capture_output=True,
        text=True,
        timeout=15,
    )
    try:
        data = json.loads(result.stdout)
    except json.JSONDecodeError:
        return None
    if isinstance(data, dict) and data.get("message") == "Not Found":
        return None
    return data


def latest_tag(repo: str) -> Optional[str]:
    release = gh_api(f"{repo}/releases/latest")
    if isinstance(release, dict) and "tag_name" in release:
        return release["tag_name"]
    tags = gh_api(f"{repo}/tags")
    if isinstance(tags, list) and tags:
        return tags[0]["name"]
    return None


def resolve_sha(repo: str, ref: str) -> Optional[str]:
    commit = gh_api(f"{repo}/commits/{ref}")
    if isinstance(commit, dict) and "sha" in commit:
        return commit["sha"]
    return None


def find_pins(root: Path) -> dict:
    """repo -> {sha, comment, locations: [...], inconsistent: bool}"""
    pins: dict = {}
    paths = list((root / ".github" / "workflows").glob("*.yml")) + list(
        (root / ".github" / "actions").rglob("action.yml")
    )
    for path in paths:
        for lineno, line in enumerate(path.read_text().splitlines(), start=1):
            m = USES_RE.search(line)
            if not m:
                continue
            repo, sha, comment = m.groups()
            entry = pins.setdefault(repo, {"sha": sha, "comment": comment, "locations": []})
            if entry["sha"] != sha or entry["comment"] != comment:
                entry["inconsistent"] = True
            entry["locations"].append(f"{path.relative_to(root)}:{lineno}")
    return pins


def main() -> None:
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
    pins = find_pins(root)
    if not pins:
        print("No SHA-pinned GitHub Actions found under .github/.", file=sys.stderr)
        sys.exit(1)

    print(f"{'ACTION':<28} {'CURRENT':<10} {'LATEST':<10} NEW SHA (if update)")
    for repo, info in sorted(pins.items()):
        current = info["comment"].lstrip("v")
        latest = latest_tag(repo)
        if latest is None:
            print(f"{repo:<28} {info['comment']:<10} {'unknown':<10}  <-- no releases/tags found, check manually")
            continue

        latest_norm = latest.lstrip("v")
        marker = ""
        new_sha = ""
        if info.get("inconsistent"):
            marker = "  <-- inconsistent pin across files, see locations below"
        elif version_key(latest_norm) != version_key(current):
            new_sha = resolve_sha(repo, latest) or "COULD NOT RESOLVE"
            marker = "  <-- update available"

        print(f"{repo:<28} {info['comment']:<10} {latest:<10} {new_sha}{marker}")
        if marker:
            for loc in info["locations"]:
                print(f"    used in: {loc}")


if __name__ == "__main__":
    main()
