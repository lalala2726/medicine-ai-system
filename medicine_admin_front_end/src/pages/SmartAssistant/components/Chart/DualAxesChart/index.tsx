import { DualAxes } from '@antv/gpt-vis';
import React from 'react';
import { useChartTheme } from '../../../_utils';
import ChartLoader from '../ChartLoader';
import { parseChartJSON } from '../parseChartJSON';

interface DualAxesChartProps {
  children?: React.ReactNode;
  className?: string;
}

/**
 * 自定义双轴图组件
 * 用于在 Markdown 中渲染 dualaxes 代码块
 * 用于展示两个不同量纲的数据关系
 */
const DualAxesChart: React.FC<DualAxesChartProps> = ({ children }) => {
  const content = String(children).trim();
  const defaultTheme = useChartTheme();

  try {
    const config = parseChartJSON(content);
    if (!config) return <ChartLoader height={400} />;
    const {
      categories,
      series,
      theme = defaultTheme,
      backgroundColor = 'transparent',
      palette = [],
      height = 400,
    } = config;

    // 确保数据存在
    if (!categories || !Array.isArray(categories) || !series || !Array.isArray(series)) {
      return (
        <div style={{ padding: 20, color: '#ff4d4f' }}>
          错误:双轴图数据格式不正确,需要提供 categories 和 series 数组
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
        <DualAxes
          categories={categories}
          series={series}
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

export default DualAxesChart;
