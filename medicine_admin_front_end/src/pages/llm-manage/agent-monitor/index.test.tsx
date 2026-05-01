import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import AgentMonitorPage from './index';

/** mock 路由跳转方法。 */
const mockNavigate = jest.fn();

/** mock 当前页面查询参数。 */
let mockSearch = '';

/** mock 监控概览接口。 */
const mockGetSummary = jest.fn();

/** mock 监控图表接口。 */
const mockGetCharts = jest.fn();

/** mock 模型排行接口。 */
const mockGetModelRanking = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useLocation: () => ({ search: mockSearch }),
}));

jest.mock('@ant-design/pro-components', () => ({
  PageContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

jest.mock('echarts-for-react', () => ({
  __esModule: true,
  default: ({ option }: { option: { series?: Array<{ name?: string }> } }) => (
    <div data-testid="monitor-chart">{option.series?.map((item) => item.name).join(',')}</div>
  ),
}));

jest.mock('@/contexts/ThemeContext', () => ({
  useThemeContext: () => ({ isDark: false }),
}));

jest.mock('@/pages/llm-manage/agent-observability/shared', () => ({
  useAgentObservabilitySecondaryMenu: jest.fn(),
}));

jest.mock('@/api/agent/traceMonitor', () => ({
  getAgentTraceMonitorSummary: (...args: unknown[]) => mockGetSummary(...args),
  getAgentTraceMonitorCharts: (...args: unknown[]) => mockGetCharts(...args),
  getAgentTraceMonitorModelRanking: (...args: unknown[]) => mockGetModelRanking(...args),
}));

describe('AgentMonitorPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockSearch = '';

    mockGetSummary.mockResolvedValue({
      callCount: 41,
      successCount: 41,
      errorCount: 0,
      successRate: 100,
      errorRate: 0,
      inputTokens: 143699,
      outputTokens: 9620,
      totalTokens: 153319,
      cacheReadTokens: 7516,
      cacheWriteTokens: 3220,
      cacheTotalTokens: 10736,
    });
    mockGetCharts.mockResolvedValue({
      callTrend: [
        {
          bucketStart: '2026-04-30 10:00:00',
          callCount: 3,
          successCount: 2,
          errorCount: 1,
        },
      ],
      durationTrend: [
        {
          bucketStart: '2026-04-30 10:00:00',
          avgDurationMs: 1200,
          maxDurationMs: 3000,
        },
      ],
      tokenCacheTrend: [
        {
          bucketStart: '2026-04-30 10:00:00',
          inputTokens: 1000,
          outputTokens: 200,
          totalTokens: 1200,
          cacheReadTokens: 100,
          cacheWriteTokens: 50,
          cacheTotalTokens: 150,
        },
      ],
    });
    mockGetModelRanking.mockResolvedValue([
      {
        provider: 'aliyun',
        modelName: 'qwen3.5-plus',
        callCount: 12,
        totalTokens: 3456,
        errorRate: 0,
        avgDurationMs: 2293,
      },
    ]);
  });

  it('loads dashboard data and renders model ranking before charts', async () => {
    render(<AgentMonitorPage />);

    expect(await screen.findByText('qwen3.5-plus')).toBeTruthy();
    await waitFor(() => expect(mockGetSummary).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockGetCharts).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockGetModelRanking).toHaveBeenCalledTimes(1));

    const rankingTitle = screen.getByText('模型排行');
    const callTitle = screen.getAllByText('调用次数')[1];
    expect(rankingTitle.compareDocumentPosition(callTitle) & Node.DOCUMENT_POSITION_FOLLOWING).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    );
  });

  it('navigates to model detail with current time range params', async () => {
    render(<AgentMonitorPage />);

    fireEvent.click(await screen.findByText('qwen3.5-plus'));

    expect(mockNavigate).toHaveBeenCalledTimes(1);
    const targetPath = mockNavigate.mock.calls[0][0] as string;
    const url = new URL(targetPath, 'http://localhost:8000');

    expect(url.pathname).toBe('/llm-manage/agent-observability/monitor/model-detail');
    expect(url.searchParams.get('provider')).toBe('aliyun');
    expect(url.searchParams.get('modelName')).toBe('qwen3.5-plus');
    expect(url.searchParams.get('rangeMinutes')).toBe('1440');
    expect(url.searchParams.get('bucketMinutes')).toBe('60');
    expect(url.searchParams.get('startTime')).toBeTruthy();
    expect(url.searchParams.get('endTime')).toBeTruthy();
  });

  it('reloads dashboard after changing range select', async () => {
    render(<AgentMonitorPage />);
    await screen.findByText('qwen3.5-plus');

    fireEvent.mouseDown(screen.getByRole('combobox'));
    fireEvent.click(await screen.findByText('最近 5 分钟'));

    await waitFor(() => expect(mockGetSummary).toHaveBeenCalledTimes(2));
    const latestRequest = mockGetSummary.mock.calls[1][0];
    expect(latestRequest.rangeMinutes).toBeUndefined();
    expect(latestRequest.bucketMinutes).toBe(5);
  });
});
