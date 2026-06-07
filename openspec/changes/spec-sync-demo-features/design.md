## Context

所有功能已在 demo.html 中实现，本设计文档记录已实现的决策供后续参考。

## Decisions

### 字体大小滑块

- 范围：16px ~ 80px，默认 36px
- UI：HTML range input，放在颜色选择下方
- 联动：滑块变化 → overlay.style.fontSize 即时更新 + 导出 fontSize 等比缩放
- 导出公式：`fontSize = state.textSize * (outputWidth / 420)`
  - 基准 420px（接近 480px 标准输出宽度）
  - 用户选 36px → 480px 输出 = `36 * 480/420` ≈ 41px
  - 用户选 36px → 360px 输出 = `36 * 360/420` ≈ 31px

### 照片模式隐藏画质

- 触发条件：`isPhoto === true`
- 渲染逻辑：画质选择器 DOM 被 `${!isPhoto ? ... : ''}` 包裹
- 照片输出分辨率：固定使用 `state.qualityLevel` 默认值 `'medium'`（480px）

### 帧溢出处理（已由 frame-overflow-options 实现）

- 溢出时显示两个按钮 + trim track 高亮动画
- uniform 模式：`frameStep = totalSourceFrames / 60`，等间隔采样
