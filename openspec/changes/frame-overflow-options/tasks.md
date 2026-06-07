## 1. State 扩展

- [x] 1.1 state 新增 `sampleMode` 字段，默认 `'truncate'`（可选 `'uniform'`）

## 2. Step 1 UI 修改

- [x] 2.1 `updateEstimate()` 中帧数溢出时，红色警告下方追加两个按钮 HTML
- [x] 2.2 "缩短选段"按钮点击 → 高亮 trim 滑块（CSS 动画闪烁边框）
- [x] 2.3 "均匀采样"按钮点击 → `state.sampleMode = 'uniform'`，文字变蓝显示"均匀采样自 XX 帧"
- [x] 2.4 均匀采样模式下追加"取消采样"按钮 → 恢复 `truncate` 模式
- [x] 2.5 帧数不再溢出时自动隐藏按钮、恢复 `truncate`

## 3. 导出逻辑

- [x] 3.1 `exportGif()` 帧循环中，当 `sampleMode === 'uniform'` 时，按 `totalSourceFrames / 60` 步长均匀取帧
- [x] 3.2 均匀采样模式输出帧数固定 60，进度条正确显示

## 4. 交互联动

- [x] 4.1 切换采样模式时 `updateEstimate()` 同步刷新 UI 显示
- [x] 4.2 Step 3 预览界面显示当前采样模式（"截断 / 均匀采样"）
