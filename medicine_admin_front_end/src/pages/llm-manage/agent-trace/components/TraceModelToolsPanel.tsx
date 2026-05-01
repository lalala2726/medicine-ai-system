import { Space, Tag, Typography } from 'antd';
import { ChevronDown, ChevronRight, Wrench } from 'lucide-react';
import React, { useEffect, useState } from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import { renderJsonPayload } from './traceShared';
import TraceToolCallCard from './TraceToolCallCard';
import { renderModelEmpty, resolveToolName } from './traceModelUtils';

const { Text } = Typography;

export interface TraceModelToolsPanelProps {
  /** 当前模型可见工具列表。 */
  tools?: AgentTraceTypes.ModelToolVo[];
  /** 当前模型发起的工具调用列表。 */
  toolCalls?: AgentTraceTypes.ModelToolCallVo[];
}

/**
 * 将未知 schema 转成普通对象。
 *
 * @param schema 工具参数 Schema。
 * @returns 普通对象；无法结构化时返回 null。
 */
function toSchemaRecord(schema: unknown): Record<string, unknown> | null {
  if (typeof schema === 'string') {
    try {
      const parsedValue = JSON.parse(schema);
      return typeof parsedValue === 'object' && parsedValue !== null && !Array.isArray(parsedValue)
        ? (parsedValue as Record<string, unknown>)
        : null;
    } catch {
      return null;
    }
  }
  return typeof schema === 'object' && schema !== null && !Array.isArray(schema)
    ? (schema as Record<string, unknown>)
    : null;
}

/**
 * 读取 schema 字符串字段。
 *
 * @param schema Schema 对象。
 * @param key 字段名。
 * @returns 字符串字段；不存在时返回空值。
 */
function readSchemaText(schema: Record<string, unknown>, key: string) {
  const value = schema[key];
  if (typeof value === 'string') {
    return value.trim() || null;
  }
  if (Array.isArray(value)) {
    return value.filter((item) => typeof item === 'string').join(' | ') || null;
  }
  return null;
}

/**
 * 读取 schema properties。
 *
 * @param schema Schema 对象。
 * @returns 参数属性列表。
 */
function readSchemaProperties(schema: Record<string, unknown>) {
  const properties = schema.properties;
  if (typeof properties !== 'object' || properties === null || Array.isArray(properties)) {
    return [];
  }
  return Object.entries(properties as Record<string, unknown>)
    .filter(([, value]) => typeof value === 'object' && value !== null && !Array.isArray(value))
    .map(([name, value]) => ({
      name,
      schema: value as Record<string, unknown>,
    }));
}

/**
 * 读取 schema required 字段。
 *
 * @param schema Schema 对象。
 * @returns 必填字段集合。
 */
function readRequiredNames(schema: Record<string, unknown>) {
  const required = schema.required;
  if (!Array.isArray(required)) {
    return new Set<string>();
  }
  return new Set(required.filter((item): item is string => typeof item === 'string'));
}

/**
 * 渲染数组 items 结构。
 *
 * @param propertySchema 参数 schema。
 * @returns items 节点。
 */
function renderArrayItems(propertySchema: Record<string, unknown>) {
  const items = toSchemaRecord(propertySchema.items);
  if (!items) {
    return null;
  }
  return (
    <div className={styles.modelToolSchemaNested}>
      <Text type="secondary">items</Text>
      <div className={styles.modelToolSchemaLine}>
        <Text>Type</Text>
        <Tag>{readSchemaText(items, 'type') || '-'}</Tag>
      </div>
    </div>
  );
}

/**
 * 渲染单个工具参数 schema。
 *
 * @param name 参数名。
 * @param propertySchema 参数 schema。
 * @param requiredNames 必填参数集合。
 * @returns 参数 schema 节点。
 */
function renderSchemaProperty(
  name: string,
  propertySchema: Record<string, unknown>,
  requiredNames: Set<string>,
) {
  const typeText = readSchemaText(propertySchema, 'type') || '-';
  const descriptionText = readSchemaText(propertySchema, 'description');
  const isRequired = requiredNames.has(name);

  return (
    <div key={name} className={styles.modelToolSchemaProperty}>
      <div className={styles.modelToolSchemaPropertyName}>{name}</div>
      <div className={styles.modelToolSchemaLine}>
        <Text>Type</Text>
        <Tag>{typeText}</Tag>
      </div>
      {descriptionText ? (
        <div className={styles.modelToolSchemaLine}>
          <Text>Description</Text>
          <Text>{descriptionText}</Text>
        </div>
      ) : null}
      <div className={styles.modelToolSchemaLine}>
        <Text>Required</Text>
        <Tag color={isRequired ? 'success' : 'default'}>{isRequired ? 'true' : 'false'}</Tag>
      </div>
      {renderArrayItems(propertySchema)}
    </div>
  );
}

/**
 * 渲染工具参数 Schema。
 *
 * @param schema 工具参数 Schema。
 * @returns Schema 节点。
 */
function renderToolSchema(schema: unknown) {
  const schemaRecord = toSchemaRecord(schema);
  if (!schemaRecord) {
    return <div className={styles.modelJsonBlock}>{renderJsonPayload(schema)}</div>;
  }
  const properties = readSchemaProperties(schemaRecord);
  if (!properties.length) {
    return <div className={styles.modelJsonBlock}>{renderJsonPayload(schemaRecord)}</div>;
  }
  const requiredNames = readRequiredNames(schemaRecord);
  return (
    <div className={styles.modelToolSchemaView}>
      {properties.map((property) =>
        renderSchemaProperty(property.name, property.schema, requiredNames),
      )}
    </div>
  );
}

