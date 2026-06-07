## Why

以下功能已在 demo.html 中实现但未形成需求文档，需要补录到 openspec 中以确保代码与文档一致：

1. **字体大小滑块**：Step 2 文字编辑界面新增字号滑块（16px-80px），用户可调节文字显示大小，同时影响预览和导出
2. **照片模式隐藏画质**：照片模式输出 PNG（无损），画质选择 bezel 在照片模式下自动隐藏
3. **帧溢出处理方案**：帧数超过 60 帧上限时，提供"缩短选段"（引导用户手动调整）和"均匀采样"（系统等间隔取 60 帧）两个操作按钮

## What Changes

- 新增 `font-size-control` capability：文字大小调节
- 修改 `quality-control`：画质选择器在照片模式下自动隐藏
- 修改 `speed-control`：帧数溢出时提供两种处理方案（已在 `frame-overflow-options` 中定义，此处仅同步主 spec）

## Capabilities

### New Capabilities

- `font-size-control`: 文字大小滑块控制，范围 16-80px，默认 36px，联动预览和导出

### Modified Capabilities

- `quality-control`: 照片模式下画质选择器隐藏，输出分辨率固定为标准 480px
- `speed-control`: 帧数溢出时增加"缩短选段"和"均匀采样"两个处理选项
- `text-overlay`: 文字叠加层 font-size 从固定值改为由 `state.textSize` 驱动

## Impact

- `state` 对象：已有 `textSize: 36`、`sampleMode: 'truncate'`
- Step 2 UI：字号滑块（颜色下方）
- Step 3 UI：画质选择器按 mediaType 条件渲染
- exportGif/exportImage：fontSize 从 `state.textSize * (w/420)` 计算
