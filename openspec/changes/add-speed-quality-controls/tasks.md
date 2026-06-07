## 1. State 扩展

- [x] 1.1 在 state 对象中新增 `speed`、`qualityLevel`、`maxFrames` 字段，设置默认值
- [x] 1.2 定义画质档位映射常量 `QUALITY_MAP`（档位 → {width, gifQuality}）

## 2. Step 0 视频时长限制

- [x] 2.1 视频选取 onchange 中，videoDuration > 30s 时弹出 toast 提示并阻止进入下一步
- [x] 2.2 超时视频不设置 state.videoFile，保持按钮 disabled

## 3. Step 1 倍速选择器 + 选段上限调整

- [x] 3.1 裁剪界面新增倍速选择器 UI（5 个按钮：1/4、1/2、原速、1.5、2），默认选中"原速"
- [x] 3.2 倍速按钮点击时更新 `state.speed`，联动更新预估信息显示
- [x] 3.3 选段上限从 `Math.min(3, videoDuration)` 改为 `Math.min(10, videoDuration)`
- [x] 3.4 在时间标签下方新增预估信息行：显示"预计 XX 帧 / 约 XXX KB"，超出 60 帧时红色标注

## 4. Step 3 画质选择器

- [x] 4.1 导出预览界面新增画质选择器 UI（3 个按钮：流畅/标准/高清），默认选中"标准"
- [x] 4.2 切换画质时更新 state.qualityLevel，联动更新预估文件大小
- [x] 4.3 画质选择器同时出现在视频模式和照片模式

## 5. 导出逻辑改写

- [x] 5.1 `exportGif()` 中 fps 保持 8，帧间隔公式改为 `t = trimStart + i / (fps * speed)`
- [x] 5.2 总帧数改为 `min(ceil((trimEnd - trimStart) * fps * speed), 60)`
- [x] 5.3 输出宽度和 quality 从 `QUALITY_MAP[state.qualityLevel]` 读取，替换硬编码的 480 和 10
- [x] 5.4 `exportImage()` 中输出宽度同样从 `QUALITY_MAP[state.qualityLevel]` 读取

## 6. 验证测试

- [ ] 6.1 选取 31 秒视频 → 提示"视频过长"
- [ ] 6.2 选取 10 秒视频 → 倍速 1/4x → 60 帧截断 → 红色提示显示
- [ ] 6.3 3 档画质分别导出 → 验证分辨率和文件大小符合规格
- [ ] 6.4 5 档倍速分别导出 → 验证实际 GIF 时长符合预期
