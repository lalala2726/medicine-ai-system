import { WordCloud } from '@antv/gpt-vis';
import React from 'react';
import { useChartTheme } from '../../../_utils';
import ChartLoader from '../ChartLoader';
import { parseChartJSON } from '../parseChartJSON';

interface WordCloudChartProps {
  children?: React.ReactNode;
  className?: string;
}

/**
 * 自定义词云图组件
 * 用于在 Markdown 中渲染 wordcloud 代码块
 * 用于展示文本中词语的频率和重要性
 */
const WordCloudChart: React.FC<WordCloudChartProps> = ({ children }) => {
  const content = String(children).trim();
  const defaultTheme = useChartTheme();

  try {
    const config = parseChartJSON(content);
    if (!config) return <ChartLoader height={400} />;
    const {
      data,
      theme = defaultTheme,
      backgroundColor = 'transparent',
      palette = [],
      height = 400,
    } = config;

    // 确保数据存在
    if (!data || !Array.isArray(data)) {
      return (
        <div style={{ padding: 20, color: '#ff4d4f' }}>
          错误:词云图数据格式不正确,需要提供 data 数组
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
        <WordCloud
          data={data}
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

export default WordCloudChart;
