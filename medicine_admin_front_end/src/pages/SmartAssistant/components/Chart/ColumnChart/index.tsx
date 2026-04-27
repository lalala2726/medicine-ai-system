import { Bar, Column } from '@antv/gpt-vis';
import React from 'react';
import { useChartTheme } from '../../../_utils';
import ChartLoader from '../ChartLoader';
import { parseChartJSON } from '../parseChartJSON';

interface ColumnChartProps {
  children?: React.ReactNode;
  className?: string;
}

/**
 * 自定义柱状图组件
 * 用于在 Markdown 中渲染 column 代码块
 * 支持单组和多组数据（通过 group 字段区分）
 * 支持横向(horizontal)和纵向(vertical)展示,默认纵向
 */
const ColumnChart: React.FC<ColumnChartProps> = ({ children, className }) => {
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
      backgroundColor = 'transparent',
      palette = [],
      height = 500,
      radiusTopLeft = 4,
      radiusTopRight = 4,
      direction = 'vertical', // vertical: 纵向(默认), horizontal: 横向
    } = config;

    // 确保数据存在
    if (!data || !Array.isArray(data)) {
      return (
        <div style={{ padding: 20, color: '#ff4d4f' }}>
          错误:柱状图数据格式不正确,需要提供 data 数组
        </div>
      );
    }

    // 构建样式对象
    const style: any = {
      radiusTopLeft,
      radiusTopRight,
    };

    if (backgroundColor) {
      style.backgroundColor = backgroundColor;
    }

    if (palette && palette.length > 0) {
      style.palette = palette;
    }

    // 检查是否是多组数据（数据中包含 group 字段）
    const isGrouped = data.some((item: any) => 'group' in item);

    // 根据 direction 选择组件
    const ChartComponent = direction === 'horizontal' ? Bar : Column;

    return (
      <div className={className} style={{ width: '100%', minHeight: height, display: 'block' }}>
        <ChartComponent
          data={data}
          axisXTitle={axisXTitle}
          axisYTitle={axisYTitle}
          containerStyle={{ height, width: '100%' }}
          theme={theme}
          style={style}
          // 如果是多组数据，启用分组
          {...(isGrouped && {
            group: true,
            legend: { position: 'top-right' },
          })}
        />
      </div>
    );
  } catch {
    return <div style={{ padding: 20, color: '#ff4d4f' }}>图表数据格式不正确，无法解析</div>;
  }
};

export default ColumnChart;
