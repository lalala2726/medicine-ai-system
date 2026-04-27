import React from 'react';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { message } from 'antd';

import SystemModels from './index';

jest.setTimeout(15000);

const mockNavigate = jest.fn();
const mockGetKnowledgeBaseConfig = jest.fn();
const mockSaveKnowledgeBaseConfig = jest.fn();
const mockGetKnowledgeBaseOptions = jest.fn();
const mockGetAdminAssistantConfig = jest.fn();
const mockSaveAdminAssistantConfig = jest.fn();
const mockGetClientAssistantConfig = jest.fn();
const mockSaveClientAssistantConfig = jest.fn();
const mockGetImageRecognitionConfig = jest.fn();
const mockSaveImageRecognitionConfig = jest.fn();
const mockGetSpeechConfig = jest.fn();
const mockSaveSpeechConfig = jest.fn();
const mockGetChatHistorySummaryConfig = jest.fn();
const mockSaveChatHistorySummaryConfig = jest.fn();
const mockGetChatTitleConfig = jest.fn();
const mockSaveChatTitleConfig = jest.fn();
const mockGetEmbeddingModelOptions = jest.fn();
const mockGetChatModelOptions = jest.fn();
const mockGetVisionModelOptions = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('@/api/llm-manage/systemModels', () => ({
  getKnowledgeBaseConfig: (...args: any[]) => mockGetKnowledgeBaseConfig(...args),
  saveKnowledgeBaseConfig: (...args: any[]) => mockSaveKnowledgeBaseConfig(...args),
  getKnowledgeBaseOptions: (...args: any[]) => mockGetKnowledgeBaseOptions(...args),
  getAdminAssistantConfig: (...args: any[]) => mockGetAdminAssistantConfig(...args),
  saveAdminAssistantConfig: (...args: any[]) => mockSaveAdminAssistantConfig(...args),
  getClientAssistantConfig: (...args: any[]) => mockGetClientAssistantConfig(...args),
  saveClientAssistantConfig: (...args: any[]) => mockSaveClientAssistantConfig(...args),
  getImageRecognitionConfig: (...args: any[]) => mockGetImageRecognitionConfig(...args),
  saveImageRecognitionConfig: (...args: any[]) => mockSaveImageRecognitionConfig(...args),
  getSpeechConfig: (...args: any[]) => mockGetSpeechConfig(...args),
  saveSpeechConfig: (...args: any[]) => mockSaveSpeechConfig(...args),
  getChatHistorySummaryConfig: (...args: any[]) => mockGetChatHistorySummaryConfig(...args),
  saveChatHistorySummaryConfig: (...args: any[]) => mockSaveChatHistorySummaryConfig(...args),
  getChatTitleConfig: (...args: any[]) => mockGetChatTitleConfig(...args),
  saveChatTitleConfig: (...args: any[]) => mockSaveChatTitleConfig(...args),
  getEmbeddingModelOptions: (...args: any[]) => mockGetEmbeddingModelOptions(...args),
  getChatModelOptions: (...args: any[]) => mockGetChatModelOptions(...args),
  getVisionModelOptions: (...args: any[]) => mockGetVisionModelOptions(...args),
}));

jest.mock('@ant-design/pro-components', () => ({
  PageContainer: ({ children }: any) => <div>{children}</div>,
}));

const defaultKnowledgeBaseConfig = {
  enabled: true,
  knowledgeNames: ['common_medicine_kb', 'otc_guide_kb'],
  embeddingDim: 1024,
  topK: 10,
  embeddingModel: {
    modelName: 'text-embedding-3-large',
    reasoningEnabled: false,
    supportReasoning: false,
    supportVision: false,
  },
  rankingEnabled: true,
  rankingModel: {
    modelName: 'gpt-4.1-mini',
    reasoningEnabled: false,
    supportReasoning: false,
    supportVision: false,
  },
};

const defaultAdminAssistantConfig = {
  routeModel: {
    modelName: 'gpt-4.1-mini',
    reasoningEnabled: false,
    supportReasoning: false,
    supportVision: false,
  },
  businessNodeSimpleModel: {
    modelName: 'gpt-4.1-mini',
    reasoningEnabled: false,
    supportReasoning: false,
    supportVision: false,
  },
  businessNodeComplexModel: {
    modelName: 'gpt-4.1',
    reasoningEnabled: true,
    supportReasoning: true,
    supportVision: false,
  },
  chatModel: {
    modelName: 'gpt-4.1',
    reasoningEnabled: true,
    supportReasoning: true,
    supportVision: true,
  },
};

const defaultClientAssistantConfig = {
  routeModel: {
    modelName: 'gpt-4.1-mini',
    reasoningEnabled: false,
    supportReasoning: false,
    supportVision: false,
  },
  chatModel: {
    modelName: 'gpt-4.1',
    reasoningEnabled: true,
    supportReasoning: true,
    supportVision: true,
  },
  orderModel: {
    modelName: 'gpt-4.1-mini',
    reasoningEnabled: false,
    supportReasoning: false,
    supportVision: false,
  },
  productModel: {
    modelName: 'gpt-4.1-mini',
    reasoningEnabled: false,
    supportReasoning: false,
    supportVision: false,
  },
  afterSaleModel: {
    modelName: 'gpt-4.1-mini',
    reasoningEnabled: false,
    supportReasoning: false,
    supportVision: false,
  },
  consultationComfortModel: {
    modelName: 'gpt-4.1-mini',
    reasoningEnabled: false,
    supportReasoning: false,
    supportVision: false,
  },
  consultationQuestionModel: {
    modelName: 'gpt-4.1',
    reasoningEnabled: true,
    supportReasoning: true,
    supportVision: true,
  },
  consultationFinalDiagnosisModel: {
    modelName: 'gpt-4.1',
    reasoningEnabled: true,
    supportReasoning: true,
    supportVision: true,
  },
};

