#!/usr/bin/env bash
set -euo pipefail

# Intent: Bumps the changelog with new values. Should only be run when a release is completed.
# presumes you have npm install -g
# Presumes the following are installed:
# - conventional-changelog (https://www.npmjs.com/package/conventional-changelog-cli)
# - trash (brew install trash)

conventional-changelog -p conventionalcommits -i CHANGELOG.md -s

# This script indents the `### Bug Fixes` and `### Features` lines because I think they should be level 4 headings.
sed -i'.original' -e 's/^### Bug Fixes$/#### Bug Fixes/g' CHANGELOG.md
sed -i'.original' -e 's/^### Features$/#### Features/g' CHANGELOG.md
sed -i'.original' -e 's/^### Code Refactoring$/#### Code Refactoring/g' CHANGELOG.md
sed -i'.original' -e 's/^### ⚠ BREAKING CHANGES$/#### ⚠ BREAKING CHANGES/g' CHANGELOG.md
trash CHANGELOG.md.original

