import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { InboxOutlined, CheckCircleFilled } from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Button,
  message,
  Radio,
  Steps,
  Typography,
  Upload,
  Slider,
  InputNumber,
  Row,
  Col,
} from 'antd';
import type { UploadFile } from 'antd';
import React, { useCallback, useMemo, useState } from 'react';

import { importDocuments } from '@/api/llm-manage/knowledgeBase';
import type { KbDocumentTypes } from '@/api/llm-manage/knowledgeBase';
import PermissionButton from '@/components/PermissionButton';
import {
  buildAcceptFromTypes,
  buildBeforeUpload,
  createServiceUploader,
  mapResponseToFileList,
  type UploadChangeParam,
} from '@/components/Upload/utils';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { buildKnowledgeBaseDocumentPath } from '@/router/paths';
import type { CustomUploadRequestOption, RcFile } from '@/components/Upload/utils';
import styles from './index.module.less';

const { Title, Text, Paragraph } = Typography;

// 支持的文件类型
const ALLOWED_TYPES = [
  '.pdf',
  '.doc',
  '.docx',
  '.ppt',
  '.pptx',
  '.txt',
  '.md',
  '.xlsx',
  '.xls',
  '.csv',
];
const MAX_SIZE_MB = 50;

type ChunkMode = KbDocumentTypes.DocumentImportRequest['chunkMode'];

// 预设切片模式配置
const CHUNK_MODE_OPTIONS: {
  value: ChunkMode;
  label: string;
  description: string;
}[] = [
  {
    value: 'balancedMode',
    label: '平衡模式',
    description: '切片大小 1000，重叠 200，适合大多数场景，能很好地平衡上下文完整性与检索精度。',
  },
  {
    value: 'precisionMode',
    label: '精准模式',
    description: '切片大小 512，重叠 100，切片颗粒度更细，适合对精确度要求极高的短文本问答。',
  },
  {
    value: 'contextMode',
    label: '上下文模式',
    description: '切片大小 2048，重叠 400，保留大段完整上下文，适合阅读理解及长篇文本总结。',
  },
  {
    value: 'custom',
    label: '自定义模式',
    description: '根据具体业务需求，自由滑动调整切片大小和重叠字符数。',
  },
];

