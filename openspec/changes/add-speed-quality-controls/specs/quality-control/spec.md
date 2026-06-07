## ADDED Requirements

### Requirement: 画质档位选择
系统 SHALL 在导出预览界面提供三档画质选择：流畅、标准、高清。

#### Scenario: 默认画质
- **WHEN** 用户首次进入导出预览界面
- **THEN** 画质默认为"标准"

#### Scenario: 切换画质档位
- **WHEN** 用户点击画质选项（流畅 / 标准 / 高清）
- **THEN** 系统更新画质档位
- **AND** 预览区更新预估文件大小

### Requirement: 画质参数映射
系统 SHALL 根据画质档位设定输出分辨率与编码质量。

#### Scenario: 流畅档参数
- **WHEN** 用户选择"流畅"画质
- **THEN** 输出宽度为 360px
- **AND** GIF 编码 quality 参数为 20

#### Scenario: 标准档参数
- **WHEN** 用户选择"标准"画质
- **THEN** 输出宽度为 480px
- **AND** GIF 编码 quality 参数为 10

#### Scenario: 高清档参数
- **WHEN** 用户选择"高清"画质
- **THEN** 输出宽度为 640px
- **AND** GIF 编码 quality 参数为 3

### Requirement: 画质适用范围
系统 SHALL 将画质档位同时应用于视频 GIF 导出和照片 PNG 导出。

#### Scenario: 视频 GIF 导出
- **WHEN** 用户在视频模式下导出 GIF
- **THEN** GIF 的分辨率和编码质量按所选画质档位设定

#### Scenario: 照片 PNG 导出
- **WHEN** 用户在照片模式下导出图片
- **THEN** PNG 的分辨率按所选画质档位设定
