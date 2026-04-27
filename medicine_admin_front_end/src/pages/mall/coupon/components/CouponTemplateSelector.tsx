import { Descriptions, Empty, Select, Tag, message } from 'antd';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { getCouponTemplateById, listCouponTemplate, type MallCouponTypes } from '@/api/mall/coupon';

type TemplateRecord = MallCouponTypes.CouponTemplateVo;

/**
 * 仅查询启用模板的状态值。
 */
const ACTIVE_TEMPLATE_STATUS = 'ACTIVE';

/**
 * 模板下拉一次拉取的最大数量。
 */
const TEMPLATE_PAGE_SIZE = 200;

/**
 * 优惠券模板选择组件属性。
 */
interface CouponTemplateSelectorProps {
  /** 当前选中的模板ID */
  value?: number;
  /** 模板切换回调 */
  onChange?: (value?: number) => void;
  /** 是否禁用 */
  disabled?: boolean;
}

/**
 * 金额格式化工具。
 * @param value 原始金额。
 * @returns 金额文本。
 */
function formatCurrency(value?: string): string {
  if (!value) {
    return '-';
  }
  return `¥${Number(value).toFixed(2)}`;
}

/**
 * 获取模板下拉展示文本。
 * @param template 模板信息。
 * @returns 模板展示文本。
 */
function getTemplateLabel(template?: TemplateRecord): string {
  if (!template) {
    return '未命名模板';
  }
  if (template.name?.trim()) {
    return template.name.trim();
  }
  return '未命名模板';
}

/**
 * 优惠券模板选择组件。
 * @param props 组件属性。
 * @returns 模板选择节点。
 */
const CouponTemplateSelector: React.FC<CouponTemplateSelectorProps> = ({
  value,
  onChange,
  disabled = false,
}) => {
  const [messageApi, contextHolder] = message.useMessage();
  const [loading, setLoading] = useState(false);
  const [templateOptions, setTemplateOptions] = useState<TemplateRecord[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState<TemplateRecord | undefined>(undefined);

  /**
   * 加载启用中的模板列表。
   * @returns 无返回值。
   */
  const loadTemplateOptions = useCallback(async () => {
    setLoading(true);
    try {
      const result = await listCouponTemplate({
        status: ACTIVE_TEMPLATE_STATUS,
        pageNum: 1,
        pageSize: TEMPLATE_PAGE_SIZE,
      });
      setTemplateOptions(result?.rows ?? []);
    } catch (error) {
      console.error('加载优惠券模板失败:', error);
      messageApi.error('加载优惠券模板失败');
    } finally {
      setLoading(false);
    }
  }, [messageApi]);

  /**
   * 按模板ID加载模板详情。
   * @param templateId 模板ID。
   * @returns 无返回值。
   */
  const loadTemplateDetail = useCallback(
    async (templateId: number) => {
      try {
        const template = await getCouponTemplateById(templateId);
        setSelectedTemplate(template);
      } catch (error) {
        console.error('加载优惠券模板详情失败:', error);
        messageApi.error('加载优惠券模板详情失败');
      }
    },
    [messageApi],
  );

  /**
   * 处理模板切换。
   * @param templateId 模板ID。
   * @returns 无返回值。
   */
  const handleTemplateChange = useCallback(
    (templateId?: number) => {
      const currentTemplate = templateOptions.find((item) => item.id === templateId);
      setSelectedTemplate(currentTemplate);
      onChange?.(templateId);
    },
    [onChange, templateOptions],
  );

  /**
   * 下拉框可选项。
   */
  const selectOptions = useMemo(() => {
    const optionMap = new Map<number, { label: string; value: number }>();
    templateOptions.forEach((item) => {
      if (item.id) {
        optionMap.set(item.id, {
          label: getTemplateLabel(item),
          value: item.id,
        });
      }
    });
    if (selectedTemplate?.id && !optionMap.has(selectedTemplate.id)) {
      optionMap.set(selectedTemplate.id, {
        label: getTemplateLabel(selectedTemplate),
        value: selectedTemplate.id,
      });
    }
    return Array.from(optionMap.values());
  }, [selectedTemplate, templateOptions]);

  /**
   * 当前选中模板展示文本。
   */
  const selectedTemplateLabel = useMemo(() => {
    if (selectedTemplate) {
      return getTemplateLabel(selectedTemplate);
    }
    const currentTemplate = templateOptions.find((item) => item.id === value);
    return currentTemplate ? getTemplateLabel(currentTemplate) : undefined;
  }, [selectedTemplate, templateOptions, value]);

  useEffect(() => {
    void loadTemplateOptions();
  }, [loadTemplateOptions]);

  useEffect(() => {
    if (!value) {
      setSelectedTemplate(undefined);
      return;
    }
    const currentTemplate = templateOptions.find((item) => item.id === value);
    if (currentTemplate) {
      setSelectedTemplate(currentTemplate);
      return;
    }
    void loadTemplateDetail(value);
  }, [loadTemplateDetail, templateOptions, value]);

  return (
    <>
      {contextHolder}
      <div
        className="coupon-template-selector"
        style={{
          background: '#ffffff',
          padding: selectedTemplate ? 16 : 0,
          borderRadius: 8,
          border: selectedTemplate ? '1px solid #f0f0f0' : 'none',
        }}
      >
        <div style={{ display: 'grid', gap: 16 }}>
          <Select<number>
            value={value}
            disabled={disabled}
            loading={loading}
            showSearch
            allowClear
            placeholder="请选择优惠券模板"
            optionLabelProp="label"
            optionFilterProp="label"
            options={selectOptions}
            labelRender={(option) => selectedTemplateLabel || String(option.label ?? '')}
            onChange={(templateId) => handleTemplateChange(templateId)}
            style={{ width: '100%' }}
          />

          {selectedTemplate ? (
            <Descriptions
              size="small"
              column={2}
              bordered
              items={[
                {
                  key: 'id',
                  label: '模板ID',
                  children: selectedTemplate.id ?? '-',
                },
                {
                  key: 'status',
                  label: '模板状态',
                  children: (
                    <Tag
                      color={
                        selectedTemplate.status === ACTIVE_TEMPLATE_STATUS ? 'success' : 'default'
                      }
                    >
                      {selectedTemplate.status || '-'}
                    </Tag>
                  ),
                },
                {
                  key: 'name',
                  label: '模板名称',
                  children: selectedTemplate.name || '-',
                },
                {
                  key: 'faceAmount',
                  label: '优惠券面额',
                  children: formatCurrency(selectedTemplate.faceAmount),
                },
                {
                  key: 'thresholdAmount',
                  label: '使用门槛',
                  children: formatCurrency(selectedTemplate.thresholdAmount),
                },
                {
                  key: 'continueUseEnabled',
                  label: '允许继续使用',
                  children: Number(selectedTemplate.continueUseEnabled) === 1 ? '允许' : '不允许',
                },
                {
                  key: 'stackableEnabled',
                  label: '允许叠加',
                  children: Number(selectedTemplate.stackableEnabled) === 1 ? '允许' : '不允许',
                },
                {
                  key: 'remark',
                  label: '模板备注',
                  children: selectedTemplate.remark || '-',
                },
              ]}
            />
          ) : (
            <div
              style={{
                padding: '24px 0',
                textAlign: 'center',
                background: '#ffffff',
                border: '1px dashed #e8e8e8',
                borderRadius: 8,
              }}
            >
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂未选择优惠券模板" />
            </div>
          )}
        </div>
      </div>
    </>
  );
};

export default CouponTemplateSelector;
