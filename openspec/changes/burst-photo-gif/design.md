## Context

现有架构中，GIF 导出流程为：视频选段 → 逐帧 seek + drawImage → gif.addFrame。连拍模式需要替换帧来源：照片数组 → 逐张 drawImage → gif.addFrame。导出逻辑高度复用。

## Decisions

### 1. 状态管理

```javascript
state.mediaType = 'burst';  // 新增值
state.photos = [];           // File[] 最多 20 张
state.frameDelay = 200;      // 帧间隔 ms，默认 200
```

### 2. 照片处理

- 文件选取：`<input multiple accept="image/*">`
- 存储：每张照片生成 Blob URL（`URL.createObjectURL`），存入 `state.photoUrls[]`
- 排序：用原生 HTML5 Drag and Drop API 实现拖拽排序
- 删除：点击缩略图 × 按钮移除，同步清理 Blob URL

### 3. 帧间隔

- gif.js 的 `delay` 参数单位是 1/100 秒（10ms）
- 用户输入 100-1000ms → `gifDelay = frameDelay / 10`
- 滑块默认 200ms → gifDelay = 20，即 5fps

### 4. 导出复用

```javascript
// 连拍模式导出（exportBurstGif 或 exportGif 分支）
for (let i = 0; i < state.photos.length; i++) {
  const img = new Image();
  img.src = state.photoUrls[i];
  await img.decode();  // 等待解码

  ctx.clearRect(0, 0, w, h);
  const scale = Math.min(w / img.naturalWidth, h / img.naturalHeight);
  ctx.drawImage(img, ...centerInCanvas);

  if (state.textContent) {
    // 复用现有文字渲染
  }
  gif.addFrame(frameCanvas, { copy: true, delay: state.frameDelay / 10 });
}
```

### 5. 流程

```
Step 0: [视频] [照片] [连拍GIF]   ← 三入口
Step 1: (burst模式)
  ┌─────────────────────────────┐
  │  选取多张照片 (最多20张)      │
  │  [缩略图1] [缩略图2] [缩略图3] │  ← 可拖拽排序
  │     ×         ×         ×    │  ← 点击删除
  │  [缩略图4] [+ 添加更多]       │
  │                             │
  │  帧间隔: [500 ══●══ 1500] ms │
  │  预计: 10帧 / 5.0s / 约150KB  │
  └─────────────────────────────┘
Step 2: 逐张配文（←第 N/M 张→ 导航器 + 文字/字体/颜色/字号/拖拽）
Step 3: 预览导出（自动轮播 + per-photo 配文联动 + 固定画质 480px）
```
