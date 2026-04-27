import { CameraOutlined, LoadingOutlined, CheckCircleFilled } from '@ant-design/icons';
import type { UploadFile } from 'antd';
import { Button, Checkbox, Modal, Space, Tag, Typography, message, theme } from 'antd';
import { Sparkles } from 'lucide-react';
import React, { useCallback, useMemo, useState } from 'react';
import { parseProductTags, type ImageParseTypes } from '@/api/imageParse';
import ImageUploadList from '@/components/Upload/ImageList';
import { useThemeContext } from '@/contexts/ThemeContext';
import type { ProductTagSelectGroup } from './productTagUtils';

const { Text } = Typography;

/**
 * AI 标签识别模态框属性。
 */
export interface AiTagRecognitionModalProps {
  /** 是否显示入口按钮（外部控制） */
  open: boolean;
  /** 关闭回调 */
  onClose: () => void;
  /** 标签分组数据 */
  tagGroups: ProductTagSelectGroup[];
  /** 当前已选中的标签 ID 列表 */
  currentTagIds: string[];
  /** 确认后回调，传入最终合并后的标签 ID 列表 */
  onConfirm: (tagIds: string[]) => void;
}

/**
 * 识别状态枚举。
 */
type RecognitionStep = 'upload' | 'recognizing' | 'result';

/**
 * AI 图片识别标签模态框组件。
 * 支持上传商品图片，AI 自动识别匹配标签，用户可选确认。
 */
