#!/usr/bin/env python3
"""Reports the latest available version for each GitHub Action used in this repo's
workflows and composite actions.

Actions here are pinned to a floating major-version tag (`owner/repo@vN`), matching
the repo convention — `.github/dependabot.yml`'s weekly `github-actions` update is
what keeps them current; this script is the on-demand single-pass equivalent. It
never rewrites files; it only reports current vs. latest so applying a bump is a
one-line tag edit (in every file that references the action).

GitHub is queried via `gh api` (authenticated: 5000 req/hr, and no token handling
here) — same convention as the bump-versions skill's inline checks. `gh` must be
logged in (`gh auth status`); the script checks that first and exits non-zero if not.
It also exits non-zero if any action's lookup failed, so a broken check can't pass as
"up to date".
"""
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Optional, Tuple

# Matches `uses: owner/repo@<ref>` where <ref> is a version tag (`v7`, `v7.0.1`).
# Local actions (`uses: ./...`), Docker actions (`uses: docker://...`), and
# branch/SHA refs don't match and are skipped.
USES_RE = re.compile(r"uses:\s*([\w.-]+/[\w.-]+)@(v[0-9][\w.-]*)")


def version_key(v: str) -> Tuple[int, ...]:
    return tuple(int(n) for n in re.findall(r"\d+", v))


def gh_api(path: str) -> Optional[object]:
    """`gh api repos/<path>` -> parsed JSON, or None on any failure (404, no auth, …)."""
    result = subprocess.run(
        ["gh", "api", f"repos/{path}"],
        capture_output=True,
        text=True,
        timeout=20,
    )
    if result.returncode != 0:
        return None
    try:
        return json.loads(result.stdout)
    except json.JSONDecodeError:
        return None


def latest_tag(repo: str) -> Optional[str]:
    release = gh_api(f"{repo}/releases/latest")
    if isinstance(release, dict) and "tag_name" in release:
        return release["tag_name"]
    tags = gh_api(f"{repo}/tags")
    if isinstance(tags, list) and tags:
        return tags[0]["name"]
    return None


def find_refs(root: Path) -> dict:
    """repo -> {refs: {ref -> [locations]}}"""
    refs: dict = {}
    paths = list((root / ".github" / "workflows").glob("*.yml")) + list(
        (root / ".github" / "actions").rglob("action.yml")
    )
    for path in paths:
        for lineno, line in enumerate(path.read_text().splitlines(), start=1):
            m = USES_RE.search(line)
            if not m:
                continue
            repo, ref = m.groups()
            entry = refs.setdefault(repo, {})
            entry.setdefault(ref, []).append(f"{path.relative_to(root)}:{lineno}")
    return refs


def main() -> None:
    auth = subprocess.run(["gh", "auth", "status"], capture_output=True, text=True, timeout=20)
    if auth.returncode != 0:
        print("`gh` is not logged in (`gh auth status` failed) — can't query GitHub.", file=sys.stderr)
        sys.exit(1)

    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
    refs = find_refs(root)
    if not refs:
        print("No version-tagged GitHub Actions found under .github/.", file=sys.stderr)
        sys.exit(1)

    print(f"{'ACTION':<28} {'CURRENT':<12} {'LATEST':<12} NOTE")
    failed = 0
    for repo, used in sorted(refs.items()):
        current_refs = sorted(used)
        current = ", ".join(current_refs)
        latest = latest_tag(repo)
        if latest is None:
            failed += 1
            print(f"{repo:<28} {current:<12} {'?':<12} <-- lookup failed (no releases/tags, or API error)")
            continue

        marker = ""
        if len(current_refs) > 1:
            marker = "  <-- INCONSISTENT ref across files, see locations below"
        else:
            # Compare only the major component — the repo pins to a floating `vN`.
            cur_major = version_key(current_refs[0])[:1]
            latest_major = version_key(latest)[:1]
            if latest_major > cur_major:
                marker = f"  <-- MAJOR update available (review {repo} release notes first)"

        print(f"{repo:<28} {current:<12} {latest:<12}{marker}")
        if marker:
            for ref in current_refs:
                for loc in used[ref]:
                    print(f"    used in: {loc}")

    if failed:
        print(f"{failed} action lookup(s) failed — results are incomplete.", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