const defaultImageRecognitionConfig = {
  imageRecognitionModel: {
    modelName: 'qwen-vl-max',
    reasoningEnabled: true,
    supportReasoning: true,
    supportVision: true,
  },
};

const defaultSpeechConfig = {
  appId: 'speech-app-id',
  accessToken: null,
  textToSpeech: {
    voiceType: 'zh_female_xiaohe_uranus_bigtts',
    maxTextChars: 300,
  },
};

const defaultChatHistorySummaryConfig = {
  chatHistorySummaryModel: {
    modelName: 'gpt-4.1-mini',
    reasoningEnabled: false,
    supportReasoning: false,
    supportVision: false,
  },
};

const defaultChatTitleConfig = {
  chatTitleModel: {
    modelName: 'gpt-4.1',
    reasoningEnabled: true,
    supportReasoning: true,
    supportVision: true,
  },
};

const defaultOptions = {
  knowledgeBase: [
    {
      knowledgeName: 'common_medicine_kb',
      displayName: '常见用药知识库',
      embeddingModel: 'text-embedding-3-large',
      embeddingDim: 1024,
    },
    {
      knowledgeName: 'otc_guide_kb',
      displayName: 'OTC 指南知识库',
      embeddingModel: 'text-embedding-3-large',
      embeddingDim: 1024,
    },
    {
      knowledgeName: 'mismatch_kb',
      displayName: '不兼容知识库',
      embeddingModel: 'text-embedding-3-small',
      embeddingDim: 768,
    },
  ],
  embedding: [
    {
      label: 'text-embedding-3-large',
      value: 'text-embedding-3-large',
      supportReasoning: false,
      supportVision: false,
    },
  ],
  chat: [
    { label: 'gpt-4.1-mini', value: 'gpt-4.1-mini', supportReasoning: false, supportVision: false },
    { label: 'gpt-4.1', value: 'gpt-4.1', supportReasoning: true, supportVision: true },
  ],
  vision: [
    { label: 'qwen-vl-max', value: 'qwen-vl-max', supportReasoning: true, supportVision: true },
  ],
};

function setupApiMocks(overrides?: {
  knowledgeBaseConfig?: any;
  adminAssistantConfig?: any;
  clientAssistantConfig?: any;
  speechConfig?: any;
  imageRecognitionConfig?: any;
  chatHistorySummaryConfig?: any;
  chatTitleConfig?: any;
  knowledgeBaseOptions?: any[];
  embeddingOptions?: any[];
  chatOptions?: any[];
  visionOptions?: any[];
}) {
  mockGetKnowledgeBaseConfig.mockResolvedValue(
    overrides?.knowledgeBaseConfig ?? defaultKnowledgeBaseConfig,
  );
  mockGetKnowledgeBaseOptions.mockResolvedValue(
    overrides?.knowledgeBaseOptions ?? defaultOptions.knowledgeBase,
  );
  mockGetAdminAssistantConfig.mockResolvedValue(
    overrides?.adminAssistantConfig ?? defaultAdminAssistantConfig,
  );
  mockGetClientAssistantConfig.mockResolvedValue(
    overrides?.clientAssistantConfig ?? defaultClientAssistantConfig,
  );
  mockGetSpeechConfig.mockResolvedValue(overrides?.speechConfig ?? defaultSpeechConfig);
  mockGetImageRecognitionConfig.mockResolvedValue(
    overrides?.imageRecognitionConfig ?? defaultImageRecognitionConfig,
  );
  mockGetChatHistorySummaryConfig.mockResolvedValue(
    overrides?.chatHistorySummaryConfig ?? defaultChatHistorySummaryConfig,
  );
  mockGetChatTitleConfig.mockResolvedValue(overrides?.chatTitleConfig ?? defaultChatTitleConfig);
  mockGetEmbeddingModelOptions.mockResolvedValue(
    overrides?.embeddingOptions ?? defaultOptions.embedding,
  );
  mockGetChatModelOptions.mockResolvedValue(overrides?.chatOptions ?? defaultOptions.chat);
  mockGetVisionModelOptions.mockResolvedValue(overrides?.visionOptions ?? defaultOptions.vision);
  mockSaveKnowledgeBaseConfig.mockResolvedValue(undefined);
  mockSaveAdminAssistantConfig.mockResolvedValue(undefined);
  mockSaveClientAssistantConfig.mockResolvedValue(undefined);
  mockSaveImageRecognitionConfig.mockResolvedValue(undefined);
  mockSaveSpeechConfig.mockResolvedValue(undefined);
  mockSaveChatHistorySummaryConfig.mockResolvedValue(undefined);
  mockSaveChatTitleConfig.mockResolvedValue(undefined);
}

