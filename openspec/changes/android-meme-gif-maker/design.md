## Context

全新安卓项目，从零搭建。目标用户是普通手机用户，需要简单直观地制作表情包GIF。项目使用 Kotlin + Jetpack Compose，最低支持 Android 10 (API 29)。核心流程：选视频 → 裁剪 → 加文字 → 导出GIF。

## Goals / Non-Goals

**Goals:**
- 提供完整的面部包制作流程：选取视频、裁剪片段（≤3秒）、添加文字、导出GIF
- 文字支持手指拖拽定位，所见即所得
- 输出GIF自动保存到系统相册
- 界面简洁直观，符合 Material 3 设计规范

**Non-Goals:**
- 不支持拍摄新视频（仅从相册选取）
- 不支持多段文字叠加
- 不支持贴纸、滤镜等高级编辑
- 不包含社交分享功能（仅保存到相册）
- 不支持视频裁剪后保留音频（GIF无音频）

## Decisions

### 1. 架构：MVVM + Compose Navigation

使用 ViewModel 管理状态，Compose 管理UI，Navigation Compose 处理页面路由。理由：这是 Google 推荐的现代安卓架构，代码清晰、可测试。

备选：MVI —— 对于此规模项目过度设计。

### 2. 视频处理：Media3 Transformer

使用 `androidx.media3:media3-transformer` 进行视频裁剪（trim）。它将视频裁剪后输出为 MP4 文件段。理由：Google 官方库，稳定且兼容性好。

备选：直接使用 MediaCodec —— API 层级过低，开发复杂。

### 3. GIF 编码：纯 Kotlin GIF 编码器

采用自研的逐帧 GIF 编码器，将视频帧提取后编码为 GIF。理由：避免引入 FFmpeg 原生库带来的体积膨胀（~30MB）和复杂配置。

实现方式：
- 使用 MediaMetadataRetriever 提取视频帧（每100ms一帧，约10fps）
- 使用 Android 原生 API（LZW 压缩 + GIF89a 格式）逐帧写入 GIF 文件

备选：FFmpeg (mobile-ffmpeg) —— 功能强大但包体积增加约30MB。

### 4. 文字渲染与定位：Compose Canvas 叠加

在视频预览画面上方叠加 Canvas 层，使用 `Modifier.pointerInput` 实现拖拽定位。文字使用 Compose `TextMeasurer` 测量尺寸，`drawText` 绘制在 Canvas 上。

最终导出时，使用 Android Canvas + Bitmap 在每一帧上绘制文字后写入 GIF。

### 5. 字体选择：预置字体列表

内置 4-6 种常用中文字体（如系统默认、黑体、楷体、仿宋等），不使用 Downloadable Fonts。理由：无网络也能使用，保证离线体验。

### 6. 页面流程：4步向导

```
选择视频 → 裁剪时间 → 添加文字 → 预览导出
```

每步一个独立页面，通过顶部进度指示器展示当前位置。最后一步展示GIF预览并提供导出按钮。

备选：全部功能在一个页面 —— 对于手机屏幕太小，操作冲突。

### 7. 权限处理

- 相册读取：使用 `PhotoPicker` (API 33+) 或 `READ_EXTERNAL_STORAGE` (API 29-32)
- 存储写入：使用 `MediaStore` API，无需 `WRITE_EXTERNAL_STORAGE` 权限（API 29+）

## Risks / Trade-offs

- **[风险] GIF 文件较大**：3秒10fps的GIF约为30-50帧，可能达到5-10MB。→ 降低帧率至8fps，限制GIF尺寸为480p。
- **[风险] 视频帧提取耗时**：大视频提取帧可能阻塞主线程。→ 所有处理流程放在后台协程中，显示进度。
- **[风险] 纯 Kotlin GIF 编码性能**：相比FFmpeg，编码速度可能较慢。→ 使用 `Dispatchers.Default` 处理，3秒视频预计10-15秒导出。
- **[权衡] 不使用FFmpeg**：减少30MB包体积，但牺牲了编码性能和未来扩展性。
