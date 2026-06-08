## MODIFIED Requirements

### Requirement: 从系统相册选取视频
系统 SHALL 允许用户从素材选取界面选择视频、单张照片或多张照片作为表情包素材。

#### Scenario: 素材选取入口
- **WHEN** 用户进入素材选取界面
- **THEN** 系统展示三个入口按钮：选取视频、选取照片、连拍GIF

#### Scenario: 连拍GIF入口
- **WHEN** 用户点击"连拍GIF"入口
- **THEN** state.mediaType 设为 burst
- **AND** 系统打开多选文件选择器（仅图片文件）
