import { Line } from '@antv/gpt-vis';
import React from 'react';
import { useChartTheme } from '../../../_utils';
import ChartLoader from '../ChartLoader';
import { parseChartJSON } from '../parseChartJSON';

interface LineChartProps {
  children?: React.ReactNode;
  className?: string;
}

/**
 * 自定义折线图组件
 * 用于在 Markdown 中渲染 line 代码块
 * 支持单条线和多条线（通过 group 字段区分）
 */
const LineChart: React.FC<LineChartProps> = ({ children, className }) => {
  const content = String(children).trim();
  const defaultTheme = useChartTheme();

  try {
    const config = parseChartJSON(content);
    if (!config) return <ChartLoader height={500} />;
    const {
      data,
      axisXTitle = 'X轴',
      axisYTitle = 'Y轴',
      theme = defaultTheme,
      lineWidth = 2,
      backgroundColor = 'transparent',
      palette = [],
      height = 500,
    } = config;

    // 确保数据存在
    if (!data || !Array.isArray(data)) {
      return (
        <div style={{ padding: 20, color: '#ff4d4f' }}>
          错误:折线图数据格式不正确,需要提供 data 数组
        </div>
      );
    }

    // 构建样式对象
    const style: any = {
      lineWidth,
    };

    if (backgroundColor) {
      style.backgroundColor = backgroundColor;
    }

    if (palette && palette.length > 0) {
      style.palette = palette;
    }

    // 检查是否是多条线（数据中包含 group 字段）
    const isMultiLine = data.some((item: any) => 'group' in item || 'type' in item);

    return (
      <div className={className} style={{ width: '100%', minHeight: height, display: 'block' }}>
        <Line
          data={data}
          axisXTitle={axisXTitle}
          axisYTitle={axisYTitle}
          containerStyle={{ height, width: '100%' }}
          theme={theme}
          style={style}
          // 如果是多条线，启用分组
          {...(isMultiLine && {
            colorField: data[0]?.group ? 'group' : 'type',
            legend: { position: 'top-right' },
          })}
        />
      </div>
    );
  } catch {
    return <div style={{ padding: 20, color: '#ff4d4f' }}>图表数据格式不正确，无法解析</div>;
  }
};

export default LineChart;
