## MODIFIED Requirements

### Requirement: 图片输出规格

系统 SHALL 按以下规格输出图片文件。

#### Scenario: PNG格式与参数
- **WHEN** 系统合成图片
- **THEN** 输出格式为 PNG
- **AND** 输出分辨率宽度按画质档位设定（流畅 360px / 标准 480px / 高清 640px），等比缩放
- **AND** 文字按照用户设定的位置、字体、颜色渲染在图片上
