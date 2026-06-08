## 1. State 扩展

- [x] 1.1 `state.mediaType` 新增 `'burst'` 可选值
- [x] 1.2 state 新增 `photos[]`、`photoUrls[]`、`frameDelay`（默认 500ms）、`photoTexts[]`、`currentPhotoIdx`

## 2. Step 0 三入口

- [x] 2.1 素材选取 UI 从双入口改为三入口：选取视频 / 选取照片 / 连拍GIF
- [x] 2.2 "连拍GIF"点击 → mediaType='burst' → 打开 `<input multiple accept="image/*">`
- [x] 2.3 多选照片 onchange：限制 20 张，生成 Blob URL，进入 Step 1

## 3. Step 1 帧排序界面

- [x] 3.1 缩略图网格布局（flex-wrap），每张带 × 删除按钮
- [x] 3.2 HTML5 Drag & Drop 实现拖拽排序
- [x] 3.3 "+ 添加更多"按钮 → 追加选取，总数不超 20
- [x] 3.4 帧间隔滑块（500-1500ms，默认 500ms）+ 预估行

## 4. Step 2 逐张配文

- [x] 4.1 照片导航器：`← 第 N/M 张 →` 切换编辑目标
- [x] 4.2 `photoTexts[]` 存储每张照片的文字设置（content/font/weight/color/x/y/size）
- [x] 4.3 `getPhotoText(idx)` 继承规则：未配文→向前查找→无则 null
- [x] 4.4 文字/字体/颜色/字号/拖拽 编辑即时保存到 `photoTexts[currentIdx]`

## 5. Step 3 轮播预览

- [x] 5.1 进入即自动 `setInterval` 循环切换照片，间隔 = frameDelay
- [x] 5.2 每张照片联动显示各自的配文（继承规则）
- [x] 5.3 离开界面时 `clearInterval` 清理定时器
- [x] 5.4 画质选择器隐藏（跟照片模式一致）

## 6. 连拍导出

- [x] 6.1 `exportBurstGif()`：遍历 photos → Image → drawImage → per-photo text → gif.addFrame
- [x] 6.2 delay = frameDelay / 10，画质固定 standard（480px/gifQuality=10）
- [x] 6.3 文字渲染使用各自 `photoTexts[i]` 的 font/size/color/position
