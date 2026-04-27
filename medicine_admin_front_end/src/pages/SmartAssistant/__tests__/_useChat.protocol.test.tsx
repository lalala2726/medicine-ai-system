import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { message } from 'antd';
import { AssistantApiError } from '@/api/chat';
import { authTokenStore, chatStore } from '@/store';
import { useChat } from '../_useChat';

const mockNavigate = jest.fn();
const mockGetConversation = jest.fn();
const mockSubmitAssistantMessage = jest.fn();
const mockStopAssistantMessage = jest.fn();
const mockFetchEventSource = jest.fn();

let mockRouteParams: Record<string, string | undefined> = {};
let latestStreamRegistration:
  | {
      conversationUuid: string;
      callbacks: Record<string, any>;
      controller: AbortController;
    }
  | undefined;

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useParams: () => mockRouteParams,
}));

jest.mock('@/api/agent', () => ({
  getConversation: (...args: any[]) => mockGetConversation(...args),
}));

jest.mock('@/api/chat', () => {
  const actual = jest.requireActual('@/api/chat');
  return {
    ...actual,
    submitAssistantMessage: (...args: any[]) => mockSubmitAssistantMessage(...args),
    stopAssistantMessage: (...args: any[]) => mockStopAssistantMessage(...args),
  };
});

jest.mock('@microsoft/fetch-event-source', () => ({
  fetchEventSource: (...args: any[]) => mockFetchEventSource(...args),
}));

const HookHarness: React.FC = () => {
  const inputRef = React.useRef<HTMLInputElement | null>(null);
  const {
    messages,
    content,
    setContent,
    submitting,
    loading,
    stopPending,
    sendMessage,
    stopGenerating,
    conversationUuid,
  } = useChat();

  return (
    <div>
      <div data-testid="conversation">{conversationUuid || ''}</div>
      <div data-testid="submitting">{String(submitting)}</div>
      <div data-testid="loading">{String(loading)}</div>
      <div data-testid="stopPending">{String(stopPending)}</div>
      <input
        aria-label="chat-input"
        ref={inputRef}
        value={content}
        onChange={(event) => setContent(event.target.value)}
      />
      <button type="button" onClick={() => sendMessage(inputRef.current?.value || '')}>
        send
      </button>
      <button type="button" onClick={stopGenerating}>
        stop
      </button>
      <pre data-testid="messages">{JSON.stringify(messages)}</pre>
    </div>
  );
};