/**
 * 解析工具调用列表。
 *
 * @param tool 当前工具。
 * @param allToolCalls 当前模型全部工具调用。
 * @returns 当前工具关联调用列表。
 */
function resolveToolCalls(
  tool: AgentTraceTypes.ModelToolVo,
  allToolCalls: AgentTraceTypes.ModelToolCallVo[],
) {
  if (tool.calls?.length) {
    return tool.calls;
  }
  return allToolCalls.filter((call) => call.name === tool.name);
}

/**
 * 解析工具描述摘要。
 *
 * @param tool 当前工具。
 * @returns 工具描述文本。
 */
function resolveToolDescription(tool: AgentTraceTypes.ModelToolVo) {
  return tool.description?.trim() || '暂无工具描述';
}

/**
 * 解析工具展开状态键。
 *
 * @param tool 当前工具。
 * @returns 展开状态键。
 */
function resolveToolExpandKey(tool: AgentTraceTypes.ModelToolVo) {
  return tool.name || resolveToolName(tool);
}

/**
 * 渲染工具调用记录。
 *
 * @param calls 工具调用列表。
 * @returns 工具调用记录节点。
 */
function renderToolCalls(calls: AgentTraceTypes.ModelToolCallVo[]) {
  if (!calls.length) {
    return renderModelEmpty('本轮未调用该工具');
  }
  return (
    <div className={styles.modelToolCalls}>
      {calls.map((call, index) => (
        <TraceToolCallCard key={call.id || `${call.name}-${index}`} call={call} />
      ))}
    </div>
  );
}

/**
 * 模型可见工具面板。
 *
 * @param props 组件属性。
 * @returns 可见工具面板节点。
 */
const TraceModelToolsPanel: React.FC<TraceModelToolsPanelProps> = ({
  tools = [],
  toolCalls = [],
}) => {
  const [expandedToolName, setExpandedToolName] = useState<string | undefined>();

  /**
   * 工具列表变化时只清理失效展开项，不自动展开任何工具。
   *
   * @returns 无返回值。
   */
  useEffect(() => {
    if (
      expandedToolName &&
      !tools.some((tool) => resolveToolExpandKey(tool) === expandedToolName)
    ) {
      setExpandedToolName(undefined);
    }
  }, [expandedToolName, tools]);

  if (!tools.length) {
    return <div className={styles.modelPanelContent}>{renderModelEmpty('暂无可见工具')}</div>;
  }

  const calledToolCount = tools.filter((tool) => {
    const calls = resolveToolCalls(tool, toolCalls);
    return Boolean(tool.called || calls.length);
  }).length;

  return (
    <div className={styles.modelPanelContent}>
      <div className={styles.modelToolSummaryBar}>
        <div className={styles.modelToolSummaryTitle}>
          <Text strong>可见工具</Text>
          <Text type="secondary">{tools.length.toLocaleString()} 个工具</Text>
        </div>
        <Space size={6} wrap>
          <Tag color="success">已调用 {calledToolCount.toLocaleString()}</Tag>
          <Tag>未调用 {(tools.length - calledToolCount).toLocaleString()}</Tag>
        </Space>
      </div>
      <div className={styles.modelToolRunList}>
        {tools.map((tool) => {
          const toolName = resolveToolName(tool);
          const calls = resolveToolCalls(tool, toolCalls);
          const toolKey = resolveToolExpandKey(tool);
          const isExpanded = expandedToolName === toolKey;
          const isCalled = Boolean(tool.called || calls.length);

          return (
            <div key={toolKey} className={styles.modelToolRunItem}>
              <button
                type="button"
                className={styles.modelToolRunButton}
                aria-expanded={isExpanded}
                onClick={() => setExpandedToolName(isExpanded ? undefined : toolKey)}
              >
                <span className={styles.modelToolRunIcon}>
                  <Wrench size={14} />
                </span>
                <div className={styles.modelToolRunContent}>
                  <div className={styles.modelToolRunHeaderLine}>
                    <Text strong className={styles.modelToolRunName}>
                      {toolName}
                    </Text>
                    <Space size={6} wrap>
                      {isCalled ? <Tag color="success">已调用</Tag> : <Tag>未调用</Tag>}
                      {calls.length ? (
                        <Tag color="blue">{calls.length.toLocaleString()} 次</Tag>
                      ) : null}
                    </Space>
                  </div>
                  <Text type="secondary" className={styles.modelToolRunDescription}>
                    {resolveToolDescription(tool)}
                  </Text>
                </div>
                <span className={styles.modelToolRunChevron}>
                  {isExpanded ? <ChevronDown size={15} /> : <ChevronRight size={15} />}
                </span>
              </button>
              {isExpanded ? (
                <div className={styles.modelToolRunDetail}>
                  <div className={styles.modelToolDetailSection}>
                    <Text strong>工具描述</Text>
                    <div className={styles.modelToolDescription}>
                      {resolveToolDescription(tool)}
                    </div>
                  </div>
                  <div className={styles.modelToolSchema}>
                    <Text strong>参数 Schema</Text>
                    {renderToolSchema(tool.argsSchema)}
                  </div>
                  <div className={styles.modelToolCallsBlock}>
                    <Text strong>调用记录</Text>
                    {renderToolCalls(calls)}
                  </div>
                </div>
              ) : null}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default TraceModelToolsPanel;
