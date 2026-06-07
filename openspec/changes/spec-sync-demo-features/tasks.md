## 1. 字体大小控制

- [x] 1.1 state 新增 `textSize` 字段（默认 36）
- [x] 1.2 Step 2 颜色下方添加 range 滑块（16-80px）
- [x] 1.3 滑块 oninput 联动 overlay.style.fontSize 和中间标签显示
- [x] 1.4 Step 2/3 文字叠层 font-size 改为 `state.textSize`
- [x] 1.5 exportGif/exportImage fontSize 公式改为 `state.textSize * (w/420)`
- [x] 1.6 清理废弃的 clamp() 和 fontScale

## 2. 照片模式隐藏画质

- [x] 2.1 Step 3 画质选择器 DOM 用 `!isPhoto` 条件包裹
- [x] 2.2 照片导出分辨率固定为标准 480px

## 3. 帧溢出处理方案

- [x] 3.1 state 新增 `sampleMode` 字段
- [x] 3.2 溢出时显示"缩短选段"+"均匀采样"按钮
- [x] 3.3 缩短选段 → trim track 紫色闪烁高亮
- [x] 3.4 均匀采样 → 蓝色提示 + 取消采样按钮
- [x] 3.5 不溢出时自动隐藏按钮、重置 sampleMode
- [x] 3.6 exportGif 均匀采样逻辑：frameStep = totalSourceFrames / 60
- [x] 3.7 Step 3 显示当前采样模式标签
