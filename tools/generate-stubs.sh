#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd "$script_dir/.." && pwd)"

echo "Generating system_libs stubs using tools/stubs..."
cd "$script_dir/stubs"
"$repo_dir/gradlew" generateAllStubs
echo "Stub jars successfully placed into $repo_dir/system_libs:"
ls -la "$repo_dir/system_libs"