const DocumentImport: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const knowledgeBaseId = Number(searchParams.get('id'));
  const knowledgeBaseName = searchParams.get('name') || '知识库';

  const [currentStep, setCurrentStep] = useState(0);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [chunkMode, setChunkMode] = useState<ChunkMode>('balancedMode');
  const [customChunkSize, setCustomChunkSize] = useState<number>(1000);
  const [customChunkOverlap, setCustomChunkOverlap] = useState<number>(200);
  const [submitting, setSubmitting] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();

  // 构建返回文档列表页的 URL
  const documentPageUrl = buildKnowledgeBaseDocumentPath({
    knowledgeBaseId,
    knowledgeBaseName,
  });

  // 上传配置
  const beforeUpload = useMemo(
    () => buildBeforeUpload({ allowedTypes: ALLOWED_TYPES, maxSizeMB: MAX_SIZE_MB }),
    [],
  );
  const accept = useMemo(() => buildAcceptFromTypes(ALLOWED_TYPES), []);
  const customRequest = useMemo(() => {
    const uploader = createServiceUploader();
    return (options: CustomUploadRequestOption) =>
      uploader({ ...options, file: options.file as RcFile });
  }, []);

  const handleUploadChange = useCallback((info: UploadChangeParam) => {
    setFileList(mapResponseToFileList(info.fileList));
  }, []);

  // 获取上传成功的文件列表（有 URL 的）
  const uploadedFiles = useMemo(() => {
    return fileList.filter((f) => f.status === 'done' && (f.url || f.response?.fileUrl));
  }, [fileList]);

  // 下一步
  const handleNext = useCallback(() => {
    if (uploadedFiles.length === 0) {
      messageApi.warning('请至少上传一个文件');
      return;
    }
    setCurrentStep(1);
  }, [uploadedFiles, messageApi]);

  // 上一步
  const handlePrev = useCallback(() => {
    setCurrentStep(0);
  }, []);

  // 完成导入
  const handleSubmit = useCallback(async () => {
    const fileDetails: KbDocumentTypes.FileDetail[] = uploadedFiles.map((f) => {
      const url = f.url || f.response?.fileUrl || '';
      const name = f.name || f.response?.fileName || '';
      return { fileName: name, fileUrl: url };
    });

    if (fileDetails.length === 0) {
      messageApi.warning('没有可导入的文件');
      return;
    }

    const data: KbDocumentTypes.DocumentImportRequest = {
      knowledgeBaseId,
      fileDetails,
      chunkMode,
    };

    if (chunkMode === 'custom') {
      if (customChunkSize < 100 || customChunkSize > 6000) {
        messageApi.warning('自定义切片大小必须在 100 到 6000 之间');
        return;
      }
      if (customChunkOverlap < 0 || customChunkOverlap > 1000) {
        messageApi.warning('自定义切片重叠大小必须在 0 到 1000 之间');
        return;
      }
      data.customChunkMode = {
        chunkSize: customChunkSize,
        chunkOverlap: customChunkOverlap,
      };
    }

    setSubmitting(true);
    try {
      await importDocuments(data);
      messageApi.success('导入任务已提交，文档处理中');
      navigate(documentPageUrl);
    } finally {
      setSubmitting(false);
    }
  }, [
    uploadedFiles,
    knowledgeBaseId,
    chunkMode,
    customChunkSize,
    customChunkOverlap,
    messageApi,
    navigate,
    documentPageUrl,
  ]);

  return (
    <PageContainer
      title="导入知识"
      onBack={() => navigate(documentPageUrl)}
      breadcrumb={{
        items: [{ title: '大模型管理' }, { title: '导入知识' }],
      }}
      className={styles.pageContainer}
    >
      {contextHolder}

      <div className={styles.container}>
        <div className={styles.stepsWrapper}>
          <Steps
            current={currentStep}
            items={[
              { title: '上传文件', icon: currentStep > 0 ? <CheckCircleFilled /> : undefined },
              { title: '切片配置' },
            ]}
          />
        </div>

        <div className={styles.contentWrapper}>
          {/* 步骤一：上传文件 */}
          <div style={{ display: currentStep === 0 ? 'block' : 'none' }}>
            <Title level={4} style={{ marginBottom: 24, fontWeight: 600 }}>
              上传知识文档
            </Title>
            <div className={styles.uploadArea}>
              <Upload.Dragger
                accept={accept}
                multiple
                beforeUpload={beforeUpload}
                customRequest={customRequest}
                onChange={handleUploadChange}
                fileList={fileList}
              >
                <div className="ant-upload-drag-icon">
                  <InboxOutlined />
                </div>
                <div className="ant-upload-text">点击或拖拽文件到此区域上传</div>
                <div className="ant-upload-hint">
                  支持 PDF、DOC、DOCX、PPT、PPTX、TXT、MD、XLSX、XLS、CSV 格式，单文件不超过 50MB
                </div>
              </Upload.Dragger>
            </div>

            <div className={styles.actionFooter}>
              <Button size="large" onClick={() => navigate(documentPageUrl)}>
                取消
              </Button>
              <Button
                size="large"
                type="primary"
                onClick={handleNext}
                disabled={uploadedFiles.length === 0}
              >
                下一步
              </Button>
            </div>
          </div>

          {/* 步骤二：切片配置 */}
          <div style={{ display: currentStep === 1 ? 'block' : 'none' }}>
            <Title level={4} style={{ marginBottom: 24, fontWeight: 600 }}>
              文本切片规则配置
            </Title>

            <Paragraph type="secondary" style={{ marginBottom: 24 }}>
              文本切片是将长文档分割成小段的过程，合理的切片模式能显著提升大模型检索和回答的准确率。
            </Paragraph>

            <Radio.Group
              value={chunkMode}
              onChange={(e) => setChunkMode(e.target.value)}
              style={{ width: '100%' }}
            >
              <div className={styles.modeCards}>
                {CHUNK_MODE_OPTIONS.map((opt) => {
                  const isActive = chunkMode === opt.value;
                  return (
                    <div
                      key={opt.value}
                      className={`${styles.modeCard} ${isActive ? styles.active : ''}`}
                      onClick={() => setChunkMode(opt.value)}
                    >
                      <Radio value={opt.value} className={styles.radio} />
                      <div className={styles.title}>{opt.label}</div>
                      <div className={styles.desc}>{opt.description}</div>
                    </div>
                  );
                })}
              </div>
            </Radio.Group>

            {/* 自定义模式参数 */}
            {chunkMode === 'custom' && (
              <div className={styles.customConfig}>
                <Row gutter={[48, 24]}>
                  <Col span={24}>
                    <div style={{ marginBottom: 8 }}>
                      <Text strong>切片最大长度 (Chunk Size)</Text>
                      <Text
                        type="secondary"
                        style={{ display: 'block', fontSize: 13, marginTop: 4 }}
                      >
                        每个文本切片包含的最大字符数，建议值在 500 - 2000 之间。
                      </Text>
                    </div>
                    <div className={styles.sliderRow}>
                      <Slider
                        min={100}
                        max={6000}
                        value={customChunkSize}
                        onChange={setCustomChunkSize}
                        className={styles.slider}
                        tooltip={{ formatter: (v) => `${v} 字符` }}
                      />
                      <InputNumber
                        autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                        min={100}
                        max={6000}
                        value={customChunkSize}
                        onChange={(v) => setCustomChunkSize(v ?? 1000)}
                        className={styles.input}
                      />
                    </div>
                  </Col>

                  <Col span={24}>
                    <div style={{ marginBottom: 8 }}>
                      <Text strong>切片重叠长度 (Chunk Overlap)</Text>
                      <Text
                        type="secondary"
                        style={{ display: 'block', fontSize: 13, marginTop: 4 }}
                      >
                        相邻切片之间重叠的字符数，用于保持上下文的连贯性，建议设置为切片大小的 10% -
                        20%。
                      </Text>
                    </div>
                    <div className={styles.sliderRow}>
                      <Slider
                        min={0}
                        max={1000}
                        value={customChunkOverlap}
                        onChange={setCustomChunkOverlap}
                        className={styles.slider}
                        tooltip={{ formatter: (v) => `${v} 字符` }}
                      />
                      <InputNumber
                        autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                        min={0}
                        max={1000}
                        value={customChunkOverlap}
                        onChange={(v) => setCustomChunkOverlap(v ?? 200)}
                        className={styles.input}
                      />
                    </div>
                  </Col>
                </Row>
              </div>
            )}

            <div className={styles.actionFooter}>
              <Button size="large" onClick={handlePrev}>
                上一步
              </Button>
              <PermissionButton
                size="large"
                type="primary"
                loading={submitting}
                access={ADMIN_PERMISSIONS.kbDocument.import}
                onClick={handleSubmit}
              >
                完成并导入
              </PermissionButton>
            </div>
          </div>
        </div>
      </div>
    </PageContainer>
  );
};

export default DocumentImport;
