#!/usr/bin/env bash
#
# This downloads the firn binary and builds the docs site.
#
set -euo pipefail
latest_release="$(curl -sL https://raw.githubusercontent.com/theiceshelf/firn/master/clojure/resources/FIRN_VERSION)"

case "$(uname -s)" in
    Linux*)     platform=linux;;
    Darwin*)    platform=mac;;
esac

download_url="https://github.com/theiceshelf/firn/releases/download/v$latest_release/firn-$platform.zip"

echo -e "Downloading Firn from: $download_url."
curl -o "firn-$latest_release-$platform.zip" -sL $download_url
unzip -qqo "firn-$latest_release-$platform.zip"
chmod +x firn
rm "firn-$latest_release-$platform.zip"
./firn build
