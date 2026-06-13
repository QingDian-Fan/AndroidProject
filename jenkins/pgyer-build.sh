#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${PROJECT_DIR:-/Users/dian/AndroidStudioProjets/DemoProjects/GitHubProjects/AndroidProject}"
BUILD_TYPE="${BUILD_TYPE:-Debug}"
JDK_HOME="${JDK_HOME:-/Users/dian/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home}"
ANDROID_SDK_ROOT_OVERRIDE="${ANDROID_SDK_ROOT_OVERRIDE:-/Users/dian/Library/Android/sdk}"

export JAVA_HOME="$JDK_HOME"
export ANDROID_HOME="$ANDROID_SDK_ROOT_OVERRIDE"
export ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT_OVERRIDE"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"

required_env() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    echo "Missing required Jenkins parameter/env: $name" >&2
    exit 2
  fi
}

required_env PGYER_API_KEY
required_env PGYER_USER_KEY
required_env NOTIFY_DING_WEBHOOK

if [ "$BUILD_TYPE" = "Release" ]; then
  required_env SIGNING_RELEASE_STORE_FILE
  required_env SIGNING_RELEASE_KEY_ALIAS
  required_env SIGNING_RELEASE_STORE_PASSWORD
  required_env SIGNING_RELEASE_KEY_PASSWORD
  test -f "$SIGNING_RELEASE_STORE_FILE"
fi

test -x "$JAVA_HOME/bin/java"
test -d "$ANDROID_SDK_ROOT"

cd "$PROJECT_DIR"
git fetch origin master
git checkout master
git pull --ff-only origin master

chmod +x ./gradlew
"$JAVA_HOME/bin/java" -version

case "$BUILD_TYPE" in
  Release)
    ./gradlew --no-daemon clean pgyerBuildRelease
    ;;
  Debug)
    ./gradlew --no-daemon clean pgyerBuildDebug
    ;;
  *)
    echo "Unsupported BUILD_TYPE: $BUILD_TYPE" >&2
    exit 2
    ;;
esac
