import { FishboneDiagram } from '@antv/gpt-vis';
import React from 'react';
import { useChartTheme } from '../../../_utils';
import ChartLoader from '../ChartLoader';
import { parseChartJSON } from '../parseChartJSON';

interface FishboneDiagramChartProps {
  children?: React.ReactNode;
}

/**
 * 自定义鱼骨图组件
 * 用于在 Markdown 中渲染 fishbonediagram 代码块
 * 用于展示问题根本原因分析、因果关系等
 */
const FishboneDiagramChart: React.FC<FishboneDiagramChartProps> = ({ children }) => {
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
          错误:鱼骨图数据格式不正确,需要提供 data 对象
        </div>
      );
    }

    // 验证数据结构
    if (!data.name) {
      return <div style={{ padding: 20, color: '#ff4d4f' }}>错误:鱼骨图数据必须包含 name 字段</div>;
    }

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
        <FishboneDiagram data={data} theme={theme} containerStyle={{ height, width: '100%' }} />
      </div>
    );
  } catch {
    return <div style={{ padding: 20, color: '#ff4d4f' }}>图表数据格式不正确，无法解析</div>;
  }
};

export default FishboneDiagramChart;