describe('SystemModels page', () => {
  const successSpy = jest.spyOn(message, 'success').mockImplementation(jest.fn());
  const errorSpy = jest.spyOn(message, 'error').mockImplementation(jest.fn());
  const originalPlay = window.HTMLMediaElement.prototype.play;
  const originalPause = window.HTMLMediaElement.prototype.pause;
  const playMediaMock = jest.fn().mockResolvedValue(undefined);
  const pauseMediaMock = jest.fn();

  function getDrawerContent(title: string) {
    const drawerTitle = screen.getByText(title);
    const drawerContent = drawerTitle.closest('.ant-drawer-content');
    if (!drawerContent) {
      throw new Error(`Unable to locate drawer content for title: ${title}`);
    }
    return drawerContent as HTMLElement;
  }

  function getOverviewCard(title: string) {
    const cardTitle = screen.getByText(title);
    const card = cardTitle.closest('.ant-card');
    if (!card) {
      throw new Error(`Unable to locate overview card for title: ${title}`);
    }
    return card as HTMLElement;
  }

  function getFormItem(label: string, container?: HTMLElement) {
    const scopedQueries = container ? within(container) : screen;
    const formItem = scopedQueries.getByText(label).closest('.ant-form-item');
    if (!formItem) {
      throw new Error(`Unable to locate form item for label: ${label}`);
    }
    return formItem as HTMLElement;
  }

  function getVisibleSelectDropdown() {
    const dropdowns = Array.from(document.body.querySelectorAll('.ant-select-dropdown')).filter(
      (element) => !element.classList.contains('ant-select-dropdown-hidden'),
    );
    const dropdown = dropdowns.at(-1);
    if (!dropdown) {
      throw new Error('Unable to locate visible select dropdown');
    }
    return dropdown as HTMLElement;
  }

  async function renderPage() {
    await act(async () => {
      render(<SystemModels />);
      await Promise.resolve();
    });
  }

  beforeAll(() => {
    Object.defineProperty(window.HTMLMediaElement.prototype, 'play', {
      configurable: true,
      writable: true,
      value: playMediaMock,
    });
    Object.defineProperty(window.HTMLMediaElement.prototype, 'pause', {
      configurable: true,
      writable: true,
      value: pauseMediaMock,
    });
  });

  beforeEach(() => {
    jest.clearAllMocks();
    setupApiMocks();
  });

  afterAll(() => {
    Object.defineProperty(window.HTMLMediaElement.prototype, 'play', {
      configurable: true,
      writable: true,
      value: originalPlay,
    });
    Object.defineProperty(window.HTMLMediaElement.prototype, 'pause', {
      configurable: true,
      writable: true,
      value: originalPause,
    });
    successSpy.mockRestore();
    errorSpy.mockRestore();
  });

  it('loads seven overview cards on mount', async () => {
    await renderPage();

    await waitFor(() => expect(mockGetKnowledgeBaseConfig).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockGetAdminAssistantConfig).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockGetClientAssistantConfig).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockGetSpeechConfig).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockGetImageRecognitionConfig).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockGetChatHistorySummaryConfig).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockGetChatTitleConfig).toHaveBeenCalledTimes(1));

    expect(await screen.findByText('知识库')).toBeTruthy();
    expect(screen.getByText('管理端助手')).toBeTruthy();
    expect(screen.getByText('客户端助手')).toBeTruthy();
    expect(screen.getByText('豆包语音')).toBeTruthy();
    expect(screen.getByText('图片识别')).toBeTruthy();
    expect(screen.getByText('聊天历史总结')).toBeTruthy();
    expect(screen.getByText('聊天标题生成')).toBeTruthy();
    expect(screen.getByText('text-embedding-3-large')).toBeTruthy();
    expect(screen.getByText('qwen-vl-max')).toBeTruthy();
    expect(screen.getByText('小何 2.0')).toBeTruthy();
    expect(screen.getByText('通用节点 5/5')).toBeTruthy();
    expect(screen.getByText('问诊节点 3/3')).toBeTruthy();
    expect(screen.queryByText('volcengine')).toBeNull();
    expect(screen.queryByText('volc.seedasr.sauc.duration')).toBeNull();
    expect(screen.getAllByText('gpt-4.1-mini').length).toBeGreaterThan(0);
  });

  it('shows disabled status in the knowledge base overview card when the config is turned off', async () => {
    setupApiMocks({
      knowledgeBaseConfig: {
        enabled: false,
        knowledgeNames: ['common_medicine_kb'],
        embeddingDim: 1024,
        topK: 10,
        embeddingModel: {
          modelName: 'text-embedding-3-large',
          reasoningEnabled: false,
          supportReasoning: false,
          supportVision: false,
        },
        rankingEnabled: false,
        rankingModel: null,
      },
    });

    await renderPage();

    const knowledgeBaseCard = getOverviewCard('知识库');
    expect(within(knowledgeBaseCard).getByText('已关闭')).toBeTruthy();
  });

  it('submits ranking disabled payload after turning off ranking switch in knowledge base drawer', async () => {
    await renderPage();

    await screen.findByText('text-embedding-3-large');
    fireEvent.click(screen.getByTestId('open-knowledge-base'));
    await screen.findByText('配置知识库');
    fireEvent.click(screen.getByTestId('knowledge-ranking-switch'));

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() =>
      expect(mockSaveKnowledgeBaseConfig).toHaveBeenCalledWith({
        enabled: true,
        knowledgeNames: ['common_medicine_kb', 'otc_guide_kb'],
        embeddingDim: 1024,
        topK: 10,
        embeddingModel: {
          modelName: 'text-embedding-3-large',
          reasoningEnabled: false,
        },
        rankingEnabled: false,
        rankingModel: null,
      }),
    );
  });

  it('submits disabled payload after turning off the knowledge base switch', async () => {
    await renderPage();

    await screen.findByText('知识库');
    fireEvent.click(screen.getByTestId('open-knowledge-base'));
    await screen.findByText('配置知识库');
    fireEvent.click(screen.getByTestId('knowledge-base-enabled-switch'));

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() =>
      expect(mockSaveKnowledgeBaseConfig).toHaveBeenCalledWith({
        enabled: false,
        knowledgeNames: ['common_medicine_kb', 'otc_guide_kb'],
        embeddingDim: 1024,
        topK: 10,
        embeddingModel: {
          modelName: 'text-embedding-3-large',
          reasoningEnabled: false,
        },
        rankingEnabled: true,
        rankingModel: {
          modelName: 'gpt-4.1-mini',
          reasoningEnabled: false,
        },
      }),
    );
  });

  it('shows the selected ranking model when reopening the knowledge base drawer', async () => {
    await renderPage();

    await screen.findByText('知识库');
    fireEvent.click(screen.getByTestId('open-knowledge-base'));
    await screen.findByText('配置知识库');

    let drawer = getDrawerContent('配置知识库');
    let rankingField = getFormItem('重排模型', drawer);
    expect(rankingField.querySelector('.ant-select-selection-item')?.textContent).toContain(
      'gpt-4.1-mini',
    );

    fireEvent.click(within(drawer).getByRole('button', { name: /取\s*消/ }));
    await waitFor(() => expect(screen.queryByText('配置知识库')).toBeNull());

    fireEvent.click(screen.getByTestId('open-knowledge-base'));
    await screen.findByText('配置知识库');

    drawer = getDrawerContent('配置知识库');
    rankingField = getFormItem('重排模型', drawer);
    expect(rankingField.querySelector('.ant-select-selection-item')?.textContent).toContain(
      'gpt-4.1-mini',
    );
  });

  it('auto fills embedding model and dimension from the first selected knowledge base', async () => {
    setupApiMocks({
      knowledgeBaseConfig: {
        enabled: true,
        knowledgeNames: [],
        embeddingDim: null,
        topK: null,
        embeddingModel: null,
        rankingEnabled: false,
        rankingModel: null,
      },
    });

    await renderPage();

    await screen.findByText('知识库');
    fireEvent.click(screen.getByTestId('open-knowledge-base'));
    await screen.findByText('配置知识库');
    const drawer = getDrawerContent('配置知识库');
    const knowledgeBaseField = getFormItem('知识库名称', drawer);
    const selector = knowledgeBaseField.querySelector('.ant-select-selector');
    if (!selector) {
      throw new Error('Unable to locate knowledge base selector');
    }

    fireEvent.mouseDown(selector);
    fireEvent.click(await screen.findByText('常见用药知识库 (common_medicine_kb)'));

    const embeddingModelInput = getFormItem('向量模型', drawer).querySelector('input');
    const embeddingDimInput = getFormItem('向量维度', drawer).querySelector('input');
    if (!embeddingModelInput || !embeddingDimInput) {
      throw new Error('Unable to locate embedding fields');
    }

    await waitFor(() => {
      expect((embeddingModelInput as HTMLInputElement).value).toBe('text-embedding-3-large');
      expect((embeddingDimInput as HTMLInputElement).value).toBe('1024');
    });
  });

  it('allows saving a disabled knowledge base config without selecting knowledge bases', async () => {
    setupApiMocks({
      knowledgeBaseConfig: {
        enabled: false,
        knowledgeNames: [],
        embeddingDim: null,
        topK: null,
        embeddingModel: null,
        rankingEnabled: false,
        rankingModel: null,
      },
    });

    await renderPage();

    await screen.findByText('知识库');
    fireEvent.click(screen.getByTestId('open-knowledge-base'));
    await screen.findByText('配置知识库');
    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() =>
      expect(mockSaveKnowledgeBaseConfig).toHaveBeenCalledWith({
        enabled: false,
        knowledgeNames: [],
        embeddingDim: null,
        topK: null,
        embeddingModel: null,
        rankingEnabled: false,
        rankingModel: null,
      }),
    );
  });

  it('treats legacy knowledge base configs without an explicit enabled flag as enabled', async () => {
    setupApiMocks({
      knowledgeBaseConfig: {
        knowledgeNames: ['common_medicine_kb'],
        embeddingDim: 1024,
        topK: 10,
        embeddingModel: {
          modelName: 'text-embedding-3-large',
          reasoningEnabled: false,
          supportReasoning: false,
          supportVision: false,
        },
        rankingEnabled: false,
        rankingModel: null,
      },
    });

    await renderPage();

    fireEvent.click(screen.getByTestId('open-knowledge-base'));
    await screen.findByText('配置知识库');

    expect(screen.getByTestId('knowledge-base-enabled-switch').getAttribute('aria-checked')).toBe(
      'true',
    );
  });

  it('submits mapped payload for admin assistant drawer', async () => {
    await renderPage();

    await screen.findByText('管理端助手');
    fireEvent.click(screen.getByTestId('open-admin-assistant'));
    await screen.findByTestId('assistant-route-model');

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() =>
      expect(mockSaveAdminAssistantConfig).toHaveBeenCalledWith({
        routeModel: {
          modelName: 'gpt-4.1-mini',
          reasoningEnabled: false,
        },
        businessNodeSimpleModel: {
          modelName: 'gpt-4.1-mini',
          reasoningEnabled: false,
        },
        businessNodeComplexModel: {
          modelName: 'gpt-4.1',
          reasoningEnabled: true,
        },
        chatModel: {
          modelName: 'gpt-4.1',
          reasoningEnabled: true,
        },
      }),
    );
  });

  it('keeps invalid admin assistant model on card but removes it from route model options', async () => {
    setupApiMocks({
      adminAssistantConfig: {
        ...defaultAdminAssistantConfig,
        routeModel: {
          modelName: 'text-embedding-v4',
          reasoningEnabled: false,
          supportReasoning: false,
          supportVision: false,
        },
      },
    });

    await renderPage();

    expect(await screen.findByText('text-embedding-v4')).toBeTruthy();
    fireEvent.click(screen.getByTestId('open-admin-assistant'));
    const routeSlot = await screen.findByTestId('assistant-route-model');

    expect(
      screen.getByText('当前主配置下模型 text-embedding-v4 不可用，请重新选择聊天模型'),
    ).toBeTruthy();

    const selector = routeSlot.querySelector('.ant-select-selector');
    if (!selector) {
      throw new Error('Unable to locate route model selector');
    }
    fireEvent.mouseDown(selector);

    const dropdown = getVisibleSelectDropdown();
    expect(within(dropdown).queryByText('text-embedding-v4')).toBeNull();
    expect(within(dropdown).getAllByText('gpt-4.1-mini').length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    expect(await screen.findByText('请选择模型')).toBeTruthy();
    expect(mockSaveAdminAssistantConfig).not.toHaveBeenCalled();
  });

  it('shows reasoning switch only for reasoning-capable models', async () => {
    await renderPage();

    fireEvent.click(screen.getByTestId('open-admin-assistant'));
    const routeSlot = await screen.findByTestId('assistant-route-model');
    const complexSlot = await screen.findByTestId('assistant-complex-model');

    expect(within(routeSlot).queryByText('启用深度思考')).toBeNull();
    expect(within(complexSlot).getByText('启用深度思考')).toBeTruthy();
    expect(within(complexSlot).getByText('支持深度思考')).toBeTruthy();
  });

  it('submits mapped payload for client assistant drawer', async () => {
    await renderPage();

    await screen.findByText('客户端助手');
    fireEvent.click(screen.getByTestId('open-client-assistant'));
    await screen.findByTestId('client-assistant-route-model');

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() =>
      expect(mockSaveClientAssistantConfig).toHaveBeenCalledWith({
        routeModel: {
          modelName: 'gpt-4.1-mini',
          reasoningEnabled: false,
        },
        chatModel: {
          modelName: 'gpt-4.1',
          reasoningEnabled: true,
        },
        orderModel: {
          modelName: 'gpt-4.1-mini',
          reasoningEnabled: false,
        },
        productModel: {
          modelName: 'gpt-4.1-mini',
          reasoningEnabled: false,
        },
        afterSaleModel: {
          modelName: 'gpt-4.1-mini',
          reasoningEnabled: false,
        },
        consultationComfortModel: {
          modelName: 'gpt-4.1-mini',
          reasoningEnabled: false,
        },
        consultationQuestionModel: {
          modelName: 'gpt-4.1',
          reasoningEnabled: true,
        },
        consultationFinalDiagnosisModel: {
          modelName: 'gpt-4.1',
          reasoningEnabled: true,
        },
      }),
    );
  });

  it('shows partial client assistant status when some slots are missing', async () => {
    setupApiMocks({
      clientAssistantConfig: {
        ...defaultClientAssistantConfig,
        afterSaleModel: {
          modelName: undefined,
          reasoningEnabled: false,
          supportReasoning: false,
          supportVision: false,
        },
        consultationFinalDiagnosisModel: {
          modelName: undefined,
          reasoningEnabled: false,
          supportReasoning: false,
          supportVision: false,
        },
      },
    });

    await renderPage();

    const clientAssistantCard = getOverviewCard('客户端助手');
    expect(within(clientAssistantCard).getByText('待完善')).toBeTruthy();
    expect(within(clientAssistantCard).getByText('4/5')).toBeTruthy();
    expect(within(clientAssistantCard).getByText('2/3')).toBeTruthy();
  });

  it('keeps invalid client assistant model on card but removes it from route model options', async () => {
    setupApiMocks({
      clientAssistantConfig: {
        ...defaultClientAssistantConfig,
        routeModel: {
          modelName: 'text-embedding-v4',
          reasoningEnabled: false,
          supportReasoning: false,
          supportVision: false,
        },
      },
    });

    await renderPage();

    expect(await screen.findByText('text-embedding-v4')).toBeTruthy();
    fireEvent.click(screen.getByTestId('open-client-assistant'));
    const routeSlot = await screen.findByTestId('client-assistant-route-model');

    expect(
      screen.getByText('当前主配置下模型 text-embedding-v4 不可用，请重新选择聊天模型'),
    ).toBeTruthy();

    const selector = routeSlot.querySelector('.ant-select-selector');
    if (!selector) {
      throw new Error('Unable to locate client assistant route model selector');
    }
    fireEvent.mouseDown(selector);

    const dropdown = getVisibleSelectDropdown();
    expect(within(dropdown).queryByText('text-embedding-v4')).toBeNull();
    expect(within(dropdown).getAllByText('gpt-4.1-mini').length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    expect(await screen.findByText('请选择模型')).toBeTruthy();
    expect(mockSaveClientAssistantConfig).not.toHaveBeenCalled();
  });

  it('uses vision options for image recognition drawer and shows compact empty hint', async () => {
    setupApiMocks({
      imageRecognitionConfig: {
        imageRecognitionModel: {
          modelName: undefined,
          reasoningEnabled: false,
          supportReasoning: false,
          supportVision: false,
        },
      },
      visionOptions: [],
    });

    await renderPage();

    await screen.findByText('图片识别');
    fireEvent.click(screen.getByTestId('open-image-recognition'));
    await screen.findByTestId('image-recognition-model');

    expect(await screen.findByText('当前暂无图片理解模型')).toBeTruthy();
    const medicineField = screen.getByTestId('image-recognition-model');
    expect(medicineField.querySelector('.ant-select-disabled')).toBeTruthy();
  });

  it('opens speech drawer and saves mapped payload without echoing old token', async () => {
    await renderPage();

    await screen.findByText('豆包语音');
    fireEvent.click(screen.getByTestId('open-speech'));
    await screen.findByText('配置 豆包语音');

    const drawer = getDrawerContent('配置 豆包语音');
    const accessTokenInput = getFormItem('Access Token', drawer).querySelector('input');
    if (!accessTokenInput) {
      throw new Error('Unable to locate access token input');
    }
    expect((accessTokenInput as HTMLInputElement).value).toBe('');

    fireEvent.click(screen.getByTestId('voice-select-trigger'));
    await screen.findByTestId('speech-voice-card-zh_female_vv_uranus_bigtts');
    fireEvent.click(screen.getByTestId('speech-voice-card-zh_female_vv_uranus_bigtts'));
    fireEvent.click(screen.getByRole('button', { name: '选择' }));

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() =>
      expect(mockSaveSpeechConfig).toHaveBeenCalledWith({
        appId: 'speech-app-id',
        accessToken: null,
        textToSpeech: {
          voiceType: 'zh_female_vv_uranus_bigtts',
          maxTextChars: 300,
        },
      }),
    );
  });

  it('shows speech voice cards and highlights the configured voice by default', async () => {
    await renderPage();

    await screen.findByText('豆包语音');
    fireEvent.click(screen.getByTestId('open-speech'));
    await screen.findByText('配置 豆包语音');
    fireEvent.click(screen.getByTestId('voice-select-trigger'));

    expect(screen.getByText('小何 2.0')).toBeTruthy();
    expect(screen.getByText('声线甜美有活力的妹妹，活泼开朗，笑容明媚。')).toBeTruthy();
    expect(
      String(
        screen.getByTestId('speech-voice-card-zh_female_xiaohe_uranus_bigtts').className,
      ).includes('voiceCardActive'),
    ).toBe(true);
  });

  it('plays preview audio without changing the selected speech voice', async () => {
    await renderPage();

    await screen.findByText('豆包语音');
    fireEvent.click(screen.getByTestId('open-speech'));
    await screen.findByText('配置 豆包语音');
    fireEvent.click(screen.getByTestId('voice-select-trigger'));

    const firstPreviewCard = screen
      .getByTestId('speech-voice-card-zh_female_xiaohe_uranus_bigtts')
      .querySelector('div[title="点击试听"]');
    const secondPreviewCard = screen
      .getByTestId('speech-voice-card-zh_female_vv_uranus_bigtts')
      .querySelector('div[title="点击试听"]');
    if (!firstPreviewCard || !secondPreviewCard) {
      throw new Error('Unable to locate speech preview trigger');
    }

    fireEvent.click(firstPreviewCard);
    fireEvent.click(secondPreviewCard);

    await waitFor(() => expect(playMediaMock).toHaveBeenCalledTimes(2));
    expect(pauseMediaMock).toHaveBeenCalled();
    expect(
      String(
        screen.getByTestId('speech-voice-card-zh_female_xiaohe_uranus_bigtts').className,
      ).includes('voiceCardActive'),
    ).toBe(true);
    expect(
      String(screen.getByTestId('speech-voice-card-zh_female_vv_uranus_bigtts').className).includes(
        'voiceCardActive',
      ),
    ).toBe(false);
  });

  it('keeps unknown speech voice type and shows fallback hint until reselected', async () => {
    setupApiMocks({
      speechConfig: {
        ...defaultSpeechConfig,
        textToSpeech: {
          ...defaultSpeechConfig.textToSpeech,
          voiceType: 'custom_unknown_voice_type',
        },
      },
    });

    await renderPage();

    await screen.findByText('豆包语音');
    fireEvent.click(screen.getByTestId('open-speech'));
    await screen.findByText('配置 豆包语音');
    fireEvent.click(screen.getByTestId('voice-select-trigger'));

    expect(screen.getByTestId('speech-voice-unknown-hint')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: '取消' }));

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() =>
      expect(mockSaveSpeechConfig).toHaveBeenCalledWith({
        appId: 'speech-app-id',
        accessToken: null,
        textToSpeech: {
          voiceType: 'custom_unknown_voice_type',
          maxTextChars: 300,
        },
      }),
    );
  });

  it('opens chat history summary drawer and saves mapped payload', async () => {
    await renderPage();

    await screen.findByText('聊天历史总结');
    fireEvent.click(screen.getByTestId('open-chat-history-summary'));
    await screen.findByTestId('chat-history-summary-model');

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() =>
      expect(mockSaveChatHistorySummaryConfig).toHaveBeenCalledWith({
        chatHistorySummaryModel: {
          modelName: 'gpt-4.1-mini',
          reasoningEnabled: false,
        },
      }),
    );
    expect(mockGetChatModelOptions).toHaveBeenCalledTimes(1);
  });

  it('keeps invalid chat history summary model on card but clears it from chat options in drawer', async () => {
    setupApiMocks({
      chatHistorySummaryConfig: {
        chatHistorySummaryModel: {
          modelName: 'text-embedding-v4',
          reasoningEnabled: false,
          supportReasoning: false,
          supportVision: false,
        },
      },
    });

    await renderPage();

    expect(await screen.findByText('text-embedding-v4')).toBeTruthy();
    fireEvent.click(screen.getByTestId('open-chat-history-summary'));
    const slot = await screen.findByTestId('chat-history-summary-model');

    expect(
      screen.getByText('当前主配置下模型 text-embedding-v4 不可用，请重新选择聊天模型'),
    ).toBeTruthy();

    const selector = slot.querySelector('.ant-select-selector');
    if (!selector) {
      throw new Error('Unable to locate chat history summary selector');
    }
    fireEvent.mouseDown(selector);

    const dropdown = getVisibleSelectDropdown();
    expect(within(dropdown).queryByText('text-embedding-v4')).toBeNull();
    expect(within(dropdown).getAllByText('gpt-4.1-mini').length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    expect(await screen.findByText('请选择模型')).toBeTruthy();
    expect(mockSaveChatHistorySummaryConfig).not.toHaveBeenCalled();
  });

  it('opens chat title drawer and saves mapped payload', async () => {
    await renderPage();

    await screen.findByText('聊天标题生成');
    fireEvent.click(screen.getByTestId('open-chat-title'));
    await screen.findByTestId('chat-title-model');

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() =>
      expect(mockSaveChatTitleConfig).toHaveBeenCalledWith({
        chatTitleModel: {
          modelName: 'gpt-4.1',
          reasoningEnabled: true,
        },
      }),
    );
    expect(mockGetChatModelOptions).toHaveBeenCalledTimes(1);
  });

  it('keeps invalid chat title model on card but clears it from chat options in drawer', async () => {
    setupApiMocks({
      chatTitleConfig: {
        chatTitleModel: {
          modelName: 'text-embedding-v4',
          reasoningEnabled: false,
          supportReasoning: false,
          supportVision: false,
        },
      },
    });

    await renderPage();

    expect(await screen.findByText('text-embedding-v4')).toBeTruthy();
    fireEvent.click(screen.getByTestId('open-chat-title'));
    const slot = await screen.findByTestId('chat-title-model');

    expect(
      screen.getByText('当前主配置下模型 text-embedding-v4 不可用，请重新选择聊天模型'),
    ).toBeTruthy();

    const selector = slot.querySelector('.ant-select-selector');
    if (!selector) {
      throw new Error('Unable to locate chat title selector');
    }
    fireEvent.mouseDown(selector);

    const dropdown = getVisibleSelectDropdown();
    expect(within(dropdown).queryByText('text-embedding-v4')).toBeNull();
    expect(within(dropdown).getAllByText('gpt-4.1').length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    expect(await screen.findByText('请选择模型')).toBeTruthy();
    expect(mockSaveChatTitleConfig).not.toHaveBeenCalled();
  });

  it('blocks knowledge base save when no knowledge base is selected', async () => {
    setupApiMocks({
      knowledgeBaseConfig: {
        enabled: true,
        knowledgeNames: [],
        embeddingDim: null,
        topK: null,
        embeddingModel: null,
        rankingEnabled: false,
        rankingModel: null,
      },
    });

    await renderPage();

    await screen.findByText('知识库');
    fireEvent.click(screen.getByTestId('open-knowledge-base'));
    await screen.findByText('配置知识库');
    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    expect(await screen.findByText('请至少选择一个知识库')).toBeTruthy();
    expect(mockSaveKnowledgeBaseConfig).not.toHaveBeenCalled();
  });

  it('suggests enabling ranking when knowledge base count exceeds threshold', async () => {
    const knowledgeBaseOptions = Array.from({ length: 4 }, (_value, index) => ({
      knowledgeName: `knowledge_${index + 1}`,
      displayName: `知识库 ${index + 1}`,
      embeddingModel: 'text-embedding-3-large',
      embeddingDim: 1024,
    }));

    setupApiMocks({
      knowledgeBaseConfig: {
        enabled: true,
        knowledgeNames: knowledgeBaseOptions.map((item) => item.knowledgeName),
        embeddingDim: 1024,
        topK: 10,
        embeddingModel: {
          modelName: 'text-embedding-3-large',
          reasoningEnabled: false,
          supportReasoning: false,
          supportVision: false,
        },
        rankingEnabled: false,
        rankingModel: null,
      },
      knowledgeBaseOptions,
    });

    await renderPage();

    await screen.findByText('知识库');
    fireEvent.click(screen.getByTestId('open-knowledge-base'));
    await screen.findByText('配置知识库');

    const rankingSwitch = screen.getByTestId('knowledge-ranking-switch');
    expect(rankingSwitch.getAttribute('aria-checked')).toBe('false');
    expect((rankingSwitch as HTMLButtonElement).disabled).toBe(false);
    expect(screen.getByText('关联知识库较多时，建议开启重排以提升回答准确度')).toBeTruthy();
  });

  it('clears invalid ranking model from knowledge base drawer chat options', async () => {
    setupApiMocks({
      knowledgeBaseConfig: {
        ...defaultKnowledgeBaseConfig,
        rankingEnabled: true,
        rankingModel: {
          modelName: 'text-embedding-v4',
          reasoningEnabled: false,
          supportReasoning: false,
          supportVision: false,
        },
      },
    });

    await renderPage();

    fireEvent.click(screen.getByTestId('open-knowledge-base'));
    await screen.findByText('配置知识库');

    expect(
      await screen.findByText('当前主配置下模型 text-embedding-v4 不可用，请重新选择聊天模型'),
    ).toBeTruthy();

    const drawer = getDrawerContent('配置知识库');
    const rankingField = getFormItem('重排模型', drawer);
    const selector = rankingField.querySelector('.ant-select-selector');
    if (!selector) {
      throw new Error('Unable to locate ranking selector');
    }
    fireEvent.mouseDown(selector);

    const dropdown = getVisibleSelectDropdown();
    expect(within(dropdown).queryByText('text-embedding-v4')).toBeNull();
    expect(within(dropdown).getAllByText('gpt-4.1-mini').length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    expect(await screen.findByText('请选择用于重排的模型')).toBeTruthy();
    expect(mockSaveKnowledgeBaseConfig).not.toHaveBeenCalled();
  });

  it('blocks speech save when max text chars exceeds limit', async () => {
    await renderPage();

    await screen.findByText('豆包语音');
    fireEvent.click(screen.getByTestId('open-speech'));
    await screen.findByText('配置 豆包语音');

    const drawer = getDrawerContent('配置 豆包语音');
    const maxTextCharsInput = getFormItem('最大文本长度', drawer).querySelector('input');
    if (!maxTextCharsInput) {
      throw new Error('Unable to locate max text chars input');
    }

    fireEvent.change(maxTextCharsInput, { target: { value: '3001' } });
    fireEvent.blur(maxTextCharsInput);
    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() => expect(mockSaveSpeechConfig).not.toHaveBeenCalled());
  });

  it('refreshes only speech card after successful save', async () => {
    await renderPage();

    await screen.findByText('豆包语音');
    fireEvent.click(screen.getByTestId('open-speech'));
    await screen.findByText('配置 豆包语音');

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() => expect(mockSaveSpeechConfig).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockGetSpeechConfig).toHaveBeenCalledTimes(2));
    expect(mockGetKnowledgeBaseConfig).toHaveBeenCalledTimes(1);
    expect(mockGetAdminAssistantConfig).toHaveBeenCalledTimes(1);
    expect(mockGetClientAssistantConfig).toHaveBeenCalledTimes(1);
    expect(mockGetImageRecognitionConfig).toHaveBeenCalledTimes(1);
    expect(mockGetChatHistorySummaryConfig).toHaveBeenCalledTimes(1);
    expect(mockGetChatTitleConfig).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(screen.queryByText('配置 豆包语音')).toBeNull());
  });

  it('refreshes only chat history summary card after successful save', async () => {
    await renderPage();

    await screen.findByText('聊天历史总结');
    fireEvent.click(screen.getByTestId('open-chat-history-summary'));
    await screen.findByTestId('chat-history-summary-model');

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() => expect(mockSaveChatHistorySummaryConfig).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockGetChatHistorySummaryConfig).toHaveBeenCalledTimes(2));
    expect(mockGetKnowledgeBaseConfig).toHaveBeenCalledTimes(1);
    expect(mockGetAdminAssistantConfig).toHaveBeenCalledTimes(1);
    expect(mockGetClientAssistantConfig).toHaveBeenCalledTimes(1);
    expect(mockGetSpeechConfig).toHaveBeenCalledTimes(1);
    expect(mockGetImageRecognitionConfig).toHaveBeenCalledTimes(1);
    expect(mockGetChatTitleConfig).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(screen.queryByTestId('chat-history-summary-model')).toBeNull());
  });

  it('hides reasoning switch for unsupported conversation summary model', async () => {
    setupApiMocks({
      chatHistorySummaryConfig: {
        chatHistorySummaryModel: {
          modelName: 'gpt-4.1-mini',
          reasoningEnabled: false,
          supportReasoning: false,
          supportVision: false,
        },
      },
      chatOptions: [
        {
          label: 'gpt-4.1-mini',
          value: 'gpt-4.1-mini',
          supportReasoning: false,
          supportVision: false,
        },
      ],
    });

    await renderPage();

    fireEvent.click(screen.getByTestId('open-chat-history-summary'));
    const slot = await screen.findByTestId('chat-history-summary-model');

    expect(within(slot).queryByText('启用深度思考')).toBeNull();
  });

  it('refreshes only knowledge base summary and closes drawer after successful save', async () => {
    await renderPage();

    await screen.findByText('知识库');
    fireEvent.click(screen.getByTestId('open-knowledge-base'));
    await screen.findByText('配置知识库');
    const drawer = getDrawerContent('配置知识库');
    expect(getFormItem('向量模型', drawer).querySelector('.ant-select')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() => expect(mockSaveKnowledgeBaseConfig).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockGetKnowledgeBaseConfig).toHaveBeenCalledTimes(2));
    expect(mockGetAdminAssistantConfig).toHaveBeenCalledTimes(1);
    expect(mockGetClientAssistantConfig).toHaveBeenCalledTimes(1);
    expect(mockGetSpeechConfig).toHaveBeenCalledTimes(1);
    expect(mockGetImageRecognitionConfig).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(screen.queryByText('配置知识库')).toBeNull());
  });

  it('shows backend error message when knowledge base save fails', async () => {
    mockSaveKnowledgeBaseConfig.mockRejectedValueOnce(
      new Error('模型未启用：text-embedding-3-large'),
    );

    await renderPage();

    await screen.findByText('知识库');
    fireEvent.click(screen.getByTestId('open-knowledge-base'));
    await screen.findByText('配置知识库');
    const drawer = getDrawerContent('配置知识库');
    expect(getFormItem('向量模型', drawer).querySelector('.ant-select')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() =>
      expect(errorSpy).toHaveBeenCalledWith('模型未启用：text-embedding-3-large'),
    );
  });

  it('opens model provider page from toolbar', async () => {
    await renderPage();

    fireEvent.click(screen.getByRole('button', { name: /提供商/ }));

    expect(mockNavigate).toHaveBeenCalledWith('/llm-manage/model-providers');
  });
});
