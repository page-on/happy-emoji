## Why

目前照片模式只能选一张静态图加文字导出 PNG。用户经常需要把多张照片连在一起做成 GIF（如前后对比、动作分解、表情三连），这是表情包制作的常见场景。增加多选照片合成 GIF 功能，覆盖这一高频需求。

## What Changes

- Step 0 素材选取增加第三个入口"连拍GIF"
- 连拍模式支持一次选取多张照片（最多 20 张）
- 新增帧排序界面：缩略图列表 + 拖拽排序 + 删除单张
- 新增帧间隔调节滑块：500ms ~ 1500ms，默认 500ms
- 新增逐张配文功能：每张照片可独立设置文字/字体/颜色/字号/位置
- 未配文照片自动继承上一张的配文设置
- 连拍模式跳过视频裁剪步骤，流程：选取 → 排序 → 文字 → 导出 GIF
- 预览导出界面自动轮播照片（间隔 = frameDelay）
- 连拍模式画质固定 480px（无画质选择器）
- 文字、字号选择器与现有功能完全复用

## Capabilities

### New Capabilities

- `burst-photo-input`: 批量选取多张照片作为 GIF 帧素材，最多 20 张，支持常见图片格式
- `frame-ordering`: 帧排序与编辑界面 —— 缩略图列表、拖拽排序、删除单张、帧间隔调节、逐张配文

### Modified Capabilities

- `gif-export`: 连拍模式下帧来源从视频改为照片数组，导出逻辑使用 per-photo text + Canvas 合成
- `text-overlay`: 连拍模式文字编辑支持逐张独立设置，state.photoTexts 存储

## Impact

- Step 0 UI：新增"连拍GIF"入口
- Step 1（连拍模式）：新建 frame-ordering 界面，替代视频裁剪
- Step 2：文字编辑新增照片导航器 ← → ，支持逐张配文
- Step 3：预览自动轮播 + per-photo text 联动
- state 新增：`photos[]`、`photoUrls[]`、`frameDelay`(500)、`photoTexts[]`、`currentPhotoIdx`
- 导出：`exportBurstGif()` 独立处理，per-photo 文字渲染，delay = frameDelay/10
