import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { Modal } from 'antd';

import ModelProviders from './index';

const mockNavigate = jest.fn();
const mockGetProviderPresetList = jest.fn();
const mockGetProviderList = jest.fn();
const mockDeleteProvider = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('antd', () => {
  const actual = jest.requireActual('antd');

  return {
    ...actual,
    Dropdown: ({ children, menu }: any) => (
      <div>
        {children}
        <div>
          {(menu?.items || []).map((item: any, index: number) => {
            if (!item || item.type === 'divider') {
              return null;
            }
            return (
              <button key={item.key || index} type="button" onClick={item.onClick}>
                {item.label}
              </button>
            );
          })}
        </div>
      </div>
    ),
  };
});

jest.mock('@/api/llm-manage/modelProviders', () => ({
  getProviderPresetList: (...args: any[]) => mockGetProviderPresetList(...args),
  getProviderList: (...args: any[]) => mockGetProviderList(...args),
  deleteProvider: (...args: any[]) => mockDeleteProvider(...args),
}));

jest.mock('@ant-design/pro-components', () => {
  const ReactMock = require('react') as typeof import('react');

  const ProTable = ({ request, columns, toolBarRender, actionRef }: any) => {
    const [rows, setRows] = ReactMock.useState<Array<Record<string, any>>>([]);

    const load = ReactMock.useCallback(async () => {
      const result = await request({ current: 1, pageSize: 10 });
      setRows(result.data || []);
    }, [request]);

    ReactMock.useEffect(() => {
      void load();
    }, [load]);

    ReactMock.useEffect(() => {
      if (actionRef) {
        actionRef.current = { reload: load };
      }
    }, [actionRef, load]);

    return (
      <div>
        <div>{toolBarRender?.()}</div>
        <div>
          {rows.map((row: Record<string, any>, rowIndex: number) => (
            <div key={row.id || rowIndex}>
              {columns.map((column: any, columnIndex: number) => (
                <div key={`${row.id}-${column.dataIndex || columnIndex}`}>
                  {column.render
                    ? column.render(row[column.dataIndex], row, rowIndex, {})
                    : row[column.dataIndex]}
                </div>
              ))}
            </div>
          ))}
        </div>
      </div>
    );
  };

  return {
    PageContainer: ({ children }: any) => <div>{children}</div>,
    ProTable,
  };
});

describe('ModelProviders page', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    mockGetProviderPresetList.mockResolvedValue([
      {
        providerKey: 'openai',
        providerName: 'OpenAI',
        providerType: 'openai',
        baseUrl: 'https://api.openai.com/v1',
        description: 'OpenAI preset',
      },
    ]);

    mockGetProviderList.mockResolvedValue({
      total: '1',
      rows: [
        {
          id: '1',
          providerName: 'OpenAI Primary',
          providerType: 'openai',
          baseUrl: 'https://api.openai.com/v1',
          modelCount: '2',
          chatModelCount: '1',
          embeddingModelCount: '1',
          updatedAt: '2026-03-10',
        },
      ],
    });
  });

  it('maps list request params and navigates from source modal', async () => {
    render(<ModelProviders />);

    await waitFor(() =>
      expect(mockGetProviderList).toHaveBeenCalledWith({
        pageNum: 1,
        pageSize: 10,
        providerName: undefined,
      }),
    );

    fireEvent.click(screen.getByText('新增提供商'));

    expect(await screen.findByText('选择模型提供商')).toBeTruthy();
    fireEvent.click(screen.getByText('兼容 OpenAI'));
    expect(mockNavigate).toHaveBeenCalledWith(
      '/llm-manage/model-providers/create?source=openai-compatible',
    );
  });

  it('deletes a provider and reloads the table', async () => {
    const confirmSpy = jest.spyOn(Modal, 'confirm').mockImplementation(({ onOk }: any) => {
      void onOk?.();
      return {
        destroy: jest.fn(),
        update: jest.fn(),
      } as any;
    });

    mockDeleteProvider.mockResolvedValue(undefined);

    render(<ModelProviders />);

    await screen.findByText('OpenAI Primary');
    const initialRequestCount = mockGetProviderList.mock.calls.length;

    fireEvent.click(screen.getByText('更多'));
    fireEvent.click(await screen.findByText('删除'));

    await waitFor(() => expect(mockDeleteProvider).toHaveBeenCalledWith('1'));
    await waitFor(() =>
      expect(mockGetProviderList.mock.calls.length).toBeGreaterThan(initialRequestCount),
    );

    confirmSpy.mockRestore();
  });
});
