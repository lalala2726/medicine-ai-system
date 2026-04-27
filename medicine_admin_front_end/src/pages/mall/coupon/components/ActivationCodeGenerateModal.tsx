import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import {
  Button,
  DatePicker,
  Drawer,
  Form,
  Input,
  InputNumber,
  Radio,
  Space,
  Typography,
  message,
} from 'antd';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import type { CaptchaVerificationResult } from '@/api/core/captcha';
import { generateActivationCodes, type MallCouponTypes } from '@/api/mall/coupon';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import SliderCaptchaModal from '@/pages/login/components/SliderCaptchaModal';
import {
  buildActivationCodeGeneratePayload,
  type ActivationCodeGenerateFormValues,
} from './activationCodeGenerateHelper';
import CouponTemplateSelector from './CouponTemplateSelector';

/**
 * 固定时间范围字段名。
 */
const FIXED_RANGE_FIELD_NAME = 'fixedRange';

/**
 * 默认生成数量。
 */
const DEFAULT_GENERATE_COUNT = 1;

/**
 * 兑换规则选项。
 */
const ACTIVATION_CODE_MODE_OPTIONS = [
  { label: '共享码', value: 'SHARED_PER_USER_ONCE' },
  { label: '唯一码', value: 'UNIQUE_SINGLE_USE' },
] satisfies Array<{
  label: string;
  value: MallCouponTypes.ActivationRedeemRuleType;
}>;

/**
 * 有效期类型选项。
 */
const ACTIVATION_VALIDITY_TYPE_OPTIONS = [
  { label: '一次性', value: 'ONCE' },
  { label: '激活后计算', value: 'AFTER_ACTIVATION' },
] satisfies Array<{
  label: string;
  value: MallCouponTypes.ActivationCodeValidityType;
}>;

/**
 * 激活码生成抽屉属性。
 */
interface ActivationCodeGenerateModalProps {
  /** 是否打开抽屉 */
  open: boolean;
  /** 抽屉开关回调 */
  onOpenChange: (open: boolean) => void;
  /** 生成成功回调 */
  onGenerated: (result: MallCouponTypes.ActivationCodeGenerateResultVo) => void;
}

/**
 * 解析有效期类型文案。
 * @param value 有效期类型编码。
 * @returns 有效期类型文案。
 */
function activationValidityTypeText(value?: string): string {
  return (
    ACTIVATION_VALIDITY_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || '-'
  );
}

/**
 * 构建导出文本内容。
 * @param result 生成结果。
 * @returns 导出文本内容。
 */
function buildExportText(result?: MallCouponTypes.ActivationCodeGenerateResultVo): string {
  if (!result?.codes?.length) {
    return '';
  }
  const headerLines = [
    `批次号：${result.batchNo || '-'}`,
    `模板名称：${result.templateName || '-'}`,
    `兑换规则：${result.redeemRuleType || '-'}`,
    `有效期类型：${activationValidityTypeText(result.validityType)}`,
    '',
    '激活码列表：',
  ];
  const codeLines = result.codes.map((item, index) => `${index + 1}. ${item.plainCode || ''}`);
  return [...headerLines, ...codeLines].join('\n');
}

/**
 * 激活码生成抽屉。
 * @param props 组件属性。
 * @returns 抽屉节点。
 */
