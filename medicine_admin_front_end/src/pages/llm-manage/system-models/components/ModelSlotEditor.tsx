import { Form, Select, Switch, Tag, Typography } from 'antd';
import React, { useEffect, useMemo } from 'react';

import type { SystemModelTypes } from '@/api/llm-manage/systemModels';

import styles from '../index.module.less';

interface ModelSlotEditorProps {
  title: string;
  description: string;
  name: string;
  options: SystemModelTypes.ModelOption[];
  invalidSelectionHint?: string;
  optionsLoading?: boolean;
  required?: boolean;
  allowClear?: boolean;
  emptyHint?: string;
  showReasoningToggle?: boolean;
  testId?: string;
}

/**
 * 模型槽位编辑项，统一承载模型选择、模型能力展示和场景参数输入。
 */
const ModelSlotEditor: React.FC<ModelSlotEditorProps> = ({
  title,
  description,
  name,
  options,
  invalidSelectionHint,
  optionsLoading = false,
  required = true,
  allowClear = false,
  emptyHint = '暂无可用模型',
  showReasoningToggle = true,
  testId,
}) => {
  const form = Form.useFormInstance();
  const selectedModelName = Form.useWatch([name, 'modelName'], form) as string | undefined;
  const reasoningEnabled = Form.useWatch([name, 'reasoningEnabled'], form) as boolean | undefined;

  const selectedOption = useMemo(
    () => options.find((item) => item.value === selectedModelName),
    [options, selectedModelName],
  );

  useEffect(() => {
    if (selectedOption && !selectedOption.supportReasoning && reasoningEnabled) {
      form.setFieldValue([name, 'reasoningEnabled'], false);
    }
  }, [form, name, reasoningEnabled, selectedOption]);

  return (
    <div className={styles.slotCard} data-testid={testId}>
      <div className={styles.slotHeader}>
        <div>
          <div className={styles.slotTitle}>{title}</div>
          <Typography.Text type="secondary" className={styles.slotDescription}>
            {description}
          </Typography.Text>
        </div>
      </div>

      <Form.Item
        label="模型名称"
        name={[name, 'modelName']}
        rules={required ? [{ required: true, message: '请选择模型' }] : undefined}
        className={styles.compactFormItem}
      >
        <Select
          allowClear={allowClear}
          showSearch
          optionFilterProp="label"
          placeholder="请选择模型"
          options={options}
          loading={optionsLoading}
          disabled={!optionsLoading && options.length === 0}
          notFoundContent={optionsLoading ? '加载中...' : '暂无可用模型'}
        />
      </Form.Item>

      {invalidSelectionHint ? (
        <Typography.Text type="warning" className={styles.inlineEmptyHint}>
          {invalidSelectionHint}
        </Typography.Text>
      ) : null}

      {!optionsLoading && options.length === 0 && emptyHint ? (
        <Typography.Text type="secondary" className={styles.inlineEmptyHint}>
          {emptyHint}
        </Typography.Text>
      ) : null}

      {selectedOption?.supportReasoning || selectedOption?.supportVision ? (
        <div className={styles.capabilityRow}>
          {selectedOption?.supportReasoning ? (
            <Tag color="geekblue" bordered={false}>
              支持深度思考
            </Tag>
          ) : null}
          {selectedOption?.supportVision ? (
            <Tag color="cyan" bordered={false}>
              支持图片理解
            </Tag>
          ) : null}
        </div>
      ) : null}

      {showReasoningToggle && selectedOption?.supportReasoning ? (
        <Form.Item
          label="启用深度思考"
          name={[name, 'reasoningEnabled']}
          valuePropName="checked"
          className={styles.compactFormItem}
        >
          <Switch checkedChildren="开启" unCheckedChildren="关闭" />
        </Form.Item>
      ) : null}
    </div>
  );
};

export default ModelSlotEditor;
