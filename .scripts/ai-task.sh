#!/usr/bin/env bash

set -Eeuo pipefail

# =========================================================
# 工作流：
#   .scripts/README.md + .scripts/images/
#       → Codex CLI 自动实现
#       → Claude Code 自动只读审查
#
# 目录要求：
#   项目根目录/
#   ├── AGENTS.md
#   ├── CLAUDE.md
#   └── .scripts/
#       ├── README.md
#       ├── images/
#       └── ai-task.sh
#
# 使用：
#   chmod +x .scripts/ai-task.sh
#   ./.scripts/ai-task.sh
#
# 可选环境变量：
#   CODEX_MODEL=模型名
#   CODEX_REASONING_EFFORT=Codex 推理强度
#   CLAUDE_MODEL=模型名
#   CLAUDE_EFFORT=Claude 推理强度
#   CLAUDE_MAX_TURNS=30
#   AI_REVIEW_DIR=审查记录根目录
#   DINGTALK_WEBHOOK_URL=钉钉机器人 Webhook
#   CODEX_SESSION_NAME=Codex 固定会话名称
#   CLAUDE_SESSION_NAME=Claude 固定会话名称
# =========================================================

# ---------- 输出辅助 ----------

info() {
  printf '\n\033[1;34m%s\033[0m\n' "$1"
}

warn() {
  printf '\033[1;33m警告：%s\033[0m\n' "$1" >&2
}

error() {
  printf '\033[1;31m错误：%s\033[0m\n' "$1" >&2
}

json_escape() {
  local value="${1:-}"

  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\t'/\\t}"

  printf '%s' "$value"
}

send_dingtalk_review_notification() {
  local content
  local payload
  local response
  local title

  DINGTALK_NOTIFICATION_ATTEMPTED=true

  title="AI 代码审查通知"
  content="### AI 代码审查通知

**任务名称：**

$TASK_NAME

**审查 Log-Id：**

$RUN_ID

**Codex 模型及推理强度：**

${CODEX_RUNTIME_MODEL} - ${CODEX_RUNTIME_EFFORT}

**Cluade 模型及推理强度：**

${CLAUDE_RUNTIME_MODEL} - ${CLAUDE_RUNTIME_EFFORT}

**Codex 执行完毕时间：**

$CODEX_FINISHED_AT

**Claude 审核完毕时间：**

$CLAUDE_FINISHED_AT"

  payload="{\"msgtype\":\"markdown\",\"markdown\":{\"title\":\"$(json_escape "$title")\",\"text\":\"$(json_escape "$content")\"}}"

  if ! response="$(
    curl \
      --silent \
      --show-error \
      --fail \
      --connect-timeout 10 \
      --max-time 30 \
      --header 'Content-Type: application/json; charset=utf-8' \
      --data "$payload" \
      "$DINGTALK_WEBHOOK_URL"
  )"; then
    error "钉钉通知发送失败。"
    return 1
  fi

  if ! grep -Eq '"errcode"[[:space:]]*:[[:space:]]*0' <<<"$response"; then
    error "钉钉机器人拒绝了通知：$response"
    return 1
  fi

  echo "钉钉通知已发送。"
}

notify_failure_on_exit() {
  local exit_code="${1:-1}"
  local failed_at

  if [[ "$exit_code" -eq 0 ]] || [[ "${DINGTALK_NOTIFICATION_ATTEMPTED:-false}" == true ]]; then
    return
  fi

  # 避免通知命令自身失败时再次触发 EXIT 通知。
  trap - EXIT
  failed_at="$(date '+%Y-%m-%d %H:%M:%S %z')"

  case "${CURRENT_STAGE:-初始化}" in
    Codex*)
      CODEX_FINISHED_AT="${failed_at}（失败，退出码：${exit_code}）"
      CLAUDE_FINISHED_AT="未执行（Codex 失败）"
      ;;
    Claude*)
      CLAUDE_FINISHED_AT="${failed_at}（失败，退出码：${exit_code}）"
      ;;
    *)
      if [[ "${CODEX_FINISHED_AT:-未执行}" == "未执行" ]]; then
        CODEX_FINISHED_AT="未执行（${CURRENT_STAGE:-初始化}阶段失败，时间：${failed_at}，退出码：${exit_code}）"
        CLAUDE_FINISHED_AT="未执行"
      else
        CLAUDE_FINISHED_AT="未执行（${CURRENT_STAGE:-未知}阶段失败，时间：${failed_at}，退出码：${exit_code}）"
      fi
      ;;
  esac

  warn "任务执行失败，正在发送钉钉通知。"

  if ! send_dingtalk_review_notification; then
    warn "任务失败通知未能发送。"
  fi
}

