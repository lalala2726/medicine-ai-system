import React from 'react';
import { act, render, screen, waitFor, within } from '@testing-library/react';

import type { SystemModelTypes } from '@/api/llm-manage/systemModels';

import SystemModels from './index';

const mockNavigate = jest.fn();
const mockGetKnowledgeBaseConfig = jest.fn();
const mockGetClientKnowledgeBaseConfig = jest.fn();
const mockGetAdminAssistantConfig = jest.fn();
const mockGetClientAssistantConfig = jest.fn();
const mockGetCommonCapabilityConfig = jest.fn();
const mockGetSpeechConfig = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('@/api/llm-manage/systemModels', () => ({
  getKnowledgeBaseConfig: (...args: any[]) => mockGetKnowledgeBaseConfig(...args),
  getClientKnowledgeBaseConfig: (...args: any[]) => mockGetClientKnowledgeBaseConfig(...args),
  getAdminAssistantConfig: (...args: any[]) => mockGetAdminAssistantConfig(...args),
  getClientAssistantConfig: (...args: any[]) => mockGetClientAssistantConfig(...args),
  getCommonCapabilityConfig: (...args: any[]) => mockGetCommonCapabilityConfig(...args),
  getSpeechConfig: (...args: any[]) => mockGetSpeechConfig(...args),
}));

jest.mock('@ant-design/pro-components', () => ({
  PageContainer: ({ children }: any) => <div>{children}</div>,
}));

jest.mock('./components/KnowledgeBaseConfigDrawer', () => () => null);
jest.mock('./components/ClientKnowledgeBaseConfigDrawer', () => () => null);
jest.mock('./components/AdminAssistantConfigDrawer', () => () => null);
jest.mock('./components/ClientAssistantConfigDrawer', () => () => null);
jest.mock('./components/SpeechConfigDrawer/index', () => () => null);
jest.mock('./components/CommonCapabilityConfigDrawer', () => () => null);

/** 默认客户端助手配置。 */
const defaultClientAssistantConfig: SystemModelTypes.ClientAssistantSystemModelConfig = {
  routeModel: { modelName: 'gpt-4.1-mini' },
  serviceNodeModel: { modelName: 'gpt-4.1' },
  diagnosisNodeModel: { modelName: 'gpt-4.1' },
};

/**
 * 初始化页面请求 mock。
 *
 * @param clientAssistantConfig 客户端助手配置。
 */
function setupApiMocks(clientAssistantConfig = defaultClientAssistantConfig) {
  mockGetKnowledgeBaseConfig.mockResolvedValue({
    enabled: true,
    embeddingModel: { modelName: 'text-embedding-3-large' },
  });
  mockGetClientKnowledgeBaseConfig.mockResolvedValue({
    enabled: true,
    embeddingModel: { modelName: 'text-embedding-3-large' },
  });
  mockGetAdminAssistantConfig.mockResolvedValue({
    adminNodeModel: { modelName: 'gpt-4.1' },
  });
  mockGetClientAssistantConfig.mockResolvedValue(clientAssistantConfig);
  mockGetCommonCapabilityConfig.mockResolvedValue({
    imageRecognitionModel: { modelName: 'qwen-vl-max' },
    chatHistorySummaryModel: { modelName: 'gpt-4.1-mini' },
    chatTitleModel: { modelName: 'gpt-4.1' },
  });
  mockGetSpeechConfig.mockResolvedValue({
    appId: 'speech-app-id',
    textToSpeech: { voiceType: 'zh_female_xiaohe_uranus_bigtts', maxTextChars: 300 },
  });
}

/**
 * 获取指定标题对应的概览卡片。
 *
 * @param title 卡片标题。
 * @returns 卡片根节点。
 */
function getOverviewCard(title: string) {
  const cardTitle = screen.getByText(title);
  const card = cardTitle.closest('.ant-card');
  if (!card) {
    throw new Error(`Unable to locate overview card for title: ${title}`);
  }
  return card as HTMLElement;
}

describe('SystemModels client assistant overview', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows configured status when all client assistant slots are ready', async () => {
    setupApiMocks();

    await act(async () => {
      render(<SystemModels />);
      await Promise.resolve();
    });

    await waitFor(() => expect(mockGetClientAssistantConfig).toHaveBeenCalledTimes(1));
    const overviewCard = getOverviewCard('客户端助手');

    expect(within(overviewCard).getByText('已配置')).toBeTruthy();
    expect(within(overviewCard).getByText('通用节点 2/2')).toBeTruthy();
    expect(within(overviewCard).getByText('2/2')).toBeTruthy();
    expect(within(overviewCard).getByText('诊断节点 1/1')).toBeTruthy();
    expect(within(overviewCard).getByText('1/1')).toBeTruthy();
    expect(within(overviewCard).getByText('路由模型')).toBeTruthy();
    expect(within(overviewCard).getByText('服务模型')).toBeTruthy();
    expect(within(overviewCard).getByText('gpt-4.1-mini')).toBeTruthy();
    expect(within(overviewCard).getAllByText('gpt-4.1').length).toBeGreaterThan(0);
  });

  it('shows pending status when part of client assistant slots are missing', async () => {
    setupApiMocks({
      ...defaultClientAssistantConfig,
      serviceNodeModel: { modelName: undefined },
      diagnosisNodeModel: { modelName: undefined },
    });

    await act(async () => {
      render(<SystemModels />);
      await Promise.resolve();
    });

    await waitFor(() => expect(mockGetClientAssistantConfig).toHaveBeenCalledTimes(1));
    const overviewCard = getOverviewCard('客户端助手');

    expect(within(overviewCard).getByText('待完善')).toBeTruthy();
    expect(within(overviewCard).getByText('1/2')).toBeTruthy();
    expect(within(overviewCard).getByText('0/1')).toBeTruthy();
  });
});
