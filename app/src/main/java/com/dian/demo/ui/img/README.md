# 媒体选择模块（img）

一套基于 `MediaStore` 的本地媒体选择组件，支持**图片 / 视频 / 图片+视频**三种选取模式、单选与多选、相册分组切换、以及大图预览。缩略图加载使用 Glide。

## 目录结构

| 文件 | 说明 |
| --- | --- |
| `ImageSelectUtil.kt` | **对外入口**，链式 API，配置参数后启动选择页 |
| `MediaType.kt` | 媒体类型枚举：`IMAGE` / `VIDEO` / `ALL` |
| `ImageSelectActivity.kt` | 选择主页面：扫描媒体库、网格展示、相册切换、确认/取消 |
| `ImageSelectAdapter.kt` | 选择网格适配器（单选 / 多选、视频角标） |
| `ImageSelectListener.kt` | 回调接口：`ImageSelectListener`（确认）、`ImageCancelListener`（取消） |
| `AlbumDialogFragment.kt` | 相册列表底部弹窗（BottomSheet） |
| `AlbumAdapter.kt` | 相册列表适配器 |
| `AlbumInfo.kt` | 相册数据模型（`@Parcelize`） |
| `ImagePreviewActivity.kt` | 大图预览页（ViewPager2） |
| `ImagePreviewAdapter.kt` | 预览页适配器 |
| `RatioFrameLayout.kt` | 按指定宽高比测量的 `FrameLayout`，用于保证缩略图为正方形等比例 |

---

## 快速使用

```kotlin
ImageSelectUtil()
    .setActivity(this)               // 必填，宿主 Activity
    .setMediaType(MediaType.ALL)     // 选填，默认 MediaType.IMAGE
    .setMaxSelect(5)                 // 选填，最大可选数量，默认 9；为 1 时为单选
    .setColumn(3)                    // 选填，每行列数，仅允许 2~4，默认 3
    .setSelectList(mSelectList)      // 选填，回显已选中的路径
    .setSelectListener(object : ImageSelectListener {
        override fun selectListener(selectList: ArrayList<String>) {
            // 点击「确认」后回调，selectList 为选中的文件路径
        }
    })
    .setCancelListener(object : ImageCancelListener {
        override fun cancel() {
            // 返回/取消时回调
        }
    })
    .create()                        // 启动选择页
```

> `create()` 内部会校验 `mActivity` 是否为空（为空抛 `RuntimeException`），随后调用 `ImageSelectActivity.start(...)`。

---

## `ImageSelectUtil` API

| 方法 | 说明 | 默认值 |
| --- | --- | --- |
| `setActivity(activity)` | 宿主 Activity（必填） | — |
| `setMediaType(type)` | 选取类型：`IMAGE` / `VIDEO` / `ALL` | `IMAGE` |
| `setMaxSelect(n)` | 最大可选数量；为 `1` 时切换为单选模式 | `9` |
| `setColumn(n)` | 网格列数，超出 `2~4` 会回退为 `3` | `3` |
| `setSelectList(list)` | 初始已选中的路径列表（回显） | `null` |
| `setSelectListener(l)` | 确认回调 | `null` |
| `setCancelListener(l)` | 取消回调 | `null` |
| `create()` | 启动选择页 | — |

### 单选 vs 多选
- `maxSelect == 1`：单选，点击即选中（替换上一个），常用于头像等场景。
- `maxSelect > 1`：多选，选中项显示序号，达到上限时弹 Toast 提示。

---

## 选取类型 `MediaType`

| 枚举值 | 含义 | 标题栏文案 | “全部”相册名 |
| --- | --- | --- | --- |
| `IMAGE` | 仅图片 | 图片选择 | 所有图片 |
| `VIDEO` | 仅视频 | 视频选择 | 所有视频 |
| `ALL` | 图片 + 视频 | 选择媒体 | 全部 |

- 视频项会在缩略图中央叠加**播放角标**（`icon_video_play`），并把路径记录在 `videoPaths` 集合中用于区分。
- 视频缩略图由 Glide 自动解码首帧，数据模型统一为文件路径 `ArrayList<String>`，图片与视频无差别。

---

## 内部流程

1. `ImageSelectUtil.create()` → `ImageSelectActivity.start(...)`。
2. `start()` 上标注 `@CheckPermissions(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, isMust = true)`，由 AOP 切面在启动前完成运行时权限申请，授权后才真正 `startActivity`。
3. `ImageSelectActivity` 读取 Intent 参数，先用空列表创建并挂载适配器，再在 `lifecycleScope` + `Dispatchers.IO` 后台执行 `getAllMedia()`（MediaStore 查询 + 逐条 `File.exists()` 校验），完成后回主线程刷新，**避免大图库阻塞主线程导致 ANR**。
4. 点击标题栏右侧文案 → `initAlbum()` 弹出 `AlbumDialogFragment` 切换相册（按文件所在目录名分组）。
5. 点击缩略图 → `ImagePreviewActivity` 大图预览；点击右下角勾选框 → 选中/取消。
6. 点击「确认」→ 回调 `ImageSelectListener.selectListener(选中路径)` 并 `finish()`；返回/取消 → 回调 `ImageCancelListener.cancel()`。

---

## 权限说明

`AndroidManifest.xml` 已声明：

- `READ_MEDIA_IMAGES`、`READ_MEDIA_VIDEO`（Android 13+ / API 33+）
- `READ_EXTERNAL_STORAGE`（`maxSdkVersion=32`）、`WRITE_EXTERNAL_STORAGE`（`maxSdkVersion=28`）

`PermissionConversionUtil` 会按系统版本自动取舍：API > 32 使用 `READ_MEDIA_*`，否则使用 `READ/WRITE_EXTERNAL_STORAGE`。因此业务侧无需关心版本差异，权限申请统一交给 `@CheckPermissions` 切面。

---

## 过滤规则（`getAllMedia`）

扫描 `MediaStore.Files`，按所选 `MediaType` 拼接查询条件，并满足：

- 文件大小 `SIZE > 0` 且 `>= 1KB`（过滤无效/极小文件）
- `DATA` 路径与 `MIME_TYPE` 非空
- 文件真实存在且为普通文件（`File.exists() && isFile`）
- 按 `DATE_MODIFIED DESC` 倒序（最新在前）
- 以文件父目录名作为相册名分组

---

## RatioFrameLayout

按 `app:sizeRatio="宽:高"`（如 `1:1`）等比例测量自身，配合 `wrap_content` / 固定边使缩略图始终保持指定比例。可通过代码 `setSizeRatio(w, h)` 动态设置。

---

## 已知限制 / 可扩展点

- 预览页 `ImagePreviewActivity` 目前对视频仅展示**首帧静帧**，不支持播放；如需播放可接入 `ui/view/video/VideoPlayerView`。
- 计数文案 `image_select_total = "共 %d 张"` 对视频/混合模式中的「张」略不贴切，可按需改为中性量词。
- 选择结果通过静态回调（`companion object` 中的 `listener` / `cancelListener`）传递，已在 `onDestroy()`（`isFinishing` 时）清空以避免内存泄漏。若后续要彻底去除静态状态，可迁移到 `registerForActivityResult` —— 但需同时迁移 `@CheckPermissions` 的权限前置校验。