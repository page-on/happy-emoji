## 1. 项目初始化

- [ ] 1.1 创建 Android 项目，配置 Kotlin + Jetpack Compose，minSdk 29，targetSdk 35
- [ ] 1.2 添加核心依赖：Media3 Transformer、Navigation Compose、Material 3、Coil
- [ ] 1.3 创建项目包结构：ui/screens、ui/components、viewmodel、domain、data
- [ ] 1.4 配置主题和基础样式（Material 3，深色/浅色主题）

## 2. 视频选取

- [ ] 2.1 实现 PhotoPicker 启动逻辑，仅过滤视频文件
- [ ] 2.2 处理权限请求（API 29-32 使用 READ_EXTERNAL_STORAGE，API 33+ 使用 PhotoPicker 无需权限）
- [ ] 2.3 创建视频选取页面 UI（空状态引导 + 选取按钮）
- [ ] 2.4 获取选取视频的 URI 和基本元数据（时长、分辨率）

## 3. 视频裁剪

- [ ] 3.1 使用 MediaMetadataRetriever 提取视频缩略图帧，生成时间轴预览条
- [ ] 3.2 实现 RangeSlider 时间轴组件（起点/终点拖拽，最大范围3秒限制）
- [ ] 3.3 实现裁剪片段实时预览（循环播放选中区间）
- [ ] 3.4 创建裁剪页面 UI：时间轴 + 预览区 + 确认按钮
- [ ] 3.5 实现裁剪结果存储（记录起止时间戳，传递到下一页面）

## 4. 文字叠加

- [ ] 4.1 创建文字输入组件（TextField，最多50字限制）
- [ ] 4.2 实现字体选择器（底部弹窗列表，至少4种字体：系统默认、黑体、楷体、仿宋）
- [ ] 4.3 实现颜色选择器（预设6色：白/黑/红/黄/蓝/绿）
- [ ] 4.4 实现 Canvas 文字渲染层（在视频预览上方叠加文字）
- [ ] 4.5 实现拖拽定位（Modifier.pointerInput，限制不超出画面边界）
- [ ] 4.6 实现双击居中快捷操作
- [ ] 4.7 创建文字编辑页面 UI：输入框 + 字体/颜色选择 + 预览画面

## 5. GIF 导出

- [ ] 5.1 实现纯 Kotlin GIF 编码器（LZW 压缩 + GIF89a 格式，8-10fps，480p）
- [ ] 5.2 实现视频帧提取（MediaMetadataRetriever，按帧间隔提取 Bitmap）
- [ ] 5.3 实现文字叠加到每帧（Android Canvas 在 Bitmap 上绘制文字）
- [ ] 5.4 实现 GIF 文件写入和保存到 MediaStore（系统相册）
- [ ] 5.5 创建导出进度页面 UI（进度条 + 百分比 + 取消按钮）
- [ ] 5.6 处理导出异常（存储空间不足、处理失败等错误提示）

## 6. 导航与流程串联

- [ ] 6.1 实现 Compose Navigation 路由（4步向导页面 + 进度指示器）
- [ ] 6.2 创建 SharedViewModel 管理跨页面状态传递（视频URI、裁剪时间、文字配置）
- [ ] 6.3 实现页面间数据传递和返回拦截（导出中返回确认）
- [ ] 6.4 创建 MainActivity 和根导航宿主

---

> **前置要求**：先生成 Demo，格式为 HTML（单文件，可直接在浏览器中运行，模拟核心交互流程）。用户确认满意后，再开始生成 Android APP。
