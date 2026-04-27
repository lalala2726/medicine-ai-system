import { Scatter } from '@antv/gpt-vis';
import React from 'react';
import { useChartTheme } from '../../../_utils';
import ChartLoader from '../ChartLoader';
import { parseChartJSON } from '../parseChartJSON';

interface ScatterChartProps {
  children?: React.ReactNode;
  className?: string;
}

/**
 * 自定义散点图组件
 * 用于在 Markdown 中渲染 scatter 代码块
 * 用于展示两个变量之间的相关关系
 */
const ScatterChart: React.FC<ScatterChartProps> = ({ children }) => {
  const content = String(children).trim();
  const defaultTheme = useChartTheme();

  try {
    const config = parseChartJSON(content);
    if (!config) return <ChartLoader height={400} />;
    const {
      data,
      axisXTitle = 'X轴',
      axisYTitle = 'Y轴',
      theme = defaultTheme,
      backgroundColor = 'transparent',
      palette = [],
      height = 400,
    } = config;

    // 确保数据存在
    if (!data || !Array.isArray(data)) {
      return (
        <div style={{ padding: 20, color: '#ff4d4f' }}>
          错误:散点图数据格式不正确,需要提供 data 数组
        </div>
      );
    }

    // 构建样式对象
    const style: any = {};

    if (backgroundColor) {
      style.backgroundColor = backgroundColor;
    }

    if (palette && palette.length > 0) {
      style.palette = palette;
    }

    return (
      <div style={{ width: '100%', minHeight: height }}>
        <Scatter
          data={data}
          axisXTitle={axisXTitle}
          axisYTitle={axisYTitle}
          containerStyle={{ height, width: '100%' }}
          theme={theme}
          style={style}
        />
      </div>
    );
  } catch {
    return <div style={{ padding: 20, color: '#ff4d4f' }}>图表数据格式不正确，无法解析</div>;
  }
};

export default ScatterChart;
