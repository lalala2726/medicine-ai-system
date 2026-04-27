/**
 * Markdown 渲染组件
 *
 * 基于 @ant-design/x-markdown，支持流式输出动画和自定义图表代码块。
 * 当代码块语言标识为图表类型（如 line、pie、bar 等）时，
 * 自动渲染为对应的可视化组件。
 */
import { Welcome } from '@ant-design/x';
import XMarkdown from '@ant-design/x-markdown';
import { Skeleton } from 'antd';
import React from 'react';
import { useMarkdownTheme } from '../../_utils';
import AreaChart from '../Chart/AreaChart';
import BarChart from '../Chart/BarChart';
import ChartContainer from '../Chart/ChartContainer';
import ColumnChart from '../Chart/ColumnChart';
import DualAxesChart from '../Chart/DualAxesChart';
import FishboneDiagramChart from '../Chart/FishboneDiagramChart';
import FlowDiagramChart from '../Chart/FlowDiagramChart';
import FunnelChart from '../Chart/FunnelChart';
import HistogramChart from '../Chart/HistogramChart';
import IndentedTreeChart from '../Chart/IndentedTreeChart';
import LineChart from '../Chart/LineChart';
import MindMapChart from '../Chart/MindMapChart';
import NetworkGraphChart from '../Chart/NetworkGraphChart';
import OrganizationChartComponent from '../Chart/OrganizationChartComponent';
import PieChart from '../Chart/PieChart';
import RadarChart from '../Chart/RadarChart';
import ScatterChart from '../Chart/ScatterChart';
import TreemapChart from '../Chart/TreemapChart';
import WordCloudChart from '../Chart/WordCloudChart';
import '@ant-design/x-markdown/themes/light.css';
import '@ant-design/x-markdown/themes/dark.css';

// ======================== 图表映射 ========================

/**
 * 代码块语言标识 → 图表组件的映射表
 * 新增图表类型时只需在此处添加一行即可。
 */
const CHART_COMPONENT_MAP: Record<string, React.FC<{ children?: React.ReactNode }>> = {
  line: LineChart,
  column: ColumnChart,
  pie: PieChart,
  area: AreaChart,
  bar: BarChart,
  histogram: HistogramChart,
  scatter: ScatterChart,
  wordcloud: WordCloudChart,
  treemap: TreemapChart,
  dualaxes: DualAxesChart,
  radar: RadarChart,
  funnel: FunnelChart,
  mindmap: MindMapChart,
  networkgraph: NetworkGraphChart,
  flowdiagram: FlowDiagramChart,
  organizationchart: OrganizationChartComponent,
  indentedtree: IndentedTreeChart,
  fishbonediagram: FishboneDiagramChart,
};

// ======================== 自定义代码块 ========================

/**
 * 代码块渲染组件
 * 当 language 命中 CHART_COMPONENT_MAP 时渲染对应图表，否则渲染默认 <code>
 */
const CodeComponent: React.FC<any> = ({
  children,
  className,
  domNode: _domNode,
  streamStatus: _streamStatus,
  block: _block,
  ...props
}) => {
  const language = className?.replace('language-', '');
  const ChartComp = language ? CHART_COMPONENT_MAP[language] : undefined;

  if (ChartComp) {
    // 提取纯文本内容：children 在不同渲染时机可能是 string 或 ReactNode[]
    const rawContent = Array.isArray(children)
      ? children.map((child: any) => (typeof child === 'string' ? child : '')).join('')
      : typeof children === 'string'
        ? children
        : String(children ?? '');

    return (
      <div
        data-chart="true"
        style={{ width: '100%', minWidth: 0, display: 'block', overflow: 'visible' }}
      >
        <ChartContainer>
          <ChartComp>{rawContent}</ChartComp>
        </ChartContainer>
      </div>
    );
  }

  return (
    <code className={className} {...props}>
      {children}
    </code>
  );
};

// ======================== 流式加载占位 ========================

/** 流式输出中尚未完成的 Markdown 元素占位组件 */
const LoadingComponents = {
  'loading-link': () => (
    <Skeleton.Button active size="small" style={{ margin: '4px 0', width: 16, height: 16 }} />
  ),
  'loading-image': () => <Skeleton.Image active style={{ width: 60, height: 60 }} />,
  'loading-table': () => <Skeleton.Node active style={{ width: 160 }} />,
  'loading-html': () => <Skeleton.Node active style={{ width: 383, height: 120 }} />,
};

// ======================== 欢迎卡片 ========================

/** 自定义 <welcome> 标签对应的渲染组件 */
const WelcomeCard = (props: Record<string, any>) => (
  <Welcome
    styles={{ icon: { flexShrink: 0 } }}
    icon={props['data-icon']}
    title={props.title}
    description={props['data-description']}
  />
);

// ======================== 主组件 ========================

interface MarkdownRenderProps {
  /** Markdown 文本内容 */
  content: string;
  /** 是否启用流式渲染（默认 true） */
  enableStreaming?: boolean;
  /** 是否启用打字动画（默认 true） */
  enableAnimation?: boolean;
}

const MarkdownRender: React.FC<MarkdownRenderProps> = ({
  content,
  enableStreaming = true,
  enableAnimation: _enableAnimation = true,
}) => {
  const [className] = useMarkdownTheme();

  /** 合并所有自定义组件（图表 + 占位 + 欢迎卡片） */
  const components = React.useMemo(
    () => ({
      code: CodeComponent,
      ...LoadingComponents,
      welcome: WelcomeCard,
    }),
    [],
  );

  return (
    <XMarkdown
      className={className}
      content={content}
      paragraphTag="div"
      openLinksInNewTab
      dompurifyConfig={{ ADD_ATTR: ['icon', 'description'] }}
      streaming={{
        hasNextChunk: enableStreaming,
        enableAnimation: true,
        animationConfig: { fadeDuration: 400 },
        incompleteMarkdownComponentMap: {
          link: 'loading-link',
          image: 'loading-image',
          table: 'loading-table',
          html: 'loading-html',
        },
      }}
      components={components}
    />
  );
};

export default MarkdownRender;