const AiTagRecognitionModal: React.FC<AiTagRecognitionModalProps> = ({
  open,
  onClose,
  tagGroups,
  currentTagIds,
  onConfirm,
}) => {
  const { token } = theme.useToken();
  const { isDark } = useThemeContext();
  const [messageApi, contextHolder] = message.useMessage();

  const [step, setStep] = useState<RecognitionStep>('upload');
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [loading, setLoading] = useState(false);
  const [aiMatchedTagIds, setAiMatchedTagIds] = useState<string[]>([]);
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>([]);
  const [reasoning, setReasoning] = useState<string | null>(null);
  const [confidence, setConfidence] = useState<string | null>(null);

  /** 扁平化所有标签，用于按 ID 查找标签信息。 */
  const allTagsMap = useMemo(() => {
    const map = new Map<string, { id: string; name: string; typeName: string }>();
    tagGroups.forEach((group) => {
      group.options.forEach((opt) => {
        map.set(String(opt.value), {
          id: String(opt.value),
          name: opt.label,
          typeName: group.typeName,
        });
      });
    });
    return map;
  }, [tagGroups]);

  /** 已选中标签的 Set（用于快速查找）。 */
  const currentTagIdSet = useMemo(() => new Set(currentTagIds.map(String)), [currentTagIds]);

  /** 重置状态。 */
  const resetState = useCallback(() => {
    setStep('upload');
    setFileList([]);
    setLoading(false);
    setAiMatchedTagIds([]);
    setSelectedTagIds([]);
    setReasoning(null);
    setConfidence(null);
  }, []);

  /** 关闭模态框并重置。 */
  const handleClose = useCallback(() => {
    resetState();
    onClose();
  }, [onClose, resetState]);

  /** 构造发送给 AI 的标签分组数据。 */
  const buildTagGroupsForApi = useCallback((): ImageParseTypes.TagGroup[] => {
    return tagGroups.map((group) => ({
      typeName: group.typeName,
      tags: group.options.map((opt) => ({
        id: String(opt.value),
        name: opt.label,
      })),
    }));
  }, [tagGroups]);

  /** 开始 AI 识别。 */
  const handleStartRecognition = useCallback(async () => {
    const imageUrls = fileList
      .map((file) => file.url || (file.response as any)?.fileUrl)
      .filter(Boolean);

    if (imageUrls.length === 0) {
      messageApi.warning('请先上传药品图片');
      return;
    }

    setStep('recognizing');
    setLoading(true);

    try {
      const result = await parseProductTags({
        image_urls: imageUrls,
        tag_groups: buildTagGroupsForApi(),
      });

      if (result && result.matchedTagIds && result.matchedTagIds.length > 0) {
        // 过滤出确实存在于标签库中的 ID
        const validMatchedIds = result.matchedTagIds.filter((id) => allTagsMap.has(String(id)));

        if (validMatchedIds.length > 0) {
          setAiMatchedTagIds(validMatchedIds.map(String));
          // 默认全部选中（排除已选中的不显示，但默认勾选）
          setSelectedTagIds(validMatchedIds.map(String));
          setReasoning(result.reasoning || null);
          setConfidence(result.confidence || null);
          setStep('result');
          messageApi.success(`AI 识别完成，匹配到 ${validMatchedIds.length} 个标签`);
        } else {
          messageApi.warning('未能识别出匹配的标签，请尝试上传更清晰的图片');
          setStep('upload');
        }
      } else {
        messageApi.warning('未能识别出匹配的标签，请尝试上传更清晰的图片');
        setStep('upload');
      }
    } catch (error) {
      console.error('AI标签识别失败:', error);
      messageApi.error('识别服务暂时不可用，请稍后再试');
      setStep('upload');
    } finally {
      setLoading(false);
    }
  }, [fileList, messageApi, buildTagGroupsForApi, allTagsMap]);

  /** 切换单个标签的选中状态。 */
  const toggleTag = useCallback((tagId: string) => {
    setSelectedTagIds((prev) =>
      prev.includes(tagId) ? prev.filter((id) => id !== tagId) : [...prev, tagId],
    );
  }, []);

  /** 全选 / 取消全选。 */
  const toggleSelectAll = useCallback(
    (checked: boolean) => {
      if (checked) {
        setSelectedTagIds([...aiMatchedTagIds]);
      } else {
        setSelectedTagIds([]);
      }
    },
    [aiMatchedTagIds],
  );

  /** 确认选择，合并到现有标签。 */
  const handleConfirm = useCallback(() => {
    if (selectedTagIds.length === 0) {
      messageApi.warning('请至少选择一个标签');
      return;
    }

    // 合并：已有标签 + 新选择的标签（去重）
    const mergedTagIds = [...new Set([...currentTagIds.map(String), ...selectedTagIds])];
    onConfirm(mergedTagIds);
    messageApi.success(
      `已添加 ${selectedTagIds.filter((id) => !currentTagIdSet.has(id)).length} 个新标签`,
    );
    handleClose();
  }, [selectedTagIds, currentTagIds, onConfirm, messageApi, currentTagIdSet, handleClose]);

  /** 按标签类型分组展示 AI 匹配的标签。 */
  const groupedMatchedTags = useMemo(() => {
    const groups: Array<{
      typeName: string;
      tags: Array<{ id: string; name: string; isAlreadySelected: boolean }>;
    }> = [];

    const typeGroupMap = new Map<string, (typeof groups)[number]>();

    aiMatchedTagIds.forEach((tagId) => {
      const tagInfo = allTagsMap.get(tagId);
      if (!tagInfo) return;

      let group = typeGroupMap.get(tagInfo.typeName);
      if (!group) {
        group = { typeName: tagInfo.typeName, tags: [] };
        typeGroupMap.set(tagInfo.typeName, group);
        groups.push(group);
      }
      group.tags.push({
        id: tagId,
        name: tagInfo.name,
        isAlreadySelected: currentTagIdSet.has(tagId),
      });
    });

    return groups;
  }, [aiMatchedTagIds, allTagsMap, currentTagIdSet]);

  /** 新标签数量（排除已选中的）。 */
  const newTagCount = useMemo(
    () => selectedTagIds.filter((id) => !currentTagIdSet.has(id)).length,
    [selectedTagIds, currentTagIdSet],
  );

  /** 渲染上传步骤。 */
  const renderUploadStep = () => (
    <div>
      <div
        style={{
          padding: '12px 16px',
          background: isDark ? 'rgba(22, 119, 255, 0.14)' : token.colorPrimaryBg,
          border: `1px solid ${isDark ? 'rgba(64, 150, 255, 0.45)' : token.colorPrimaryBorder}`,
          borderRadius: '6px',
          marginBottom: '16px',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            fontSize: '14px',
            color: isDark ? token.colorPrimaryText : token.colorPrimaryTextActive,
            fontWeight: 600,
          }}
        >
          <Sparkles
            color={isDark ? token.colorPrimaryText : token.colorPrimaryTextActive}
            size={18}
          />
          上传药品包装图片，AI 将自动识别并推荐匹配的商品标签
        </div>
      </div>
      <ImageUploadList
        value={fileList}
        onChange={setFileList}
        maxCount={5}
        tip="支持上传最多5张药品图片（正面、背面、侧面等），图片越多识别越准确"
        allowedTypes={['image/png', 'image/jpeg']}
        maxSizeMB={5}
      />
    </div>
  );

  /** 渲染识别中状态。 */
  const renderRecognizingStep = () => (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '60px 0',
        gap: '16px',
      }}
    >
      <LoadingOutlined style={{ fontSize: 40, color: token.colorPrimary }} />
      <Text style={{ fontSize: 16, color: token.colorTextSecondary }}>
        AI 正在分析图片并匹配标签，请稍候...
      </Text>
      <Text type="secondary" style={{ fontSize: 13 }}>
        识别过程可能需要 10-30 秒
      </Text>
    </div>
  );

  /** 渲染识别结果步骤。 */
  const renderResultStep = () => (
    <div>
      {/* 置信度与理由 */}
      {(confidence || reasoning) && (
        <div
          style={{
            padding: '10px 16px',
            background: isDark ? 'rgba(255, 255, 255, 0.04)' : token.colorFillAlter,
            borderRadius: 6,
            marginBottom: 16,
            border: `1px solid ${isDark ? '#434343' : token.colorBorderSecondary}`,
          }}
        >
          {confidence && (
            <Text type="secondary" style={{ fontSize: 13 }}>
              匹配置信度：
              <Tag
                color={
                  confidence === 'high' ? 'success' : confidence === 'medium' ? 'warning' : 'error'
                }
                style={{ marginLeft: 4 }}
              >
                {confidence === 'high' ? '高' : confidence === 'medium' ? '中' : '低'}
              </Tag>
            </Text>
          )}
          {reasoning && (
            <div style={{ marginTop: confidence ? 8 : 0 }}>
              <Text type="secondary" style={{ fontSize: 13 }}>
                识别理由：{reasoning}
              </Text>
            </div>
          )}
        </div>
      )}

      {/* 全选控制栏 */}
      <div
        style={{
          marginBottom: 16,
          padding: '10px 16px',
          background: isDark ? 'rgba(255, 255, 255, 0.04)' : token.colorFillAlter,
          borderRadius: 6,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          border: `1px solid ${isDark ? '#434343' : token.colorBorderSecondary}`,
        }}
      >
        <Checkbox
          checked={selectedTagIds.length === aiMatchedTagIds.length && aiMatchedTagIds.length > 0}
          indeterminate={
            selectedTagIds.length > 0 && selectedTagIds.length < aiMatchedTagIds.length
          }
          onChange={(e) => toggleSelectAll(e.target.checked)}
        >
          <span style={{ fontWeight: 600 }}>全选推荐标签</span>
        </Checkbox>
        <span style={{ color: token.colorTextSecondary, fontSize: 13 }}>
          已选 <strong style={{ color: token.colorPrimary }}>{selectedTagIds.length}</strong> /{' '}
          {aiMatchedTagIds.length} 项
          {newTagCount > 0 && (
            <span style={{ marginLeft: 8 }}>
              （其中 <strong style={{ color: token.colorSuccess }}>{newTagCount}</strong>{' '}
              个为新标签）
            </span>
          )}
        </span>
      </div>

      {/* 分组展示匹配标签 */}
      <div
        style={{
          maxHeight: '400px',
          overflowY: 'auto',
          paddingRight: '4px',
          display: 'flex',
          flexDirection: 'column',
          gap: '20px',
        }}
      >
        {groupedMatchedTags.map((group) => (
          <div key={group.typeName}>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                marginBottom: '10px',
              }}
            >
              <div
                style={{
                  width: '4px',
                  height: '14px',
                  backgroundColor: token.colorPrimary,
                  borderRadius: '2px',
                }}
              />
              <Text strong style={{ fontSize: '14px', color: token.colorTextHeading }}>
                {group.typeName}
              </Text>
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
              {group.tags.map((tag) => {
                const isSelected = selectedTagIds.includes(tag.id);
                const isAlreadyBound = tag.isAlreadySelected;

                return (
                  <Tag
                    key={tag.id}
                    style={{
                      cursor: isAlreadyBound ? 'not-allowed' : 'pointer',
                      padding: '4px 12px',
                      fontSize: 13,
                      borderRadius: 16,
                      opacity: isAlreadyBound ? 0.6 : 1,
                      backgroundColor: isAlreadyBound
                        ? isDark
                          ? 'rgba(255, 255, 255, 0.08)'
                          : '#f5f5f5'
                        : isSelected
                          ? isDark
                            ? 'rgba(22, 119, 255, 0.2)'
                            : token.colorPrimaryBg
                          : 'transparent',
                      borderColor: isAlreadyBound
                        ? token.colorBorderSecondary
                        : isSelected
                          ? token.colorPrimary
                          : token.colorBorder,
                      color: isAlreadyBound
                        ? token.colorTextDisabled
                        : isSelected
                          ? token.colorPrimary
                          : token.colorText,
                    }}
                    onClick={() => {
                      if (!isAlreadyBound) {
                        toggleTag(tag.id);
                      }
                    }}
                  >
                    {isAlreadyBound && (
                      <CheckCircleFilled
                        style={{ marginRight: 4, color: token.colorTextDisabled }}
                      />
                    )}
                    {isSelected && !isAlreadyBound && (
                      <CheckCircleFilled style={{ marginRight: 4, color: token.colorPrimary }} />
                    )}
                    {tag.name}
                    {isAlreadyBound && (
                      <span style={{ marginLeft: 4, fontSize: 11 }}>（已有）</span>
                    )}
                  </Tag>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );

  return (
    <>
      {contextHolder}
      <Modal
        title={
          <Space>
            <Sparkles size={18} color={token.colorPrimary} />
            {step === 'upload'
              ? 'AI 图片识别标签'
              : step === 'recognizing'
                ? '识别中...'
                : 'AI 推荐标签确认'}
          </Space>
        }
        open={open}
        onCancel={handleClose}
        width={640}
        destroyOnClose
        footer={
          step === 'upload'
            ? [
                <Button key="cancel" onClick={handleClose}>
                  取消
                </Button>,
                <Button
                  key="recognize"
                  type="primary"
                  icon={<CameraOutlined />}
                  loading={loading}
                  disabled={fileList.filter((f) => f.status === 'done').length === 0}
                  onClick={handleStartRecognition}
                >
                  开始识别
                </Button>,
              ]
            : step === 'recognizing'
              ? null
              : [
                  <Button
                    key="retry"
                    onClick={() => {
                      setStep('upload');
                      setAiMatchedTagIds([]);
                      setSelectedTagIds([]);
                    }}
                  >
                    重新识别
                  </Button>,
                  <Button key="cancel" onClick={handleClose}>
                    取消
                  </Button>,
                  <Button
                    key="confirm"
                    type="primary"
                    disabled={selectedTagIds.length === 0}
                    onClick={handleConfirm}
                  >
                    确认添加
                    {selectedTagIds.filter((id) => !currentTagIdSet.has(id)).length > 0
                      ? ` (${selectedTagIds.filter((id) => !currentTagIdSet.has(id)).length} 个新标签)`
                      : ''}
                  </Button>,
                ]
        }
      >
        {step === 'upload' && renderUploadStep()}
        {step === 'recognizing' && renderRecognizingStep()}
        {step === 'result' && renderResultStep()}
      </Modal>
    </>
  );
};

AiTagRecognitionModal.displayName = 'AiTagRecognitionModal';

export default AiTagRecognitionModal;
