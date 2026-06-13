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

has_local_property() {
  local key="$1"
  [ -f local.properties ] && grep -Eq "^[[:space:]]*${key}[[:space:]]*=[[:space:]]*[^[:space:]]+" local.properties
}

required_config() {
  local env_name="$1"
  local local_key="$2"
  if [ -z "${!env_name:-}" ] && ! has_local_property "$local_key"; then
    echo "Missing required config: set Jenkins parameter/env $env_name or local.properties key $local_key" >&2
    exit 2
  fi
}

test -x "$JAVA_HOME/bin/java"
test -d "$ANDROID_SDK_ROOT"

cd "$PROJECT_DIR"

required_config PGYER_API_KEY "pgyer\\.apiKey"
required_config PGYER_USER_KEY "pgyer\\.userKey"
required_config NOTIFY_DING_WEBHOOK "notify\\.dingWebhook"

if [ "$BUILD_TYPE" = "Release" ]; then
  required_config SIGNING_RELEASE_STORE_FILE "signing\\.release\\.storeFile"
  required_config SIGNING_RELEASE_KEY_ALIAS "signing\\.release\\.keyAlias"
  required_config SIGNING_RELEASE_STORE_PASSWORD "signing\\.release\\.storePassword"
  required_config SIGNING_RELEASE_KEY_PASSWORD "signing\\.release\\.keyPassword"
  if [ -n "${SIGNING_RELEASE_STORE_FILE:-}" ]; then
    test -f "$SIGNING_RELEASE_STORE_FILE"
  fi
fi

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
