# Demo GIF/Video Bug Fixes — Debug Skill

## 适用范围

网页版表情包制作器 Demo (`demo.html`)，使用 `gif.js` 做 GIF 编码，HTML5 `<video>` 做视频播放，以 `file://` 协议本地打开。

---

## 问题 1：视频选取后裁剪界面画面黑

### 现象
Step 1（裁剪时间）的预览区域全黑，看不到视频帧。

### 根因
浏览器不会渲染从未播放过的 `<video>` 元素的画面。视频设为隐藏/极小尺寸（如 `width:1px;height:1px`）时，浏览器会跳帧解码优化性能，导致 canvas 也无法通过 `drawImage(video)` 获取画面。

### 修复
预览区使用 **可见全尺寸 video + canvas 叠层** 方案：

```html
<div class="preview-area" id="previewArea" style="position:relative">
  <video id="videoPlayer" src="..." preload="auto" muted loop playsinline
    style="position:absolute;top:0;left:0;width:100%;height:100%;object-fit:contain">
  </video>
  <canvas id="trimPreviewCanvas"
    style="position:absolute;top:0;left:0;width:100%;height:100%">
  </canvas>
</div>
```

- video 元素全尺寸可见，确保浏览器正常解码帧
- canvas 叠在 video 上方，通过 `seeked` 事件 + `drawTrimFrame()` 绘制当前帧
- 预览播放时通过 `requestAnimationFrame` 持续刷帧

### 关键代码位置
`demo.html:renderStep1()` — `drawTrimFrame()` 函数

---

## 问题 2：GIF 导出失败 —— canvas width/height 为 0

### 现象
```
Failed to execute 'drawImage' on 'CanvasRenderingContext2D':
The image argument is a canvas element with a width or height of 0.
```

### 根因
`exportGif()` 中新建 `<video>` 元素加载与 Step 3 预览区相同的 Blob URL。两个 video 共享解码器资源时，新 video 的 `videoWidth`/`videoHeight` 可能为 0，导致 `h = Math.round(w * 0/0)` = NaN → canvas 尺寸为 0。

### 修复
不复用新建 video，改为直接使用 Step 3 预览中已加载完成的 `#mediaPlayer` 元素：

```javascript
video = videoEl;  // videoEl = $('#mediaPlayer')，已在 Step 3 播放中
video.pause();    // 暂停即可用于帧提取
```

### 关键代码位置
`demo.html:exportGif()` — video 变量赋值

---

## 问题 3：Edge 浏览器 Worker 跨域报错

### 现象
```
Failed to construct 'Worker': Script at
'https://cdn.jsdelivr.net/npm/gif.js@0.2.0/dist/gif.worker.js'
cannot be accessed from origin 'null'.
```

### 根因
`file://` 协议下页面 origin 为 `null`，无法跨域加载 CDN 上的外部 Worker 脚本（浏览器安全策略）。

### 修复（初版 → 废弃）
设为 `workers: 0` 主线程编码。但 gif.js 主线程用 `setTimeout(fn, 0)` 分片编码，NeuQuant 量化 + LZW 压缩极慢，导致后续问题 4。

### 关键代码位置
`demo.html:exportGif()` — GIF 构造函数

---

## 问题 4：GIF 编码超时（30 秒）

### 现象
主线程模式下帧提取完成（进度条 100%），但 GIF 编码阶段超过 30 秒超时。

### 根因
`workers: 0` 模式下 gif.js 用 `setTimeout(fn, 0)` 在主线程逐帧进行 NeuQuant 色彩量化 + LZW 压缩。480p 帧处理耗时，且 `setTimeout` 在后台标签会被 Chrome 节流到 1 秒最小间隔。

### 修复
将 gif.worker.js 全文嵌入 HTML，通过 Blob URL 恢复 Web Worker：

```html
<script type="text/plain" id="gifWorkerSrc">
// gif.worker.js 完整源码 (~20KB)
</script>
```

```javascript
// 页面初始化时创建同源 Blob URL
var src = document.getElementById('gifWorkerSrc');
var blob = new Blob([src.textContent], { type: 'application/javascript' });
GIF_WORKER_URL = URL.createObjectURL(blob);

// GIF 构造时使用 Worker
gif = new GIF({
  workers: 2,
  quality: 10,
  width: w, height: h,
  repeat: 0,
  workerScript: GIF_WORKER_URL
});
```

Blob URL 与页面同源（均为 `null`），Worker 可正常创建。编码在独立线程并行执行，< 1 秒完成。

### 关键代码位置
- `demo.html:8` — `<script type="text/plain" id="gifWorkerSrc">`
- `demo.html:145-151` — `GIF_WORKER_URL` 初始化
- `demo.html:639-646` — GIF 构造函数

---

## 问题 5：帧提取卡死（Promise 永久 pending）

### 现象
一直显示"正在生成GIF..."，进度条不动。

### 根因
`await new Promise(r => { video.onseeked = r; video.currentTime = t; })` 中：
1. Handler 在 `currentTime` 之后绑定 → 竞态条件
2. 当 `t` 等于当前 time 时，`seeked` 不触发 → 永久挂起
3. 视频不支持精确 seek 到目标时间 → `seeked` 不触发

### 修复
三层超时保护 + 事件绑定顺序修正：

```javascript
// 1. 单帧 seek 超时（5 秒）+ error 监听
if (Math.abs(video.currentTime - t) > 0.001) {
  const seekPromise = new Promise((resolve, reject) => {
    video.addEventListener('seeked', resolve, { once: true });  // 先绑定
    video.addEventListener('error', reject, { once: true });
    video.currentTime = t;  // 后设时间
  });
  await Promise.race([seekPromise, timeout(5000)]).finally(cleanup);
}

// 2. 整体帧提取超时（60 秒）
const genStartTime = Date.now();
for (...) {
  if (Date.now() - genStartTime > 60000) throw new Error('生成超时');
  // ...
}

// 3. GIF 编码超时（30 秒）
await new Promise((resolve, reject) => {
  const encTimeout = setTimeout(() => reject(...), 30000);
  gif.on('finished', blob => { clearTimeout(encTimeout); resolve(); });
  gif.render();
});
```

### 关键代码位置
`demo.html:exportGif()` — 帧循环、编码 Promise

---

## 通用调试技巧总结

| 问题类型 | 排查方向 |
|---------|---------|
| Video 画面黑 | 检查 video 是否可见、是否 preload、是否 play 过。Safari 需要 `playsinline` |
| Canvas drawImage 失败 | 检查 video.videoWidth/Height 是否为 0，readyState >= 2 |
| Worker 跨域 | `file://` 下 origin = `null`，必须用 Blob URL 或同源路径 |
| Promise 永久 pending | 检查事件绑定顺序（先绑后触发），加超时兜底 |
| 编码超时 | Worker 模式比主线程快 10-100 倍，优先用 Worker |
