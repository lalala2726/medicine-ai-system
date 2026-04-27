import React from 'react';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';

import ClientAssistantConfigDrawer from './ClientAssistantConfigDrawer';

const mockGetChatModelOptions = jest.fn();
const mockSaveClientAssistantConfig = jest.fn();

jest.mock('@/api/llm-manage/systemModels', () => ({
  getChatModelOptions: (...args: any[]) => mockGetChatModelOptions(...args),
  saveClientAssistantConfig: (...args: any[]) => mockSaveClientAssistantConfig(...args),
}));

jest.mock('@/contexts/ThemeContext', () => ({
  useThemeContext: () => ({ isDark: false }),
}));

/** 默认客户端助手详情。 */
const defaultDetail = {
  routeModel: {
    modelName: 'gpt-4.1-mini',
    reasoningEnabled: false,
    supportReasoning: false,
    supportVision: false,
  },
  serviceNodeModel: {
    modelName: 'gpt-4.1',
    reasoningEnabled: true,
    supportReasoning: true,
    supportVision: true,
  },
  diagnosisNodeModel: {
    modelName: 'gpt-4.1',
    reasoningEnabled: false,
    supportReasoning: true,
    supportVision: true,
  },
};

/**
 * 渲染客户端助手配置抽屉。
 *
 * @param detail 当前详情。
 */
async function renderDrawer(detail = defaultDetail) {
  await act(async () => {
    render(
      <ClientAssistantConfigDrawer open detail={detail} onClose={jest.fn()} onSaved={jest.fn()} />,
    );
    await Promise.resolve();
  });
}

describe('ClientAssistantConfigDrawer', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetChatModelOptions.mockResolvedValue([
      {
        label: 'gpt-4.1-mini',
        value: 'gpt-4.1-mini',
        supportReasoning: false,
        supportVision: false,
      },
      {
        label: 'gpt-4.1',
        value: 'gpt-4.1',
        supportReasoning: true,
        supportVision: true,
      },
    ]);
    mockSaveClientAssistantConfig.mockResolvedValue(undefined);
  });

  it('submits mapped payload for all client assistant slots', async () => {
    await renderDrawer();

    await screen.findByTestId('client-assistant-route-model');
    fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

    await waitFor(() =>
      expect(mockSaveClientAssistantConfig).toHaveBeenCalledWith({
        routeModel: {
          modelName: 'gpt-4.1-mini',
        },
        serviceNodeModel: {
          modelName: 'gpt-4.1',
        },
        diagnosisNodeModel: {
          modelName: 'gpt-4.1',
        },
      }),
    );
  });

  it('shows invalid selection hint and removes invalid client assistant model from options', async () => {
    await renderDrawer({
      ...defaultDetail,
      routeModel: {
        modelName: 'text-embedding-v4',
        reasoningEnabled: false,
        supportReasoning: false,
        supportVision: false,
      },
    });

    const routeSlot = await screen.findByTestId('client-assistant-route-model');
    expect(
      screen.getByText('当前主配置下模型 text-embedding-v4 不可用，请重新选择聊天模型'),
    ).toBeTruthy();

    const selector = routeSlot.querySelector('.ant-select-selector');
    if (!selector) {
      throw new Error('Unable to locate client assistant route model selector');
    }
    fireEvent.mouseDown(selector);

    const dropdowns = Array.from(document.body.querySelectorAll('.ant-select-dropdown')).filter(
      (element) => !element.classList.contains('ant-select-dropdown-hidden'),
    );
    const dropdown = dropdowns.at(-1);
    if (!dropdown) {
      throw new Error('Unable to locate visible select dropdown');
    }

    expect(within(dropdown as HTMLElement).queryByText('text-embedding-v4')).toBeNull();
    expect(within(dropdown as HTMLElement).getAllByText('gpt-4.1-mini').length).toBeGreaterThan(0);
  });
});
