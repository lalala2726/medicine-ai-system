import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { PageContainer } from '@ant-design/pro-components';
import XMarkdown from '@ant-design/x-markdown';
import { Button, Card, Input, Space, Switch, Tag, Typography } from 'antd';
import React, { useState } from 'react';
import { RichTextEditor } from '@/components';
import { useThemeContext } from '@/contexts/ThemeContext';
import styles from './index.module.less';
import '@ant-design/x-markdown/themes/light.css';
import '@ant-design/x-markdown/themes/dark.css';

const { Paragraph, Text, Title } = Typography;

/**
 * 示例 Markdown 内容。
 */
const SAMPLE_MARKDOWN_CONTENT = `# Rich Text Editor 调试页

> 这里用于联调富文本编辑器的基础能力、上传能力和暗色模式表现。

## 功能检查

- [x] Markdown 输出
- [x] 图片上传
- [x] 视频上传
- [x] 附件上传
- [x] 暗色模式切换

## 表格示例

| 字段 | 说明 |
| --- | --- |
| contentFormat | 当前默认输出为 Markdown |
| editable | 支持编辑态与只读态切换 |
| theme | 跟随系统暗色模式切换 |

## 代码块示例

\`\`\`ts
const dosage = '每日 2 次';
const medicineName = '调试示例药品';

console.log(\`\${medicineName}：\${dosage}\`);
\`\`\`

## 图片示例

![优惠券图标](/icons/coupon.svg)

## 链接与附件示例

[查看 OpenAI 官网](https://openai.com)

[下载附件示例](https://example.com/files/editor-preview.pdf)
`;

/**
 * 空内容预览文案。
 */
const EMPTY_PREVIEW_CONTENT = '暂无内容，左侧输入后这里会实时预览。';

/**
 * 编辑器预览调试页面。
 * @returns 页面节点。
 */
const EditorPreviewPage: React.FC = () => {
  const { isDark } = useThemeContext();
  const [content, setContent] = useState<string>(SAMPLE_MARKDOWN_CONTENT);
  const [editable, setEditable] = useState(true);

  /**
   * Markdown 预览主题类名。
   */
  const markdownThemeClassName = isDark ? 'x-markdown-dark' : 'x-markdown-light';

  return (
    <PageContainer title="编辑器预览">
      <div className={styles.page}>
        <Card className={styles.summaryCard} bordered={false}>
          <div className={styles.summaryRow}>
            <div className={styles.summaryText}>
              <Title level={5} style={{ marginBottom: 0 }}>
                Rich Text Editor 联调工作台
              </Title>
              <Paragraph className={styles.summaryDesc}>
                左侧编辑器默认输出
                Markdown，右侧实时查看渲染结果；上传图片、视频、附件后，也可以直接在这里验证最终效果。
              </Paragraph>
            </div>

            <Space wrap>
              <Tag color="blue">当前格式：Markdown</Tag>
              <Tag color={isDark ? 'processing' : 'default'}>
                当前主题：{isDark ? '暗色' : '亮色'}
              </Tag>
              <Text>只读模式</Text>
              <Switch checked={!editable} onChange={(checked) => setEditable(!checked)} />
              <Button
                onClick={() => {
                  setContent(SAMPLE_MARKDOWN_CONTENT);
                }}
              >
                填充示例
              </Button>
              <Button
                onClick={() => {
                  setContent('');
                }}
              >
                清空内容
              </Button>
            </Space>
          </div>
        </Card>

        <div className={styles.workspace}>
          <Card className={styles.panelCard} title="富文本编辑器" bordered={false}>
            <div className={styles.panelBody}>
              <RichTextEditor
                value={content}
                onChange={setContent}
                editable={editable}
                contentFormat="markdown"
                height="100%"
                placeholder="请输入调试内容"
              />
            </div>
          </Card>

          <Card className={styles.panelCard} title="实时预览" bordered={false}>
            <div className={styles.previewBody}>
              {content ? (
                <XMarkdown
                  className={markdownThemeClassName}
                  content={content}
                  paragraphTag="div"
                  openLinksInNewTab
                />
              ) : (
                <Text className={styles.previewEmpty}>{EMPTY_PREVIEW_CONTENT}</Text>
              )}
            </div>
          </Card>
        </div>

        <Card className={styles.sourceCard} title="Markdown 原文" bordered={false}>
          <Input.TextArea
            autoComplete={TEXT_INPUT_AUTOCOMPLETE}
            className={styles.sourceArea}
            readOnly
            value={content}
            autoSize={{ minRows: 8, maxRows: 18 }}
          />
        </Card>
      </div>
    </PageContainer>
  );
};

export default EditorPreviewPage;