update_codex_runtime_metadata() {
  local log_file="$1"
  local runtime_model
  local runtime_effort

  if [[ ! -f "$log_file" ]]; then
    return
  fi

  runtime_model="$(
    sed -nE 's/^[[:space:]]*model:[[:space:]]*(.+)[[:space:]]*$/\1/p' "$log_file" |
      sed -n '1p'
  )"
  runtime_effort="$(
    sed -nE 's/^[[:space:]]*reasoning effort:[[:space:]]*(.+)[[:space:]]*$/\1/p' "$log_file" |
      sed -n '1p'
  )"

  if [[ -n "$runtime_model" ]]; then
    CODEX_RUNTIME_MODEL="$runtime_model"
  fi
  if [[ -n "$runtime_effort" ]]; then
    CODEX_RUNTIME_EFFORT="$runtime_effort"
  fi
}

update_claude_runtime_metadata() {
  local session_id="$1"
  local claude_projects_dir
  local session_file
  local runtime_model
  local runtime_effort

  claude_projects_dir="${CLAUDE_CONFIG_DIR:-${HOME:-}/.claude}/projects"

  if [[ ! -d "$claude_projects_dir" ]]; then
    return
  fi

  session_file="$(
    find "$claude_projects_dir" \
      -type f \
      -name "${session_id}.jsonl" \
      -print \
      -quit 2>/dev/null
  )"

  if [[ -z "$session_file" ]] || [[ ! -f "$session_file" ]]; then
    return
  fi

  runtime_model="$(
    grep -Eo '"model"[[:space:]]*:[[:space:]]*"[^"]+"' "$session_file" |
      tail -n 1 |
      sed -E 's/^"model"[[:space:]]*:[[:space:]]*"([^"]+)"$/\1/' ||
      true
  )"
  runtime_effort="$(
    grep -Eo '"effort"[[:space:]]*:[[:space:]]*"[^"]+"' "$session_file" |
      tail -n 1 |
      sed -E 's/^"effort"[[:space:]]*:[[:space:]]*"([^"]+)"$/\1/' ||
      true
  )"

  if [[ -n "$runtime_model" ]]; then
    CLAUDE_RUNTIME_MODEL="$runtime_model"
  fi
  if [[ -n "$runtime_effort" ]]; then
    CLAUDE_RUNTIME_EFFORT="$runtime_effort"
  fi
}

set_codex_session_name() {
  local session_id="$1"
  local session_name="$2"
  local app_server_dir
  local app_server_input
  local app_server_log
  local app_server_pid
  local app_server_output
  local app_server_response
  local app_server_exit_code
  local initialize_succeeded=false
  local rename_response_received=false
  local attempt

  set +e

  app_server_dir="$(mktemp -d "${TMPDIR:-/tmp}/ai-task-codex-name.XXXXXX")"
  app_server_input="$app_server_dir/input"
  app_server_log="$app_server_dir/output.log"

  if [[ -z "$app_server_dir" ]] || ! mkfifo "$app_server_input"; then
    set -e
    error "Codex 会话命名失败：无法创建临时通信目录。"
    return 1
  fi

  codex app-server --stdio <"$app_server_input" >"$app_server_log" 2>&1 &
  app_server_pid=$!

  # 通过文件描述符保持 app-server 的标准输入打开，等待 initialize
  # 响应后再发送命名请求，避免请求尚未处理时因 EOF 提前退出。
  exec 3>"$app_server_input"
  printf '%s\n' \
    '{"id":1,"method":"initialize","params":{"clientInfo":{"name":"ai-task-script","version":"1.0.0"},"capabilities":{"experimentalApi":true}}}' >&3

  for ((attempt = 0; attempt < 100; attempt++)); do
    if grep -Eq '"id"[[:space:]]*:[[:space:]]*1' "$app_server_log"; then
      initialize_succeeded=true
      break
    fi

    if ! kill -0 "$app_server_pid" 2>/dev/null; then
      break
    fi

    sleep 0.1
  done

  if [[ "$initialize_succeeded" == true ]]; then
    printf '%s\n' \
      '{"method":"initialized"}' \
      "{\"id\":2,\"method\":\"thread/name/set\",\"params\":{\"threadId\":\"$(json_escape "$session_id")\",\"name\":\"$(json_escape "$session_name")\"}}" >&3

    for ((attempt = 0; attempt < 100; attempt++)); do
      if grep -Eq '"id"[[:space:]]*:[[:space:]]*2' "$app_server_log"; then
        rename_response_received=true
        break
      fi

      if ! kill -0 "$app_server_pid" 2>/dev/null; then
        break
      fi

      sleep 0.1
    done
  fi

  exec 3>&-
  wait "$app_server_pid"
  app_server_exit_code=$?
  app_server_output="$(<"$app_server_log")"
  app_server_response="$(
    grep -E '"id"[[:space:]]*:[[:space:]]*2' "$app_server_log" |
      tail -n 1
  )"

  rm -f "$app_server_input" "$app_server_log"
  rmdir "$app_server_dir"

  set -e

  if [[ $app_server_exit_code -ne 0 ]]; then
    error "Codex 会话命名失败：app-server 退出码 $app_server_exit_code"
    echo "$app_server_output" >&2
    return 1
  fi

  if [[ "$initialize_succeeded" != true ]] ||
    [[ "$rename_response_received" != true ]] ||
    grep -Eq '"error"[[:space:]]*:' <<<"$app_server_response"; then
    error "Codex 会话命名失败：未收到成功响应。"
    echo "$app_server_output" >&2
    return 1
  fi

  echo "Codex 固定会话：${session_name}（${session_id}）"
}

