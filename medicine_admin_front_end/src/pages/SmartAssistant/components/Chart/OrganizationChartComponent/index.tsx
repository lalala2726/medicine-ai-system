import { OrganizationChart } from '@antv/gpt-vis';
import React from 'react';
import { useChartTheme } from '../../../_utils';
import ChartLoader from '../ChartLoader';
import { parseChartJSON } from '../parseChartJSON';

interface OrganizationChartProps {
  children?: React.ReactNode;
}

/**
 * 自定义组织架构图组件
 * 用于在 Markdown 中渲染 organizationchart 代码块
 * 用于展示组织结构、员工层级等
 */
const OrganizationChartComponent: React.FC<OrganizationChartProps> = ({ children }) => {
  const content = String(children).trim();
  const defaultTheme = useChartTheme();

  try {
    const config = parseChartJSON(content);
    if (!config) return <ChartLoader height={600} />;
    const { data, theme = defaultTheme, height = 600 } = config;

    // 确保数据存在
    if (!data || typeof data !== 'object') {
      return (
        <div style={{ padding: 20, color: '#ff4d4f' }}>
          错误:组织架构图数据格式不正确,需要提供 data 对象
        </div>
      );
    }

    // 验证数据结构
    if (!data.name) {
      return (
        <div style={{ padding: 20, color: '#ff4d4f' }}>错误:组织架构图数据必须包含 name 字段</div>
      );
    }

    /**
     * gpt-vis 当前类型定义未暴露 theme 字段，这里按运行时能力透传主题配置。
     */
    const organizationChartProps = {
      data,
      containerStyle: { height, width: '100%', minWidth: 1000 },
      theme,
    } as any;

    return (
      <div
        style={{
          width: '100%',
          height: height,
          overflow: 'hidden',
          border: '1px solid #f0f0f0',
          borderRadius: '4px',
        }}
      >
        <OrganizationChart {...organizationChartProps} />
      </div>
    );
  } catch {
    return <div style={{ padding: 20, color: '#ff4d4f' }}>图表数据格式不正确，无法解析</div>;
  }
};

export default OrganizationChartComponent;
