import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { CameraOutlined, LoadingOutlined, CheckCircleFilled } from '@ant-design/icons';
import type { UploadFile } from 'antd';
import {
  Button,
  Col,
  Form,
  Input,
  Modal,
  Radio,
  Row,
  Select,
  message,
  theme,
  Checkbox,
  Descriptions,
  Space,
} from 'antd';
import { Sparkles } from 'lucide-react';
import React, { useState } from 'react';
import { parseDrug } from '@/api/imageParse';
import ImageUploadList from '@/components/Upload/ImageList';
import { DRUG_CATEGORY_META_LIST, getDrugCategoryMeta } from '@/constants/drugCategory';
import { useThemeContext } from '@/contexts/ThemeContext';

const { TextArea } = Input;

const fieldMap: Record<string, string> = {
  commonName: '药品通用名',
  brand: '品牌',
  packaging: '包装规格',
  productionUnit: '生产单位',
  approvalNumber: '批准文号',
  executiveStandard: '执行标准',
  validityPeriod: '有效期',
  originType: '产地类型',
  drugCategory: '药品分类',
  isOutpatientMedicine: '是否外用药',
  composition: '成分',
  characteristics: '性状',
  storageConditions: '贮藏条件',
  efficacy: '功能主治',
  usageMethod: '用法用量',
  adverseReactions: '不良反应',
  precautions: '注意事项',
  taboo: '禁忌',
  instruction: '药品说明书全文',
  warmTips: '温馨提示',
};

/**
 * 药品分类下拉选项列表。
 */
const DRUG_CATEGORY_OPTIONS = DRUG_CATEGORY_META_LIST.map((item) => ({
  label: `${item.shortLabel} / ${item.name}：${item.description}`,
  value: item.code,
}));