# ---------- 路径 ----------

SCRIPT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1
  pwd
)"

PROJECT_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || true)"

if [[ -z "$PROJECT_ROOT" ]]; then
  error "当前脚本不在 Git 项目中：$SCRIPT_DIR"
  exit 1
fi

cd "$PROJECT_ROOT"

REQUIREMENT_SOURCE="$SCRIPT_DIR/README.md"
IMAGE_DIR="$SCRIPT_DIR/images"
AGENTS_FILE="$PROJECT_ROOT/AGENTS.md"
CLAUDE_FILE="$PROJECT_ROOT/CLAUDE.md"
DINGTALK_WEBHOOK_FILE="$SCRIPT_DIR/.dingtalk-webhook"
CODEX_SESSION_ID_FILE="$SCRIPT_DIR/.ai-codex-session-id"
CLAUDE_SESSION_ID_FILE="$SCRIPT_DIR/.ai-claude-session-id"
CODEX_SESSION_NAME="${CODEX_SESSION_NAME:-ai-codex-需求实现}"
CLAUDE_SESSION_NAME="${CLAUDE_SESSION_NAME:-ai-calude-代码审查}"

if [[ -z "${DINGTALK_WEBHOOK_URL:-}" ]] && [[ -f "$DINGTALK_WEBHOOK_FILE" ]]; then
  DINGTALK_WEBHOOK_URL="$(<"$DINGTALK_WEBHOOK_FILE")"
fi

# ---------- 基础检查 ----------

if [[ ! -f "$AGENTS_FILE" ]]; then
  error "项目根目录缺少 AGENTS.md"
  exit 1
fi

if [[ ! -f "$CLAUDE_FILE" ]]; then
  error "项目根目录缺少 CLAUDE.md"
  exit 1
fi

if [[ ! -f "$REQUIREMENT_SOURCE" ]]; then
  error "需求文件不存在：$REQUIREMENT_SOURCE"
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  error "未找到 curl 命令，无法发送钉钉通知。"
  exit 1
fi

if [[ -z "${DINGTALK_WEBHOOK_URL:-}" ]]; then
  error "缺少钉钉机器人 Webhook。"
  echo "请设置 DINGTALK_WEBHOOK_URL，或写入：$DINGTALK_WEBHOOK_FILE"
  exit 1
fi

if [[ "$DINGTALK_WEBHOOK_URL" != "https://oapi.dingtalk.com/robot/send?access_token="* ]]; then
  error "钉钉机器人 Webhook 格式不正确。"
  exit 1
fi

REQUIREMENT="$(cat "$REQUIREMENT_SOURCE")"

if ! LC_ALL=C grep -q '[^[:space:]]' "$REQUIREMENT_SOURCE"; then
  error "$REQUIREMENT_SOURCE 中的需求不能为空。"
  exit 1
fi

TASK_NAME="$(
  sed -nE \
    -e 's/^[[:space:]#]*需求：[[:space:]]*//p' \
    -e 's/^[[:space:]#]*需求:[[:space:]]*//p' \
    "$REQUIREMENT_SOURCE" |
    sed -n '1p' |
    sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//'
)"

if [[ -z "$TASK_NAME" ]]; then
  error "未能从 $REQUIREMENT_SOURCE 中提取任务名称。"
  echo "请添加标题，例如：# 需求：服务接口地址改为可配置"
  exit 1
fi

RUN_ID="$(date '+%Y%m%d-%H%M%S')"
CODEX_RUNTIME_MODEL="${CODEX_MODEL:-未获取}"
CODEX_RUNTIME_EFFORT="${CODEX_REASONING_EFFORT:-未获取}"
CLAUDE_RUNTIME_MODEL="${CLAUDE_MODEL:-未获取}"
CLAUDE_RUNTIME_EFFORT="${CLAUDE_EFFORT:-未获取}"
CODEX_FINISHED_AT="未执行"
CLAUDE_FINISHED_AT="未执行"
CURRENT_STAGE="前置检查"
DINGTALK_NOTIFICATION_ATTEMPTED=false

# 任务名称和运行 ID 已就绪后，任何非零退出都发送一次失败通知。
trap 'notify_failure_on_exit "$?"' EXIT

if ! command -v codex >/dev/null 2>&1; then
  error "未找到 codex 命令。请先安装并登录 Codex CLI。"
  exit 1
fi

if ! command -v claude >/dev/null 2>&1; then
  error "未找到 claude 命令。请先安装并登录 Claude Code。"
  exit 1
fi

# 在 Codex 开始实现前确认 Claude Code 已登录，避免实现完成后才因
# OAuth 过期而无法进入审查阶段。
CLAUDE_AUTH_HELP="$(claude auth --help 2>&1 || true)"

