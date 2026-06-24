## Why

目前 APP 有两个独立入口处理静态素材：
- **照片模式**：选 1 张照片 → 文字编辑 → 导出 **PNG**（3 步）
- **连拍模式**：选 1~20 张照片 → 帧排序+间隔 → 逐张配文 → 导出 **GIF**（4 步）

两者本质上都是"照片 + 文字 → 输出"，只是帧数不同。分拆导致：
1. 用户心智负担：需要先判断"我要做单张表情还是多张表情"
2. 代码重复：文字编辑、颜色/字体/字号控件、导出流程高度重合
3. PNG 单帧与 GIF 单帧功能几乎等价，却用了两套导出逻辑

合并后用户只需选照片（1~20 张），系统自动适配流程。

## What Changes

- **合并入口**：素材选取从 3 入口（视频/照片/连拍）→ 2 入口（视频/图片转GIF）
- **移除 PNG 导出**：静态图片不再输出 PNG，统一输出 GIF（1 帧 GIF 在聊天中同样显示为"静图"）
- **智能跳步**：选取 1 张 → 跳过排序/间隔步骤（3 步）；选取 ≥2 张 → 完整 4 步
- **统一文字编辑**：1 张时用现有文字编辑页（无导航器）；多张时用逐张配文（有导航器+继承规则）
- **统一预览导出**：1 张时单帧预览；多张时轮播预览。导出均为 GIF89a
- **画质**：统一固定 480px（`PHOTO_OUTPUT_WIDTH`），无画质选择器

## Capabilities

### New Capabilities

无新增能力。合并已有能力。

### Modified Capabilities

- `photo-input`：改为支持 1~20 张选取。1 张时直接进入文字编辑；≥2 张时进入帧排序
- `burst-photo-input`：合并入 `photo-input`，不再独立存在
- `image-export`：**移除**。PNG 导出逻辑删除
- `gif-export`：图片转GIF 模式统一走 GIF 导出。1 帧时 `GIF_DELAY_DIVISOR` 设为 0（静态帧）
- `text-overlay`：逐张配文逻辑在 1 张时自动退化为基础文字编辑（无导航器、无继承）
- `frame-ordering`：仅在 count ≥ 2 时显示

### Removed Capabilities

- `image-export`（PNG 导出）

## Impact

- Step 0 UI：3 入口 → 2 入口（🎬 视频 / 🖼 图片转GIF）
- Step 1（图片转GIF）：count=1 跳过；count≥2 显示排序+间隔
- Step 2：count=1 → 基础文字编辑；count≥2 → 逐张配文
- Step 3：count=1 → 单帧预览；count≥2 → 轮播预览
- 导出：移除 `exportPhotoPNG()`，统一 `exportPhotoGIF()`
- state 简化：`mediaType` 枚举从 `VIDEO / PHOTO / BURST` → `VIDEO / PHOTO`
- 文档：`APP_REQUIREMENTS.md` §五（照片模式）和 §六（连拍 GIF）合并为新的 §五（图片转GIF）
- 文档：`APP_DESIGN.md` 对应章节合并
- 文档：`TASKS.md` Phase 1 + Phase 3 → 新的 Phase 1
