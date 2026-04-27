import React from 'react';

interface ErrorBoundaryState {
  hasError: boolean;
  error?: Error;
}

interface ErrorBoundaryProps {
  children: React.ReactNode;
}

class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    // 更新 state 使下一次渲染能够显示降级后的 UI
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    // 可以将错误日志上报给服务器
    console.error('Login ErrorBoundary caught an error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      // 可以自定义降级后的 UI 并渲染
      return (
        <div style={{ padding: '20px', textAlign: 'center' }}>
          <h2>登录页面出现错误</h2>
          <p>请刷新页面重试</p>
          <button
            type="button"
            onClick={() => {
              this.setState({ hasError: false });
              window.location.reload();
            }}
            style={{
              padding: '8px 16px',
              backgroundColor: '#1890ff',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
            }}
          >
            刷新页面
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