const ActivationCodeGenerateModal: React.FC<ActivationCodeGenerateModalProps> = ({
  open,
  onOpenChange,
  onGenerated,
}) => {
  const [messageApi, contextHolder] = message.useMessage();
  const [form] = Form.useForm<ActivationCodeGenerateFormValues>();
  const [submitting, setSubmitting] = useState(false);
  const [captchaOpen, setCaptchaOpen] = useState(false);
  const [pendingValues, setPendingValues] = useState<ActivationCodeGenerateFormValues | null>(null);
  const [generatedResult, setGeneratedResult] = useState<
    MallCouponTypes.ActivationCodeGenerateResultVo | undefined
  >(undefined);
  const redeemRuleType = Form.useWatch('redeemRuleType', form) || 'SHARED_PER_USER_ONCE';
  const validityType = Form.useWatch('validityType', form) || 'ONCE';

  /**
   * 表单初始值。
   */
  const initialValues = useMemo<ActivationCodeGenerateFormValues>(
    () => ({
      redeemRuleType: 'SHARED_PER_USER_ONCE',
      generateCount: DEFAULT_GENERATE_COUNT,
      validityType: 'ONCE',
    }),
    [],
  );

  useEffect(() => {
    if (!open) {
      form.resetFields();
      setCaptchaOpen(false);
      setPendingValues(null);
      setGeneratedResult(undefined);
      return;
    }
    form.setFieldsValue(initialValues);
  }, [form, initialValues, open]);

  /**
   * 提交激活码生成请求。
   * @returns 无返回值。
   */
  const submitGenerateRequest = useCallback(
    async (payload: MallCouponTypes.ActivationCodeGenerateRequest) => {
      try {
        setSubmitting(true);
        const result = await generateActivationCodes(payload);
        setGeneratedResult(result);
        onGenerated(result);
        messageApi.success('激活码生成成功');
      } catch (error) {
        if (error instanceof Error) {
          console.error('生成激活码失败:', error);
        }
      } finally {
        setSubmitting(false);
      }
    },
    [messageApi, onGenerated],
  );

  /**
   * 点击生成后先拉起滑块验证码。
   * @returns 无返回值。
   */
  const handleSubmit = useCallback(async () => {
    try {
      const values = await form.validateFields();
      setPendingValues(values);
      setCaptchaOpen(true);
    } catch (error) {
      if (error instanceof Error) {
        console.error('生成激活码失败:', error);
      }
    }
  }, [form]);

  /**
   * 取消滑块验证。
   * @returns 无返回值。
   */
  const handleCaptchaCancel = useCallback(() => {
    setCaptchaOpen(false);
    setPendingValues(null);
  }, []);

  /**
   * 滑块验证通过后提交生成请求。
   * @param captchaVerificationResult 验证码结果。
   * @returns 无返回值。
   */
  const handleCaptchaVerified = useCallback(
    async (captchaVerificationResult: CaptchaVerificationResult) => {
      if (!pendingValues) {
        setCaptchaOpen(false);
        return;
      }
      const payload = buildActivationCodeGeneratePayload(
        pendingValues,
        captchaVerificationResult.id,
      );
      setCaptchaOpen(false);
      setPendingValues(null);
      await submitGenerateRequest(payload);
    },
    [pendingValues, submitGenerateRequest],
  );

  /**
   * 关闭抽屉。
   * @returns 无返回值。
   */
  const handleClose = useCallback(() => {
    setCaptchaOpen(false);
    setPendingValues(null);
    onOpenChange(false);
  }, [onOpenChange]);

  /**
   * 返回表单态。
   * @returns 无返回值。
   */
  const handleBackToForm = useCallback(() => {
    setGeneratedResult(undefined);
  }, []);

  /**
   * 复制全部激活码。
   * @returns 无返回值。
   */
  const handleCopyAll = useCallback(async () => {
    const text = (generatedResult?.codes || [])
      .map((item) => item.plainCode)
      .filter((code): code is string => Boolean(code))
      .join('\n');
    if (!text) {
      messageApi.warning('暂无可复制的激活码');
      return;
    }
    try {
      await navigator.clipboard.writeText(text);
      messageApi.success('激活码已复制');
    } catch (error) {
      console.error('复制激活码失败:', error);
      messageApi.error('复制激活码失败，请手动复制');
    }
  }, [generatedResult, messageApi]);

  /**
   * 导出激活码文本文件。
   * @returns 无返回值。
   */
  const handleExport = useCallback(() => {
    const exportText = buildExportText(generatedResult);
    if (!exportText) {
      messageApi.warning('暂无可导出的激活码');
      return;
    }
    const blob = new Blob([exportText], { type: 'text/plain;charset=utf-8' });
    const downloadUrl = URL.createObjectURL(blob);
    const downloadLink = document.createElement('a');
    downloadLink.href = downloadUrl;
    downloadLink.download = `${generatedResult?.batchNo || 'activation-codes'}.txt`;
    downloadLink.click();
    URL.revokeObjectURL(downloadUrl);
  }, [generatedResult, messageApi]);

  return (
    <>
      {contextHolder}
      <Drawer
        title={generatedResult ? '激活码生成结果' : '生成激活码'}
        open={open}
        width={760}
        destroyOnHidden
        onClose={handleClose}
        extra={
          generatedResult ? (
            <Space>
              <Button onClick={handleBackToForm}>继续生成</Button>
              <Button onClick={handleCopyAll}>复制全部</Button>
              <Button onClick={handleExport}>导出文本</Button>
              <Button type="primary" onClick={handleClose}>
                关闭
              </Button>
            </Space>
          ) : (
            <Space>
              <Button onClick={handleClose}>取消</Button>
              <PermissionButton
                type="primary"
                loading={submitting}
                access={ADMIN_PERMISSIONS.mallCoupon.activationBatchGenerate}
                onClick={() => void handleSubmit()}
              >
                生成
              </PermissionButton>
            </Space>
          )
        }
      >
        {generatedResult ? (
          <div style={{ display: 'grid', gap: 16 }}>
            <Space direction="vertical" size={4}>
              <Typography.Text>批次号：{generatedResult.batchNo || '-'}</Typography.Text>
              <Typography.Text>模板名称：{generatedResult.templateName || '-'}</Typography.Text>
              <Typography.Text>兑换规则：{generatedResult.redeemRuleType || '-'}</Typography.Text>
              <Typography.Text>
                有效期类型：{activationValidityTypeText(generatedResult.validityType)}
              </Typography.Text>
              <Typography.Text>本次生成数量：{generatedResult.generatedCount || 0}</Typography.Text>
            </Space>

            <Typography.Text type="warning">
              激活码明文仅在当前抽屉展示，请及时复制或导出保存。
            </Typography.Text>

            <div
              style={{
                display: 'grid',
                gap: 10,
                maxHeight: 520,
                overflowY: 'auto',
                paddingRight: 4,
              }}
            >
              {(generatedResult.codes || []).map((item, index) => (
                <div
                  key={String(item.id || item.plainCode || index)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 12,
                    padding: '12px 14px',
                    border: '1px solid #f0f0f0',
                    borderRadius: 10,
                    background: '#fff',
                  }}
                >
                  <Typography.Text type="secondary" style={{ width: 40 }}>
                    {index + 1}.
                  </Typography.Text>
                  <Typography.Text
                    copyable={item.plainCode ? { text: item.plainCode } : false}
                    style={{ flex: 1, fontFamily: 'monospace' }}
                  >
                    {item.plainCode || '-'}
                  </Typography.Text>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <Form<ActivationCodeGenerateFormValues>
            form={form}
            layout="vertical"
            initialValues={initialValues}
          >
            <Form.Item
              label="优惠券模板"
              name="templateId"
              rules={[{ required: true, message: '请选择优惠券模板' }]}
            >
              <CouponTemplateSelector />
            </Form.Item>

            <Form.Item
              label="兑换规则"
              name="redeemRuleType"
              rules={[{ required: true, message: '请选择兑换规则' }]}
            >
              <Radio.Group options={ACTIVATION_CODE_MODE_OPTIONS} optionType="button" />
            </Form.Item>

            {redeemRuleType === 'UNIQUE_SINGLE_USE' && (
              <Form.Item
                label="生成数量"
                name="generateCount"
                rules={[{ required: true, message: '请输入生成数量' }]}
              >
                <InputNumber
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  min={1}
                  precision={0}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            )}

            <Form.Item
              label="优惠券有效期"
              name="validityType"
              rules={[{ required: true, message: '请选择优惠券有效期类型' }]}
            >
              <Radio.Group options={ACTIVATION_VALIDITY_TYPE_OPTIONS} optionType="button" />
            </Form.Item>

            {validityType === 'ONCE' && (
              <Form.Item
                label="固定生效区间"
                name={FIXED_RANGE_FIELD_NAME}
                rules={[{ required: true, message: '请选择固定生效区间' }]}
              >
                <DatePicker.RangePicker showTime style={{ width: '100%' }} />
              </Form.Item>
            )}

            {validityType === 'AFTER_ACTIVATION' && (
              <Form.Item
                label="激活后有效天数"
                name="relativeValidDays"
                rules={[{ required: true, message: '请输入激活后有效天数' }]}
              >
                <InputNumber
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  min={1}
                  precision={0}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            )}

            <Form.Item label="备注" name="remark">
              <Input.TextArea
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                maxLength={200}
                showCount
                autoSize={{ minRows: 2, maxRows: 4 }}
              />
            </Form.Item>
          </Form>
        )}
      </Drawer>
      <SliderCaptchaModal
        open={captchaOpen}
        onCancel={handleCaptchaCancel}
        onVerified={handleCaptchaVerified}
      />
    </>
  );
};

export default ActivationCodeGenerateModal;