describe('useChat protocol flow', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockRouteParams = {};
    latestStreamRegistration = undefined;
    chatStore.getState().clearMessages();
    authTokenStore.getState().setAccessToken('Bearer test-token');
    window.history.pushState({}, '', '/smart-assistant');

    mockGetConversation.mockResolvedValue([]);
    mockNavigate.mockImplementation((path: string) => {
      const conversationId = path.replace('/smart-assistant/', '').replace('/smart-assistant', '');
      mockRouteParams = conversationId ? { conversationId, '*': undefined } : {};
      window.history.pushState({}, '', path);
    });
    mockFetchEventSource.mockImplementation(async (url: string, options: any) => {
      await options.onopen?.({ ok: true, status: 200, statusText: 'OK' });
      const conversationUuid = new URL(url, 'http://localhost').searchParams.get(
        'conversation_uuid',
      );
      latestStreamRegistration = {
        conversationUuid: conversationUuid || '',
        callbacks: {
          onMessage: (payload: Record<string, any>) =>
            options.onmessage?.({ data: JSON.stringify(payload) }),
          onFinish: () => options.onclose?.(),
          onError: (error: unknown) => options.onerror?.(error),
        },
        controller: new AbortController(),
      };
      return Promise.resolve();
    });
  });

  it('navigates immediately when submit creates a new conversation', async () => {
    mockSubmitAssistantMessage.mockResolvedValue({
      conversation_uuid: 'conv-new',
      message_uuid: 'msg-new',
      run_status: 'running',
    });

    render(<HookHarness />);

    fireEvent.change(screen.getByLabelText('chat-input'), {
      target: { value: '帮我查一下退款订单' },
    });
    fireEvent.click(screen.getByText('send'));

    await waitFor(() => {
      expect(mockSubmitAssistantMessage).toHaveBeenCalledWith({
        question: '帮我查一下退款订单',
        conversation_uuid: undefined,
      });
    });

    expect(mockNavigate).toHaveBeenCalledWith('/smart-assistant/conv-new', { replace: false });
    expect(screen.getByTestId('conversation').textContent).toBe('conv-new');
  });

  it('keeps draft and attaches existing stream on submit conflict 409', async () => {
    const infoSpy = jest.spyOn(message, 'info').mockImplementation(jest.fn());
    mockRouteParams = { conversationId: 'conv-1', '*': undefined };
    mockSubmitAssistantMessage.mockRejectedValue(
      new AssistantApiError(409, '当前会话已有正在输出的回答', {
        conversation_uuid: 'conv-1',
        message_uuid: 'msg-running',
        run_status: 'running',
      }),
    );

    render(<HookHarness />);

    await waitFor(() => {
      expect(mockGetConversation).toHaveBeenCalledWith('conv-1', 1, 50);
    });

    fireEvent.change(screen.getByLabelText('chat-input'), {
      target: { value: '第二条消息' },
    });
    fireEvent.click(screen.getByText('send'));

    await waitFor(() => {
      expect(mockFetchEventSource).toHaveBeenCalledWith(
        '/ai_api/admin/assistant/chat/stream?conversation_uuid=conv-1',
        expect.any(Object),
      );
    });

    expect(infoSpy).toHaveBeenCalledWith('当前会话已有回答进行中，已为你恢复连接');
    expect((screen.getByLabelText('chat-input') as HTMLInputElement).value).toBe('第二条消息');
    expect(chatStore.getState().messages).toEqual([
      expect.objectContaining({
        id: 'msg-running',
        role: 'ai',
        status: 'streaming',
        isFinished: false,
      }),
    ]);
  });

  it('auto attaches when history last assistant message is still streaming', async () => {
    mockRouteParams = { conversationId: 'conv-2', '*': undefined };
    mockGetConversation.mockResolvedValue([
      {
        message_uuid: 'msg-2',
        role: 'assistant',
        content: '已有内容',
        status: 'streaming',
      },
    ]);

    render(<HookHarness />);

    await waitFor(() => {
      expect(mockFetchEventSource).toHaveBeenCalledWith(
        '/ai_api/admin/assistant/chat/stream?conversation_uuid=conv-2',
        expect.any(Object),
      );
    });

    expect(chatStore.getState().messages).toEqual([
      expect.objectContaining({
        id: 'msg-2',
        role: 'ai',
        content: '已有内容',
        status: 'streaming',
        isFinished: false,
      }),
    ]);

    act(() => {
      latestStreamRegistration?.callbacks.onMessage({
        type: 'answer',
        content: { text: '当前已生成的完整回答', state: 'replace' },
        meta: {
          conversation_uuid: 'conv-2',
          message_uuid: 'msg-2',
          snapshot: true,
          replace: true,
        },
        is_end: false,
      });
      latestStreamRegistration?.callbacks.onMessage({
        type: 'answer',
        content: { text: '', state: 'success' },
        meta: {
          message_uuid: 'msg-2',
          run_status: 'success',
        },
        is_end: true,
      });
    });

    await waitFor(() => {
      const aiMessage = chatStore.getState().messages[0];
      expect(aiMessage.content).toBe('当前已生成的完整回答');
      expect(aiMessage.status).toBe('success');
      expect(aiMessage.isFinished).toBe(true);
    });
  });

  it('sends stop request and waits for cancelled end event', async () => {
    mockRouteParams = { conversationId: 'conv-1', '*': undefined };
    mockGetConversation.mockResolvedValue([
      {
        message_uuid: 'msg-1',
        role: 'assistant',
        content: '生成中',
        status: 'streaming',
      },
    ]);
    mockStopAssistantMessage.mockResolvedValue({
      conversation_uuid: 'conv-1',
      message_uuid: 'msg-1',
      run_status: 'running',
      stop_requested: true,
    });

    render(<HookHarness />);

    await waitFor(() => {
      expect(mockFetchEventSource).toHaveBeenCalledWith(
        '/ai_api/admin/assistant/chat/stream?conversation_uuid=conv-1',
        expect.any(Object),
      );
    });

    fireEvent.click(screen.getByText('stop'));

    await waitFor(() => {
      expect(mockStopAssistantMessage).toHaveBeenCalledWith({ conversation_uuid: 'conv-1' });
      expect(screen.getByTestId('stopPending').textContent).toBe('true');
    });

    act(() => {
      latestStreamRegistration?.callbacks.onMessage({
        type: 'answer',
        content: { text: '', state: 'cancelled', message: '已停止生成' },
        meta: {
          message_uuid: 'msg-1',
          run_status: 'cancelled',
        },
        is_end: true,
      });
    });

    await waitFor(() => {
      const aiMessage = chatStore.getState().messages[0];
      expect(aiMessage.status).toBe('cancelled');
      expect(aiMessage.isFinished).toBe(true);
      expect(screen.getByTestId('stopPending').textContent).toBe('false');
      expect(screen.getByTestId('loading').textContent).toBe('false');
    });
  });
});
