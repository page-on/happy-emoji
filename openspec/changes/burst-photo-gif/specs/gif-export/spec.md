## MODIFIED Requirements

### Requirement: 视频+文字合成GIF
系统 SHALL 将素材与用户添加的文字合成为 GIF 动图或 PNG 图片，根据 mediaType 选择合成路径。

#### Scenario: 视频模式导出
- **WHEN** mediaType 为 video 且用户点击导出
- **THEN** 系统按倍速/画质参数从视频提取帧，合成 GIF

#### Scenario: 照片模式导出
- **WHEN** mediaType 为 photo 且用户点击导出
- **THEN** 系统将单张照片与文字合成 PNG 图片

#### Scenario: 连拍模式导出
- **WHEN** mediaType 为 burst 且用户点击导出
- **THEN** 系统将照片数组中每张图作为一帧，按帧间隔合成 GIF
- **AND** 每帧使用各自 photoTexts[i] 的文字设置进行渲染
- **AND** 画质固定为标准 480px/gifQuality=10

#### Scenario: 连拍模式预览
- **WHEN** 用户进入连拍模式的预览导出界面
- **THEN** 系统自动开始轮播照片（间隔 = frameDelay）
- **AND** 每张照片的配文联动切换
- **AND** 离开界面时自动停止轮播

#### Scenario: 连拍帧延迟
- **WHEN** 连拍模式导出 GIF
- **THEN** gif.js delay 参数 = state.frameDelay / 10
- **AND** 所有帧使用相同延迟值

#### Scenario: 导出失败
- **WHEN** 合成过程中发生错误
- **THEN** 系统显示错误提示并允许重试
