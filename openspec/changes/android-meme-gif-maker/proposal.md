## Why

用户需要一款在安卓手机上快速制作表情包GIF的工具。目前市面上多数表情包制作工具要么功能冗余、要么无法精确控制文字位置和裁剪时间。这款APP聚焦核心流程：选视频 → 裁剪片段 → 加文字 → 导出GIF，让用户在几秒内完成表情包制作。

## What Changes

- 新建安卓原生项目，使用 Kotlin + Jetpack Compose，最低支持 Android 10 (API 29)
- 实现从系统相册选取视频功能
- 实现视频时间裁剪功能，最大可选片段不超过3秒
- 实现文字叠加功能：用户输入文字、选择字体、手指拖拽调整位置
- 实现将裁剪后的视频片段+文字合成导出为GIF文件，保存到系统相册
- 核心依赖：Media3 (视频处理)、FFmpeg (GIF编码)、Coil (图片加载)

## Capabilities

### New Capabilities

- `video-selection`: 从系统相册选取视频文件，支持预览
- `video-trimming`: 在视频时间轴上选择起止点，裁剪片段（最大3秒）
- `text-overlay`: 输入文字内容，选择字体样式，手指拖拽定位文字在画面中的位置
- `gif-export`: 将裁剪后的视频片段与文字叠加合成GIF，保存到系统相册

### Modified Capabilities

<!-- 全新项目，无已有能力需修改 -->

## Impact

- 全新项目，无现有代码影响
- 依赖：Android SDK 29+, Kotlin 2.0+, Jetpack Compose, Media3 Transformer, FFmpeg (mobile-ffmpeg)
- 需要申请权限：相册读取、存储写入
