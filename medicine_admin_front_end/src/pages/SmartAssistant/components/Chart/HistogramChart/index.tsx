import { Histogram } from '@antv/gpt-vis';
import React from 'react';
import { useChartTheme } from '../../../_utils';
import ChartLoader from '../ChartLoader';
import { parseChartJSON } from '../parseChartJSON';

interface HistogramChartProps {
  children?: React.ReactNode;
  className?: string;
}

/**
 * 自定义直方图组件
 * 用于在 Markdown 中渲染 histogram 代码块
 * 用于展示数据分布情况
 */
const HistogramChart: React.FC<HistogramChartProps> = ({ children }) => {
  const content = String(children).trim();
  const defaultTheme = useChartTheme();

  try {
    const config = parseChartJSON(content);
    if (!config) return <ChartLoader height={400} />;
    const {
      data,
      title = '数据分布直方图',
      binNumber = 10,
      axisXTitle = '区间',
      axisYTitle = '频数',
      theme = defaultTheme,
      backgroundColor = 'transparent',
      palette = [],
      height = 400,
    } = config;

    // 确保数据存在
    if (!data || !Array.isArray(data)) {
      return (
        <div style={{ padding: 20, color: '#ff4d4f' }}>
          错误:直方图数据格式不正确,需要提供 data 数组
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
        <Histogram
          title={title}
          data={data}
          binNumber={binNumber}
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

export default HistogramChart;
