import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import AgentMonitorModelDetailPage from './model-detail';

/** mock 路由跳转方法。 */
const mockNavigate = jest.fn();

/** mock 当前页面查询参数。 */
let mockSearch =
  '?provider=aliyun&modelName=qwen3.5-plus&startTime=2026-04-30%2008%3A00%3A00&endTime=2026-04-30%2009%3A00%3A00&rangeMinutes=1440&bucketMinutes=60';

/** mock 单模型详情接口。 */
const mockGetModelDetail = jest.fn();

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
    <div data-testid="model-detail-chart">{option.series?.map((item) => item.name).join(',')}</div>
  ),
}));

jest.mock('@/contexts/ThemeContext', () => ({
  useThemeContext: () => ({ isDark: false }),
}));

jest.mock('@/pages/llm-manage/agent-observability/shared', () => ({
  useAgentObservabilitySecondaryMenu: jest.fn(),
}));

jest.mock('@/api/agent/traceMonitor', () => ({
  getAgentTraceMonitorModelDetail: (...args: unknown[]) => mockGetModelDetail(...args),
}));

describe('AgentMonitorModelDetailPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockSearch =
      '?provider=aliyun&modelName=qwen3.5-plus&startTime=2026-04-30%2008%3A00%3A00&endTime=2026-04-30%2009%3A00%3A00&rangeMinutes=1440&bucketMinutes=60';
    mockGetModelDetail.mockResolvedValue({
      provider: 'aliyun',
      modelName: 'qwen3.5-plus',
      summary: {
        callCount: 6,
        successCount: 5,
        errorCount: 1,
        successRate: 83.33,
        errorRate: 16.67,
        inputTokens: 2000,
        outputTokens: 100,
        totalTokens: 2100,
        cacheReadTokens: 300,
        cacheWriteTokens: 120,
        cacheTotalTokens: 420,
        avgDurationMs: 1500,
        maxDurationMs: 3000,
      },
      timeline: [
        {
          bucketStart: '2026-04-30 08:00:00',
          callCount: 6,
          successCount: 5,
          errorCount: 1,
          avgDurationMs: 1500,
          maxDurationMs: 3000,
          inputTokens: 2000,
          outputTokens: 100,
          totalTokens: 2100,
          cacheReadTokens: 300,
          cacheWriteTokens: 120,
          cacheTotalTokens: 420,
        },
      ],
    });
  });

  it('loads model detail with provider and model name from query', async () => {
    render(<AgentMonitorModelDetailPage />);

    expect(await screen.findByText('qwen3.5-plus')).toBeTruthy();
    await waitFor(() =>
      expect(mockGetModelDetail).toHaveBeenCalledWith(
        expect.objectContaining({
          provider: 'aliyun',
          modelName: 'qwen3.5-plus',
          bucketMinutes: 60,
        }),
      ),
    );
  });

  it('reloads model detail after changing time range', async () => {
    render(<AgentMonitorModelDetailPage />);
    await screen.findByText('qwen3.5-plus');

    fireEvent.mouseDown(screen.getByRole('combobox'));
    fireEvent.click(await screen.findByText('最近 5 分钟'));

    await waitFor(() => expect(mockGetModelDetail).toHaveBeenCalledTimes(2));
    expect(mockGetModelDetail.mock.calls[1][0]).toEqual(
      expect.objectContaining({
        provider: 'aliyun',
        modelName: 'qwen3.5-plus',
        bucketMinutes: 5,
      }),
    );
  });

  it('returns dashboard with current time params', async () => {
    render(<AgentMonitorModelDetailPage />);
    await screen.findByText('qwen3.5-plus');

    fireEvent.click(screen.getByText('返回监控面板'));

    expect(mockNavigate).toHaveBeenCalledTimes(1);
    const targetPath = mockNavigate.mock.calls[0][0] as string;
    const url = new URL(targetPath, 'http://localhost:8000');

    expect(url.pathname).toBe('/llm-manage/agent-observability/monitor');
    expect(url.searchParams.get('provider')).toBeNull();
    expect(url.searchParams.get('modelName')).toBeNull();
    expect(url.searchParams.get('rangeMinutes')).toBe('1440');
    expect(url.searchParams.get('bucketMinutes')).toBe('60');
    expect(url.searchParams.get('startTime')).toBeTruthy();
    expect(url.searchParams.get('endTime')).toBeTruthy();
  });
});
