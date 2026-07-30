#!/usr/bin/env bash
set -u

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
STATE_DIR="$PROJECT_DIR/.claude/state"
PENDING_FILE="$STATE_DIR/gradle-check-pending"
LAST_INPUT_FILE="$STATE_DIR/gradle-check-last-edit.json"

mkdir -p "$STATE_DIR" || exit 0

INPUT="$(cat || true)"
printf '%s\n' "$INPUT" > "$LAST_INPUT_FILE"

FILE_PATH="$(
  INPUT_VALUE="$INPUT" python3 - <<'PY'
import json
import os

try:
    payload = json.loads(os.environ.get("INPUT_VALUE", ""))
except Exception:
    payload = {}

print(payload.get("tool_input", {}).get("file_path", ""))
PY
)"

case "$FILE_PATH" in
  *.kt|*.java|*.xml|*.gradle|*.kts|*.toml|*.properties|*.aidl|*.c|*.cc|*.cpp|*.h|*.hpp|*.pro|*/CMakeLists.txt)
    date '+%Y-%m-%d %H:%M:%S %z' > "$PENDING_FILE"
    ;;
esac

exit 0