const DrugDetail: React.FC = () => {
  const { token } = theme.useToken();
  const { isDark } = useThemeContext();
  const [loading, setLoading] = useState(false);
  const [uploadModalOpen, setUploadModalOpen] = useState(false);
  const [confirmModalOpen, setConfirmModalOpen] = useState(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [aiResult, setAiResult] = useState<any>(null);
  const [selectedAiFields, setSelectedAiFields] = useState<string[]>([]);
  const form = Form.useFormInstance();
  const [messageApi, contextHolder] = message.useMessage();

  // 打开上传模态框
  const handleAIScanClick = () => {
    setUploadModalOpen(true);
    setFileList([]);
  };

  // 开始识别
  const handleStartRecognition = async () => {
    const imageUrls = fileList
      .map((file) => file.url || (file.response as any)?.fileUrl)
      .filter(Boolean);

    if (imageUrls.length === 0) {
      messageApi.warning('请先上传药品图片');
      return;
    }

    console.log('开始识别，图片URLs:', imageUrls);

    setLoading(true);

    try {
      const result = await parseDrug({ image_urls: imageUrls });
      console.log('AI识别原始结果:', result);

      if (result) {
        // 检查结果是否为空对象，并找出有效的字段映射
        const validKeys = Object.keys(result).filter((key) => {
          const value = result[key as keyof typeof result];
          return fieldMap[key] && value !== null && value !== undefined && value !== '';
        });

        if (validKeys.length > 0) {
          setAiResult(result);
          setSelectedAiFields(validKeys);
          setUploadModalOpen(false);
          setConfirmModalOpen(true);
          messageApi.success('AI识别完成，请确认是否填充');
        } else {
          messageApi.warning('未能识别出有效的药品信息，请尝试上传更清晰的图片');
        }
      } else {
        messageApi.error('未能识别出药品信息，请重试');
      }
    } catch (error) {
      console.error('AI识别失败:', error);
      messageApi.error('识别服务暂时不可用，请稍后再试');
    } finally {
      setLoading(false);
    }
  };

  // 确认填充
  const handleConfirmFill = () => {
    if (aiResult) {
      if (selectedAiFields.length === 0) {
        messageApi.warning('未选择任何需要填充的字段');
        return;
      }

      console.log('AI识别结果:', aiResult);
      console.log('用户选择的填充字段:', selectedAiFields);
      console.log('当前表单drugDetail值:', form.getFieldValue(['drugDetail']));

      // 获取当前的drugDetail值
      const currentDrugDetail = form.getFieldValue(['drugDetail']) || {};

      // 仅合并用户勾选的字段到现有drugDetail中
      const fieldsToFill = selectedAiFields.reduce(
        (acc, key) => {
          acc[key] = aiResult[key];
          return acc;
        },
        {} as Record<string, any>,
      );

      const updatedDrugDetail = {
        ...currentDrugDetail,
        ...fieldsToFill,
      };

      console.log('更新后的drugDetail值:', updatedDrugDetail);

      // 设置更新后的drugDetail
      form.setFieldValue(['drugDetail'], updatedDrugDetail);
      form.validateFields();

      messageApi.success('已自动填充表单');
    }
    setConfirmModalOpen(false);
    setAiResult(null);
  };

  // 渲染确认信息列表
  const renderConfirmList = () => {
    if (!aiResult) return null;

    const data = Object.entries(aiResult)
      .filter(([key, value]) => {
        return fieldMap[key] && value !== null && value !== undefined && value !== '';
      })
      .map(([key, value]) => {
        return [key, value] as [string, any];
      });

    if (data.length === 0) {
      return (
        <div style={{ textAlign: 'center', color: token.colorTextTertiary, padding: '20px' }}>
          未识别到有效信息
        </div>
      );
    }

    return (
      <div style={{ maxHeight: '500px', overflowY: 'auto', paddingRight: '4px' }}>
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
            checked={selectedAiFields.length === data.length && data.length > 0}
            indeterminate={selectedAiFields.length > 0 && selectedAiFields.length < data.length}
            onChange={(e) => {
              if (e.target.checked) {
                setSelectedAiFields(data.map(([key]) => key));
              } else {
                setSelectedAiFields([]);
              }
            }}
          >
            <span style={{ fontWeight: 600 }}>全选提取字段</span>
          </Checkbox>
          <span style={{ color: token.colorTextSecondary, fontSize: 13 }}>
            已选 <strong style={{ color: token.colorPrimary }}>{selectedAiFields.length}</strong> /{' '}
            {data.length} 项
          </span>
        </div>
        <Descriptions
          bordered
          column={1}
          size="small"
          labelStyle={{
            width: 180,
            fontWeight: 500,
            color: token.colorTextSecondary,
            backgroundColor: isDark ? 'transparent' : '#fafafa',
          }}
          contentStyle={{ backgroundColor: isDark ? 'transparent' : '#fff' }}
        >
          {data.map(([key, value]) => (
            <Descriptions.Item
              key={key}
              label={
                <Checkbox
                  checked={selectedAiFields.includes(key)}
                  onChange={(e) => {
                    if (e.target.checked) {
                      setSelectedAiFields((prev) => [...prev, key]);
                    } else {
                      setSelectedAiFields((prev) => prev.filter((k) => k !== key));
                    }
                  }}
                >
                  <div onClick={(e) => e.stopPropagation()}>
                    <Space size={8} style={{ width: '100%' }}>
                      {fieldMap[key]}
                    </Space>
                  </div>
                </Checkbox>
              }
            >
              <span
                style={{
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                  color: token.colorText,
                  lineHeight: 1.6,
                }}
              >
                {key === 'drugCategory'
                  ? (() => {
                      const meta = getDrugCategoryMeta(Number(value));
                      return meta ? `${meta.shortLabel} / ${meta.name}` : String(value);
                    })()
                  : String(value)}
              </span>
            </Descriptions.Item>
          ))}
        </Descriptions>
      </div>
    );
  };

  return (
    <>
      {contextHolder}

      {/* AI 功能入口区 */}
      <div
        style={{
          padding: '12px 16px',
          background: isDark ? 'rgba(22, 119, 255, 0.14)' : token.colorPrimaryBg,
          border: `1px solid ${isDark ? 'rgba(64, 150, 255, 0.45)' : token.colorPrimaryBorder}`,
          borderRadius: '6px',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 12,
          flexWrap: 'wrap',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            fontSize: '15px',
            color: isDark ? token.colorPrimaryText : token.colorPrimaryTextActive,
            fontWeight: 600,
          }}
        >
          <Sparkles
            color={isDark ? token.colorPrimaryText : token.colorPrimaryTextActive}
            size={20}
          />
          智能识别，一键提取药品包装与说明书信息
        </div>

        <Button
          onClick={handleAIScanClick}
          type="primary"
          size="small"
          style={{
            borderRadius: '16px',
            boxShadow: isDark
              ? '0 4px 12px rgba(22, 119, 255, 0.3)'
              : '0 2px 4px rgba(24, 144, 255, 0.2)',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
          }}
          icon={<CameraOutlined />}
        >
          AI 图片识别填充
        </Button>
      </div>

      {/* 上传模态框 */}
      <Modal
        title="上传药品图片"
        open={uploadModalOpen}
        onCancel={() => setUploadModalOpen(false)}
        footer={[
          <Button key="cancel" onClick={() => setUploadModalOpen(false)}>
            取消
          </Button>,
          <Button
            key="submit"
            type="primary"
            loading={loading}
            onClick={handleStartRecognition}
            disabled={fileList.length === 0}
          >
            {loading ? '识别中...' : '开始识别'}
          </Button>,
        ]}
      >
        <div style={{ padding: '16px 0', position: 'relative' }}>
          <p style={{ marginBottom: '16px', color: token.colorTextSecondary, lineHeight: 1.6 }}>
            请上传药品包装盒的正面、背面，或者说明书的清晰图片。AI
            将自动为您提取相关信息。支持最多上传 5 张。
          </p>
          <div style={{ minHeight: 120 }}>
            {loading ? (
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  padding: '40px 0',
                }}
              >
                <div
                  className="ant-spin ant-spin-spinning"
                  style={{ fontSize: 32, marginBottom: 16 }}
                >
                  <LoadingOutlined spin />
                </div>
                <div style={{ color: token.colorPrimary }}>智能识别中，请耐心等待...</div>
              </div>
            ) : (
              <ImageUploadList value={fileList} onChange={setFileList} maxCount={5} />
            )}
          </div>
        </div>
      </Modal>

      {/* 确认模态框 */}
      <Modal
        title={
          <Space>
            <CheckCircleFilled style={{ color: token.colorSuccess, fontSize: 18 }} />
            <span style={{ fontSize: 16, fontWeight: 600 }}>AI 识别完成</span>
          </Space>
        }
        open={confirmModalOpen}
        onCancel={() => setConfirmModalOpen(false)}
        width={800}
        okText="确认填充"
        cancelText="取消"
        onOk={handleConfirmFill}
      >
        <div
          style={{
            marginBottom: '16px',
            padding: '12px 16px',
            backgroundColor: isDark ? 'rgba(250, 173, 20, 0.1)' : '#fffbe6',
            border: `1px solid ${isDark ? 'rgba(250, 173, 20, 0.4)' : '#ffe58f'}`,
            borderRadius: '6px',
            color: token.colorTextSecondary,
            lineHeight: 1.6,
          }}
        >
          AI
          已经为您从图片中提取了以下信息。您可以勾选想要保留的字段，确认填充后，选中的字段内容将覆盖现有表单。
        </div>
        {renderConfirmList()}
      </Modal>

      <Row gutter={16}>
        <Col span={12}>
          <Form.Item
            label="药品通用名"
            name={['drugDetail', 'commonName']}
            rules={[{ max: 100, message: '药品通用名不能超过100个字符' }]}
          >
            <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入药品通用名" />
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item
            label="品牌"
            name={['drugDetail', 'brand']}
            rules={[{ max: 100, message: '品牌名称不能超过100个字符' }]}
          >
            <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入品牌名称" />
          </Form.Item>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={12}>
          <Form.Item
            label="包装规格"
            name={['drugDetail', 'packaging']}
            rules={[{ max: 100, message: '包装规格不能超过100个字符' }]}
          >
            <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="例如: 10mg*24片/盒" />
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item
            label="生产单位"
            name={['drugDetail', 'productionUnit']}
            rules={[{ max: 200, message: '生产单位不能超过200个字符' }]}
          >
            <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入生产单位" />
          </Form.Item>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={12}>
          <Form.Item
            label="批准文号"
            name={['drugDetail', 'approvalNumber']}
            rules={[{ max: 100, message: '批准文号不能超过100个字符' }]}
          >
            <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入批准文号" />
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item
            label="执行标准"
            name={['drugDetail', 'executiveStandard']}
            rules={[{ max: 100, message: '执行标准不能超过100个字符' }]}
          >
            <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入执行标准" />
          </Form.Item>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={12}>
          <Form.Item
            label="有效期"
            name={['drugDetail', 'validityPeriod']}
            rules={[{ max: 50, message: '有效期不能超过50个字符' }]}
          >
            <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="例如: 24个月" />
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item
            label="产地类型"
            name={['drugDetail', 'originType']}
            rules={[{ max: 50, message: '产地类型不能超过50个字符' }]}
          >
            <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="例如: 国产、进口" />
          </Form.Item>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={12}>
          <Form.Item label="药品分类" name={['drugDetail', 'drugCategory']} initialValue={0}>
            <Select options={DRUG_CATEGORY_OPTIONS} placeholder="请选择药品分类" />
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item
            label="是否外用药"
            name={['drugDetail', 'isOutpatientMedicine']}
            initialValue={false}
          >
            <Radio.Group>
              <Radio value={true}>是</Radio>
              <Radio value={false}>否</Radio>
            </Radio.Group>
          </Form.Item>
        </Col>
      </Row>

      <Form.Item
        label="成分"
        name={['drugDetail', 'composition']}
        rules={[{ max: 1000, message: '成分不能超过1000个字符' }]}
      >
        <TextArea rows={3} placeholder="请输入药品成分" showCount maxLength={1000} />
      </Form.Item>

      <Form.Item
        label="性状"
        name={['drugDetail', 'characteristics']}
        rules={[{ max: 500, message: '性状不能超过500个字符' }]}
      >
        <TextArea rows={3} placeholder="请输入药品性状" showCount maxLength={500} />
      </Form.Item>

      <Form.Item
        label="贮藏条件"
        name={['drugDetail', 'storageConditions']}
        rules={[{ max: 200, message: '贮藏条件不能超过200个字符' }]}
      >
        <TextArea rows={2} placeholder="例如: 密封，置阴凉干燥处保存" showCount maxLength={200} />
      </Form.Item>

      <Form.Item
        label="功能主治"
        name={['drugDetail', 'efficacy']}
        rules={[{ max: 1000, message: '功能主治不能超过1000个字符' }]}
      >
        <TextArea rows={4} placeholder="请输入药品的功能主治" showCount maxLength={1000} />
      </Form.Item>

      <Form.Item
        label="用法用量"
        name={['drugDetail', 'usageMethod']}
        rules={[{ max: 1000, message: '用法用量不能超过1000个字符' }]}
      >
        <TextArea rows={4} placeholder="请输入药品的用法用量" showCount maxLength={1000} />
      </Form.Item>

      <Form.Item
        label="不良反应"
        name={['drugDetail', 'adverseReactions']}
        rules={[{ max: 1000, message: '不良反应不能超过1000个字符' }]}
      >
        <TextArea rows={4} placeholder="请输入药品的不良反应" showCount maxLength={1000} />
      </Form.Item>

      <Form.Item
        label="注意事项"
        name={['drugDetail', 'precautions']}
        rules={[{ max: 1000, message: '注意事项不能超过1000个字符' }]}
      >
        <TextArea rows={4} placeholder="请输入药品的注意事项" showCount maxLength={1000} />
      </Form.Item>

      <Form.Item
        label="禁忌"
        name={['drugDetail', 'taboo']}
        rules={[{ max: 1000, message: '禁忌不能超过1000个字符' }]}
      >
        <TextArea rows={4} placeholder="请输入药品的禁忌" showCount maxLength={1000} />
      </Form.Item>

      <Form.Item
        label="药品说明书全文"
        name={['drugDetail', 'instruction']}
        rules={[{ max: 5000, message: '药品说明书不能超过5000个字符' }]}
      >
        <TextArea rows={6} placeholder="请输入完整的药品说明书内容" showCount maxLength={5000} />
      </Form.Item>

      <Form.Item
        label="温馨提示"
        name={['drugDetail', 'warmTips']}
        rules={[
          { required: true, message: '请输入温馨提示' },
          { max: 1000, message: '温馨提示不能超过1000个字符' },
        ]}
      >
        <TextArea
          rows={4}
          placeholder="请输入温馨提示，如用药注意事项、储存方式等"
          showCount
          maxLength={1000}
        />
      </Form.Item>
    </>
  );
};

export default DrugDetail;