if grep -q -- 'status' <<<"$CLAUDE_AUTH_HELP"; then
  CLAUDE_AUTH_STATUS="$(claude auth status 2>&1 || true)"

  if ! grep -Eq '"loggedIn"[[:space:]]*:[[:space:]]*true' <<<"$CLAUDE_AUTH_STATUS"; then
    error "Claude Code 当前未登录或 OAuth 会话已过期。"
    echo "请先在终端中执行："
    echo "  claude auth login --claudeai"
    echo
    echo "登录完成后确认："
    echo "  claude auth status"
    echo
    echo "认证状态："
    echo "$CLAUDE_AUTH_STATUS"
    exit 1
  fi
else
  warn "当前 Claude Code 未提供 auth status，无法在任务开始前检查登录状态。"
fi

# ---------- 收集需求图片 ----------

IMAGE_PATHS=()

if [[ -d "$IMAGE_DIR" ]]; then
  while IFS= read -r -d '' image_file; do
    IMAGE_PATHS+=("$image_file")
  done < <(
    find "$IMAGE_DIR" \
      -maxdepth 1 \
      -type f \
      \( \
        -iname '*.png' \
        -o -iname '*.jpg' \
        -o -iname '*.jpeg' \
        -o -iname '*.webp' \
        -o -iname '*.gif' \
      \) \
      -print0
  )
fi

IMAGE_LIST_TEXT="无"

