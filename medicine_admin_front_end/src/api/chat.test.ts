import { attachAssistantStream, resolveAssistantFinalStatus, submitAssistantMessage } from './chat';
import { authTokenStore } from '@/store/authTokenStore';

const mockFetchEventSource = jest.fn();

jest.mock('@microsoft/fetch-event-source', () => ({
  fetchEventSource: (...args: any[]) => mockFetchEventSource(...args),
}));

describe('assistant chat api', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    authTokenStore.getState().setAccessToken('Bearer test-token');
    (global as any).fetch = jest.fn();
  });

  it('submits assistant message and returns data on code 200', async () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      ok: true,
      status: 200,
      statusText: 'OK',
      json: async () => ({
        code: 200,
        message: '操作成功',
        timestamp: 1742400000000,
        data: {
          conversation_uuid: 'conv-1',
          message_uuid: 'msg-1',
          run_status: 'running',
        },
      }),
    });

    const result = await submitAssistantMessage({
      question: '帮我查一下退款订单',
      conversation_uuid: 'conv-1',
      model_name: '运营助手',
      reasoning_enabled: false,
    });

    expect(result).toEqual({
      conversation_uuid: 'conv-1',
      message_uuid: 'msg-1',
      run_status: 'running',
    });
    expect(global.fetch).toHaveBeenCalledWith('/ai_api/admin/assistant/chat/submit', {
      method: 'POST',
      headers: {
        Authorization: 'Bearer test-token',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        question: '帮我查一下退款订单',
        conversation_uuid: 'conv-1',
        model_name: '运营助手',
        reasoning_enabled: false,
      }),
    });
  });

  it('throws AssistantApiError with conflict payload on code 409', async () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      ok: true,
      status: 200,
      statusText: 'OK',
      json: async () => ({
        code: 409,
        message: '当前会话已有正在输出的回答',
        timestamp: 1742400000000,
        data: {
          conversation_uuid: 'conv-1',
          message_uuid: 'msg-running',
          run_status: 'running',
        },
      }),
    });

    await expect(
      submitAssistantMessage({
        question: '第二条问题',
        conversation_uuid: 'conv-1',
        model_name: '运营助手',
        reasoning_enabled: true,
      }),
    ).rejects.toMatchObject({
      name: 'AssistantApiError',
      code: 409,
      data: {
        conversation_uuid: 'conv-1',
        message_uuid: 'msg-running',
        run_status: 'running',
      },
    });
  });

  it('attaches stream through GET endpoint and parses SSE payload', async () => {
    const onMessage = jest.fn();
    const onFinish = jest.fn();

    mockFetchEventSource.mockImplementation(async (_url: string, options: any) => {
      await options.onopen({ ok: true, status: 200, statusText: 'OK' });
      options.onmessage({
        data: JSON.stringify({
          type: 'answer',
          content: { text: '你好' },
          meta: {
            conversation_uuid: 'conv-1',
            message_uuid: 'msg-1',
          },
          is_end: false,
        }),
      });
      options.onclose();
    });

    attachAssistantStream('conv-1', {
      onMessage,
      onFinish,
    });

    await Promise.resolve();

    expect(mockFetchEventSource).toHaveBeenCalledWith(
      '/ai_api/admin/assistant/chat/stream?conversation_uuid=conv-1',
      expect.objectContaining({
        method: 'GET',
        headers: {
          Authorization: 'Bearer test-token',
        },
      }),
    );
    expect(onMessage).toHaveBeenCalledWith({
      type: 'answer',
      content: { text: '你好' },
      meta: {
        conversation_uuid: 'conv-1',
        message_uuid: 'msg-1',
      },
      is_end: false,
    });
    expect(onFinish).toHaveBeenCalled();
  });

  it('resolves final status with meta first and content state fallback', () => {
    expect(
      resolveAssistantFinalStatus({
        type: 'answer',
        content: { state: 'error' },
        meta: { run_status: 'cancelled' },
        is_end: true,
      }),
    ).toBe('cancelled');

    expect(
      resolveAssistantFinalStatus({
        type: 'answer',
        content: { state: 'error' },
        is_end: true,
      }),
    ).toBe('error');

    expect(
      resolveAssistantFinalStatus({
        type: 'answer',
        content: {},
        is_end: true,
      }),
    ).toBe('success');
  });
});
