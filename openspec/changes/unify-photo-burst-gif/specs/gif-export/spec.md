## gif-export (Modified)

### 图片转GIF 导出

- 输入：`photos[]` + `photoTexts[]` + `frameDelay`
- 输出：GIF89a，宽度 `PHOTO_OUTPUT_WIDTH`px

#### 单张（count == 1）

- 加载照片 → 缩放至 `PHOTO_OUTPUT_WIDTH`px → Canvas 绘制文字 → 1 帧 GIF
- `delay = GIF_DELAY_DIVISOR`（单帧表现为静图）

#### 多张（count ≥ 2）

- 逐张加载 → 缩放 → 逐帧文字渲染 → 多帧 GIF
- `delay = frameDelay / GIF_DELAY_DIVISOR`

### 移除 PNG 导出

- 删除 `image-export` capability
- 删除 `exportPhotoPNG()` 方法