if [[ ${#IMAGE_PATHS[@]} -gt 0 ]]; then
  IMAGE_LIST_TEXT=""

  for image_file in "${IMAGE_PATHS[@]}"; do
    relative_image="${image_file#"$PROJECT_ROOT"/}"
    IMAGE_LIST_TEXT="${IMAGE_LIST_TEXT}
- ${relative_image}"
  done
fi

# ---------- Git 工作区检查 ----------

# README.md 和 images/ 是本次需求输入，可以变化。
# 其他文件必须干净，避免 Claude 把旧改动误认为 Codex 本次改动。
is_requirement_input_path() {
  case "$1" in
    ".scripts/README.md"|".scripts/images/"*)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

collect_dirty_paths() {
  {
    git diff --name-only
    git diff --cached --name-only
    git ls-files --others --exclude-standard
  } | LC_ALL=C sort -u
}

UNRELATED_DIRTY_PATHS=()

while IFS= read -r path; do
  [[ -z "$path" ]] && continue

  if ! is_requirement_input_path "$path"; then
    UNRELATED_DIRTY_PATHS+=("$path")
  fi
done < <(collect_dirty_paths)

if [[ ${#UNRELATED_DIRTY_PATHS[@]} -gt 0 ]]; then
  error "运行前存在与本次需求无关的未提交修改："
  printf '  - %s\n' "${UNRELATED_DIRTY_PATHS[@]}"
  echo
  echo "请先提交或暂存这些修改，例如："
  echo '  git stash push -u -m "before ai task"'
  echo
  echo "允许保留的需求输入只有："
  echo "  .scripts/README.md"
  echo "  .scripts/images/*"
  exit 1
fi

BASE_COMMIT="$(git rev-parse HEAD)"
BASE_BRANCH="$(git branch --show-current)"
PROJECT_NAME="$(basename "$PROJECT_ROOT")"
CURRENT_STAGE="运行记录初始化"

if [[ -z "$BASE_BRANCH" ]]; then
  BASE_BRANCH="DETACHED_HEAD"
fi

# 日志保存在项目内已忽略的目录，避免污染 Git Diff。
RUN_DIR="${AI_REVIEW_DIR:-$PROJECT_ROOT/.ai-code-reviews}/${RUN_ID}"
RUN_IMAGE_DIR="$RUN_DIR/images"
REQUIREMENT_FILE="$RUN_DIR/requirement.md"
CODEX_LOG="$RUN_DIR/codex-result.log"
CLAUDE_REVIEW_FILE="$RUN_DIR/claude-review.md"
CHANGED_FILES_FILE="$RUN_DIR/changed-files.txt"

mkdir -p "$RUN_DIR" "$RUN_IMAGE_DIR"

cat > "$REQUIREMENT_FILE" <<EOF
# 本次开发需求

$REQUIREMENT

## 参考图片

$IMAGE_LIST_TEXT

## 基准信息

- 项目：$PROJECT_NAME
- 分支：$BASE_BRANCH
- 开始提交：$BASE_COMMIT
- 执行时间：$RUN_ID
EOF

# 归档图片，防止后续替换需求图片后无法追溯。
if [[ ${#IMAGE_PATHS[@]} -gt 0 ]]; then
  for image_file in "${IMAGE_PATHS[@]}"; do
    cp "$image_file" "$RUN_IMAGE_DIR/"
  done
fi

info "AI 开发任务"
echo "项目：$PROJECT_NAME"
echo "分支：$BASE_BRANCH"
echo "基准：$BASE_COMMIT"
echo "需求：$REQUIREMENT_SOURCE"
echo "参考图片：${#IMAGE_PATHS[@]} 张"
echo "记录目录：$RUN_DIR"

# =========================================================
# 第一阶段：Codex 实现
# =========================================================

CODEX_PROMPT=$(cat <<EOF
你是本项目的功能开发工程师。

请先读取并严格遵守项目根目录的 AGENTS.md。

本次需求文件：
.scripts/README.md

本次需求内容：

<requirement>
$REQUIREMENT
</requirement>

参考图片：
$IMAGE_LIST_TEXT

执行要求：

1. 开始前阅读 AGENTS.md、需求文件、参考图片和相关代码。
2. 参考图片是需求的一部分；涉及 UI 时必须结合图片理解布局、样式和交互。
3. 阅读相关模块、现有调用链和相似实现后再修改。
4. 严格按照需求实现，不扩大需求范围。
5. 优先复用项目现有架构、基础类、工具类和代码风格。
6. 不进行与需求无关的重构或大范围格式化。
7. 不升级无关依赖，不修改无关模块。
8. 不执行 git add、git commit、git push、git reset、git restore、git checkout 或 git clean。
9. 不修改 AGENTS.md、CLAUDE.md、.scripts/README.md、.scripts/images/ 和 .scripts/ai-task.sh。
10. 新增功能必须处理异常、空值、生命周期、并发和资源释放。
11. 根据实际影响范围运行对应模块的编译、Lint 或测试。
12. 即使验证失败，也要如实记录，不得隐瞒或伪造成功结果。

完成后输出：

1. 修改的文件；
2. 每个文件的修改内容；
3. 实现方案；
4. 执行的验证命令；
5. 验证结果；
6. 尚未解决的风险；
7. 需求是否全部完成。
EOF
)

info "第一阶段：Codex 开始实现"
CURRENT_STAGE="Codex 执行"

CODEX_ROOT_HELP="$(codex --help 2>&1 || true)"
CODEX_EXEC_HELP="$(codex exec --help 2>&1 || true)"
CODEX_RESUME_HELP="$(codex exec resume --help 2>&1 || true)"

# Codex 顶层参数必须放在 exec 之前，exec 子命令参数放在 exec 之后。
# 不要合并两级帮助信息，否则会把顶层参数误判为 exec 参数。
CODEX_GLOBAL_ARGS=()
CODEX_EXEC_ARGS=(exec)
CODEX_RESUME_ARGS=(exec resume)

if grep -q -- '--sandbox' <<<"$CODEX_ROOT_HELP"; then
  CODEX_GLOBAL_ARGS+=(--sandbox workspace-write)
elif grep -q -- '--sandbox' <<<"$CODEX_EXEC_HELP"; then
  CODEX_EXEC_ARGS+=(--sandbox workspace-write)
fi

if grep -q -- '--ask-for-approval' <<<"$CODEX_ROOT_HELP"; then
  CODEX_GLOBAL_ARGS+=(--ask-for-approval never)
elif grep -q -- '--ask-for-approval' <<<"$CODEX_EXEC_HELP"; then
  CODEX_EXEC_ARGS+=(--ask-for-approval never)
elif grep -q -- '--approval-policy' <<<"$CODEX_ROOT_HELP"; then
  CODEX_GLOBAL_ARGS+=(--approval-policy never)
elif grep -q -- '--approval-policy' <<<"$CODEX_EXEC_HELP"; then
  CODEX_EXEC_ARGS+=(--approval-policy never)
elif grep -q -- '--full-auto' <<<"$CODEX_EXEC_HELP"; then
  CODEX_EXEC_ARGS+=(--full-auto)
elif grep -q -- '--full-auto' <<<"$CODEX_ROOT_HELP"; then
  CODEX_GLOBAL_ARGS+=(--full-auto)
else
  warn "当前 Codex CLI 未检测到自动审批参数，执行期间可能要求人工确认。"
fi

if [[ -n "${CODEX_MODEL:-}" ]] && grep -q -- '--model' <<<"$CODEX_ROOT_HELP"; then
  CODEX_GLOBAL_ARGS+=(--model "$CODEX_MODEL")
elif [[ -n "${CODEX_MODEL:-}" ]]; then
  if grep -q -- '--model' <<<"$CODEX_EXEC_HELP"; then
    CODEX_EXEC_ARGS+=(--model "$CODEX_MODEL")
  fi
  if grep -q -- '--model' <<<"$CODEX_RESUME_HELP"; then
    CODEX_RESUME_ARGS+=(--model "$CODEX_MODEL")
  fi
fi

if [[ -n "${CODEX_REASONING_EFFORT:-}" ]]; then
  CODEX_GLOBAL_ARGS+=(
    --config
    "model_reasoning_effort=\"$CODEX_REASONING_EFFORT\""
  )
fi

# 按当前 CLI 支持情况附加图片。
if [[ ${#IMAGE_PATHS[@]} -gt 0 ]]; then
  if grep -q -- '--image' <<<"$CODEX_EXEC_HELP" &&
    grep -q -- '--image' <<<"$CODEX_RESUME_HELP"; then
    for image_file in "${IMAGE_PATHS[@]}"; do
      CODEX_EXEC_ARGS+=(--image "$image_file")
      CODEX_RESUME_ARGS+=(--image "$image_file")
    done
  elif grep -Eq '(^|[[:space:],])-i([[:space:],]|$)' <<<"$CODEX_EXEC_HELP" &&
    grep -Eq '(^|[[:space:],])-i([[:space:],]|$)' <<<"$CODEX_RESUME_HELP"; then
    for image_file in "${IMAGE_PATHS[@]}"; do
      CODEX_EXEC_ARGS+=(-i "$image_file")
      CODEX_RESUME_ARGS+=(-i "$image_file")
    done
  else
    error "当前 Codex CLI 无法在新建和恢复会话时同时附加需求图片。"
    echo "请先执行以下命令确认或升级 Codex CLI："
    echo "  codex exec --help"
    echo "  codex exec resume --help"
    exit 1
  fi
fi

CODEX_SESSION_ID=""
CODEX_IS_NEW_SESSION=true

if [[ -s "$CODEX_SESSION_ID_FILE" ]]; then
  CODEX_SESSION_ID="$(tr -d '[:space:]' <"$CODEX_SESSION_ID_FILE")"

  if ! grep -Eq '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$' <<<"$CODEX_SESSION_ID"; then
    error "Codex 固定会话 ID 文件格式无效：$CODEX_SESSION_ID_FILE"
    exit 1
  fi

  CODEX_IS_NEW_SESSION=false
  echo "恢复 Codex 固定会话：${CODEX_SESSION_NAME}（${CODEX_SESSION_ID}）"
else
  echo "首次创建 Codex 固定会话：$CODEX_SESSION_NAME"
fi

set +e

if [[ "$CODEX_IS_NEW_SESSION" == true ]]; then
  codex \
    "${CODEX_GLOBAL_ARGS[@]}" \
    "${CODEX_EXEC_ARGS[@]}" \
    - \
    <<<"$CODEX_PROMPT" \
    2>&1 | tee "$CODEX_LOG"
  CODEX_EXIT_CODE=${PIPESTATUS[0]}
else
  codex \
    "${CODEX_GLOBAL_ARGS[@]}" \
    "${CODEX_RESUME_ARGS[@]}" \
    "$CODEX_SESSION_ID" \
    - \
    <<<"$CODEX_PROMPT" \
    2>&1 | tee "$CODEX_LOG"
  CODEX_EXIT_CODE=${PIPESTATUS[0]}
fi

set -e

update_codex_runtime_metadata "$CODEX_LOG"

CODEX_SESSION_PERSIST_EXIT_CODE=0

if [[ "$CODEX_IS_NEW_SESSION" == true ]]; then
  CODEX_SESSION_ID="$(
    sed -nE \
      's/^[[:space:]]*session id:[[:space:]]*([0-9a-fA-F-]+).*$/\1/p' \
      "$CODEX_LOG" |
      sed -n '1p'
  )"

  if grep -Eq '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$' <<<"$CODEX_SESSION_ID"; then
    # Codex 在开始执行时便创建了会话。即使实现中途失败，也要保留
    # 这个 ID，确保下一次脚本执行仍进入同一份记忆。
    printf '%s\n' "$CODEX_SESSION_ID" >"$CODEX_SESSION_ID_FILE"
  elif [[ $CODEX_EXIT_CODE -eq 0 ]]; then
    error "无法从 Codex 日志中提取新会话 ID。"
    CODEX_SESSION_ID=""
    CODEX_SESSION_PERSIST_EXIT_CODE=1
  else
    warn "Codex 尚未输出有效会话 ID，本次失败没有可恢复的会话。"
    CODEX_SESSION_ID=""
  fi
fi

if [[ -n "$CODEX_SESSION_ID" ]] &&
  ! set_codex_session_name "$CODEX_SESSION_ID" "$CODEX_SESSION_NAME"; then
  CODEX_SESSION_PERSIST_EXIT_CODE=1
fi

if [[ $CODEX_EXIT_CODE -ne 0 ]]; then
  error "Codex 执行失败，退出码：$CODEX_EXIT_CODE"
  echo "Codex 日志：$CODEX_LOG"
  echo "Claude Code 审查未启动，以免把未完成实现误判为最终结果。"
  exit "$CODEX_EXIT_CODE"
fi

CODEX_FINISHED_AT="$(date '+%Y-%m-%d %H:%M:%S %z')"
CURRENT_STAGE="Codex 会话持久化"

if [[ $CODEX_SESSION_PERSIST_EXIT_CODE -ne 0 ]]; then
  exit "$CODEX_SESSION_PERSIST_EXIT_CODE"
fi

echo
echo "Codex 已结束。"
CURRENT_STAGE="变更收集"

# ---------- 收集本次可审查变更 ----------

collect_reviewable_changed_paths() {
  {
    git diff --name-only "$BASE_COMMIT"
    git ls-files --others --exclude-standard
  } | LC_ALL=C sort -u
}

REVIEWABLE_CHANGED_PATHS=()

while IFS= read -r path; do
  [[ -z "$path" ]] && continue

  if ! is_requirement_input_path "$path"; then
    REVIEWABLE_CHANGED_PATHS+=("$path")
  fi
done < <(collect_reviewable_changed_paths)

if [[ ${#REVIEWABLE_CHANGED_PATHS[@]} -eq 0 ]]; then
  warn "Codex 没有产生可审查的项目代码变更。"
  echo "Codex 日志：$CODEX_LOG"
  echo "Claude Code 审查未启动。"
  exit 0
fi

printf '%s\n' "${REVIEWABLE_CHANGED_PATHS[@]}" > "$CHANGED_FILES_FILE"

echo
echo "本次待审查变更："
printf '  - %s\n' "${REVIEWABLE_CHANGED_PATHS[@]}"

# =========================================================
# 第二阶段：Claude Code 自动只读审查
# =========================================================

CHANGED_FILES_TEXT="$(cat "$CHANGED_FILES_FILE")"

CLAUDE_REVIEW_PROMPT=$(cat <<EOF
你是独立的高级 Android 代码审查工程师。

这是只读代码审查任务。Codex 已根据需求完成实现。

请先读取并遵守项目根目录的 CLAUDE.md，并进入其中定义的“审查模式”。

本次需求：

<requirement>
$REQUIREMENT
</requirement>

参考图片：
$IMAGE_LIST_TEXT

基准信息：

- 项目：$PROJECT_NAME
- 初始分支：$BASE_BRANCH
- 基准提交：$BASE_COMMIT

脚本检测到的待审查文件：

<changed_files>
$CHANGED_FILES_TEXT
</changed_files>

强制要求：

1. 只进行审查，不得修改、创建、移动或删除任何项目文件。
2. 不得生成或应用补丁。
3. 不得执行 git add、git commit、git push、git reset、git restore、git checkout 或 git clean。
4. 不要相信 Codex 的自我评价，必须独立检查。
5. 检查从 $BASE_COMMIT 到当前工作区的全部代码变化。
6. 使用 git status --short 检查未跟踪文件，并使用 Read 或 Glob 逐个读取。
7. 使用 git diff $BASE_COMMIT 检查已跟踪文件的完整变更。
8. 阅读变更代码周围的调用链和上下文，不能只看 Diff。
9. 对照需求文字和参考图片逐条检查。
10. .scripts/README.md 和 .scripts/images/ 是需求输入，不属于 Codex 实现代码，不要把它们作为代码问题报告。
11. 只报告高置信度、能够说明触发条件和实际影响的问题。
12. 不要为了凑数量报告纯风格问题。
13. 可以运行必要的 Gradle 编译、Lint 或测试，但不得修改源码。
14. 若命令因权限或环境无法执行，必须在“未验证内容”中说明。

重点检查：

- 需求是否全部实现；
- UI 是否符合参考图片和交互要求；
- 是否存在需求理解偏差或无关修改；
- 崩溃、空指针和异常处理；
- Activity、Fragment、View 和 Context 生命周期；
- 协程、线程、并发安全和主线程 IO；
- 文件流、Cursor、监听器和其他资源释放；
- Android minSdk 21 兼容性；
- WorkManager、定时任务、重试和幂等性；
- 数据丢失、状态不一致和重复提交；
- 权限、安全、隐私和敏感信息；
- Java/Kotlin 空安全及互操作；
- ProGuard/R8 兼容性；
- 测试覆盖和回归风险。

请优先执行：

1. git status --short
2. git diff --check $BASE_COMMIT
3. git diff --stat $BASE_COMMIT
4. git diff $BASE_COMMIT

输出格式：

# Claude Code 审查报告

## 审查结论

只能选择一个：

- 通过
- 有阻塞问题
- 有非阻塞问题
- 无法完成审查

## 问题列表

按照严重等级输出：

### P0 严重

数据丢失、安全漏洞、大范围崩溃或核心功能完全不可用。

### P1 高

主要功能错误、稳定崩溃、明显不符合需求或严重回归。

### P2 中

边界错误、兼容性问题、资源泄漏、并发风险或必要测试缺失。

每个问题必须包含：

- 严重等级
- 文件路径
- 代码位置
- 问题描述
- 触发条件
- 实际影响
- 修复建议

## 需求覆盖情况

逐条说明需求是否实现。

## 已执行检查

列出实际执行的命令及结果。

## 未验证内容

说明因为环境、设备、账号、网络或依赖限制而无法验证的部分。

如果没有发现明确问题，写明：

“未发现阻塞性问题。”

不要直接修改代码。
EOF
)

info "第二阶段：Claude Code 自动只读审查"
CURRENT_STAGE="Claude 审查"

CLAUDE_HELP="$(claude --help 2>&1 || true)"
CLAUDE_ARGS=(-p --output-format text)

if grep -q -- '--max-turns' <<<"$CLAUDE_HELP"; then
  CLAUDE_ARGS+=(--max-turns "${CLAUDE_MAX_TURNS:-30}")
fi

if [[ -n "${CLAUDE_MODEL:-}" ]] && grep -q -- '--model' <<<"$CLAUDE_HELP"; then
  CLAUDE_ARGS+=(--model "$CLAUDE_MODEL")
fi

if [[ -n "${CLAUDE_EFFORT:-}" ]]; then
  if grep -q -- '--effort' <<<"$CLAUDE_HELP"; then
    CLAUDE_ARGS+=(--effort "$CLAUDE_EFFORT")
  else
    error "当前 Claude Code 不支持 --effort，无法设置推理强度。"
    exit 1
  fi
fi

CLAUDE_SESSION_ID=""
CLAUDE_IS_NEW_SESSION=true

if [[ -s "$CLAUDE_SESSION_ID_FILE" ]]; then
  CLAUDE_SESSION_ID="$(tr -d '[:space:]' <"$CLAUDE_SESSION_ID_FILE")"

  if ! grep -Eq '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$' <<<"$CLAUDE_SESSION_ID"; then
    error "Claude 固定会话 ID 文件格式无效：$CLAUDE_SESSION_ID_FILE"
    exit 1
  fi

  if ! grep -q -- '--resume' <<<"$CLAUDE_HELP"; then
    error "当前 Claude Code 不支持恢复固定会话。"
    exit 1
  fi

  CLAUDE_IS_NEW_SESSION=false
  CLAUDE_ARGS+=(--resume "$CLAUDE_SESSION_ID" --name "$CLAUDE_SESSION_NAME")
  echo "恢复 Claude 固定会话：${CLAUDE_SESSION_NAME}（${CLAUDE_SESSION_ID}）"
else
  if ! command -v uuidgen >/dev/null 2>&1; then
    error "未找到 uuidgen，无法创建 Claude 固定会话 ID。"
    exit 1
  fi

  if ! grep -q -- '--session-id' <<<"$CLAUDE_HELP" ||
    ! grep -q -- '--name' <<<"$CLAUDE_HELP"; then
    error "当前 Claude Code 不支持固定会话 ID 或会话名称。"
    exit 1
  fi

  CLAUDE_SESSION_ID="$(uuidgen | tr 'A-F' 'a-f')"
  CLAUDE_ARGS+=(--session-id "$CLAUDE_SESSION_ID" --name "$CLAUDE_SESSION_NAME")
  echo "首次创建 Claude 固定会话：${CLAUDE_SESSION_NAME}（${CLAUDE_SESSION_ID}）"
fi

# 仅允许读取、Git 查询和 Gradle 验证。
CLAUDE_ARGS+=(
  --allowedTools
  "Read"
  "Grep"
  "Glob"
  "Bash(git status:*)"
  "Bash(git diff:*)"
  "Bash(git log:*)"
  "Bash(git show:*)"
  "Bash(git ls-files:*)"
  "Bash(git rev-parse:*)"
  "Bash(./gradlew:*)"
  --disallowedTools
  "Write"
  "Edit"
  "NotebookEdit"
  "Bash(git add:*)"
  "Bash(git commit:*)"
  "Bash(git push:*)"
  "Bash(git reset:*)"
  "Bash(git restore:*)"
  "Bash(git checkout:*)"
  "Bash(git clean:*)"
  "Bash(rm:*)"
  "Bash(mv:*)"
  "Bash(cp:*)"
)

# Claude 会话 ID 由脚本预先生成，因此在启动 Claude 前即可保存。
# 即使审查中途失败，下一次执行仍会恢复同一份审查记忆。
if [[ "$CLAUDE_IS_NEW_SESSION" == true ]]; then
  printf '%s\n' "$CLAUDE_SESSION_ID" >"$CLAUDE_SESSION_ID_FILE"
fi

set +e

# --allowedTools / --disallowedTools 都是可接收多个值的参数。
# 如果把提示词放在命令行末尾，Claude CLI 可能将其误解析为工具权限规则。
# 通过标准输入传递提示词，避免参数边界歧义。
claude "${CLAUDE_ARGS[@]}" \
  <<<"$CLAUDE_REVIEW_PROMPT" \
  2>&1 | tee "$CLAUDE_REVIEW_FILE"
CLAUDE_EXIT_CODE=${PIPESTATUS[0]}

set -e

update_claude_runtime_metadata "$CLAUDE_SESSION_ID"

info "执行完成"
echo "需求归档：$REQUIREMENT_FILE"
echo "图片归档：$RUN_IMAGE_DIR"
echo "变更清单：$CHANGED_FILES_FILE"
echo "Codex 日志：$CODEX_LOG"
echo "Claude 审查：$CLAUDE_REVIEW_FILE"

if [[ $CLAUDE_EXIT_CODE -ne 0 ]]; then
  error "Claude Code 审查执行失败，退出码：$CLAUDE_EXIT_CODE"
  exit "$CLAUDE_EXIT_CODE"
fi

CLAUDE_FINISHED_AT="$(date '+%Y-%m-%d %H:%M:%S %z')"
CURRENT_STAGE="钉钉通知"

echo
echo "Claude Code 已完成自动审查。"

if ! send_dingtalk_review_notification; then
  exit 1
fi
