#!/usr/bin/env bash
set -u

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
STATE_DIR="$PROJECT_DIR/.claude/state"
PENDING_FILE="$STATE_DIR/gradle-check-pending"
LOG_FILE="$STATE_DIR/gradle-check-last.log"
COMMAND="${CLAUDE_GRADLE_CHECK_CMD:-./gradlew :demo:assembleDebug}"

cat >/dev/null || true

if [ ! -f "$PENDING_FILE" ]; then
  exit 0
fi

mkdir -p "$STATE_DIR" || exit 0

if [ ! -f "$PROJECT_DIR/gradlew" ]; then
  rm -f "$PENDING_FILE"
  exit 0
fi

cd "$PROJECT_DIR" || exit 0

{
  printf 'Command: %s\n' "$COMMAND"
  printf 'Started: %s\n\n' "$(date '+%Y-%m-%d %H:%M:%S %z')"
} > "$LOG_FILE"

OUTPUT="$(bash -lc "$COMMAND" 2>&1)"
STATUS=$?

{
  printf '%s\n' "$OUTPUT"
  printf '\nFinished: %s\nExit code: %s\n' "$(date '+%Y-%m-%d %H:%M:%S %z')" "$STATUS"
} >> "$LOG_FILE"

if [ "$STATUS" -eq 0 ]; then
  rm -f "$PENDING_FILE"
  COMMAND_VALUE="$COMMAND" python3 - <<'PY'
import json
import os

command = os.environ.get("COMMAND_VALUE", "./gradlew :demo:assembleDebug")

print(json.dumps({
    "systemMessage": f"Gradle check passed: {command}",
    "suppressOutput": False
}, ensure_ascii=False))
PY
  exit 0
fi

TAIL_OUTPUT="$(printf '%s\n' "$OUTPUT" | tail -n 180)"

COMMAND_VALUE="$COMMAND" \
STATUS_VALUE="$STATUS" \
OUTPUT_VALUE="$TAIL_OUTPUT" \
LOG_FILE_VALUE="$LOG_FILE" \
python3 - <<'PY'
import json
import os

command = os.environ.get("COMMAND_VALUE", "./gradlew :demo:assembleDebug")
status = os.environ.get("STATUS_VALUE", "unknown")
output = os.environ.get("OUTPUT_VALUE", "")
log_file = os.environ.get("LOG_FILE_VALUE", "")

reason = (
    "Gradle check failed after code changes.\n"
    f"Command: `{command}`\n"
    f"Exit code: {status}\n"
    f"Full log: `{log_file}`\n\n"
    "Fix the build errors, then let the Stop hook run this Gradle check again.\n\n"
    "Last output:\n"
    "```text\n"
    f"{output}\n"
    "```"
)

print(json.dumps({
    "decision": "block",
    "reason": reason
}, ensure_ascii=False))
PY

exit 0
