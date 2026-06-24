## photo-input (Modified)

### 选取能力

- 支持从系统相册选取 1~20 张照片（JPG/PNG/HEIC）
- 单张选取与多张选取使用同一入口
- count ≤ `BURST_MAX_COUNT`，超限自动截取前 `BURST_MAX_COUNT` 并 Toast 提示

### 后续流程

- count == 1：跳过帧排序，直接进入文字编辑
- count ≥ 2：进入帧排序界面
