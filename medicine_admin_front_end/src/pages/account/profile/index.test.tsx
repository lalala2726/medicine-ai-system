import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import AccountProfilePage from './index';

const mockNavigate = jest.fn();
const mockSetInitialState = jest.fn();
const mockClearAuth = jest.fn();
const mockMessageSuccess = jest.fn();
const mockMessageError = jest.fn();
const mockMessageWarning = jest.fn();
const mockChangeAdminPassword = jest.fn();
const mockChangeAdminPhone = jest.fn();
const mockCurrentUser = jest.fn();
const mockSendAdminPhoneCode = jest.fn();
const mockUpdateAdminProfile = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('antd', () => {
  const actual = jest.requireActual('antd');

  return {
    ...actual,
    message: {
      ...actual.message,
      useMessage: () => [
        {
          success: mockMessageSuccess,
          error: mockMessageError,
          warning: mockMessageWarning,
        },
        <div key="message-holder" />,
      ],
    },
  };
});

jest.mock('@ant-design/pro-components', () => ({
  PageContainer: ({ title, children }: any) => (
    <div>
      <div>{title}</div>
      {children}
    </div>
  ),
}));

jest.mock('@/api/core/login', () => ({
  changeAdminPassword: (...args: any[]) => mockChangeAdminPassword(...args),
  changeAdminPhone: (...args: any[]) => mockChangeAdminPhone(...args),
  currentUser: (...args: any[]) => mockCurrentUser(...args),
  sendAdminPhoneCode: (...args: any[]) => mockSendAdminPhoneCode(...args),
  updateAdminProfile: (...args: any[]) => mockUpdateAdminProfile(...args),
}));

jest.mock('@/components', () => ({
  AvatarUpload: () => <div data-testid="avatar-upload">头像上传</div>,
}));

jest.mock('@/contexts/ThemeContext', () => ({
  useThemeContext: () => ({ isDark: false }),
}));

jest.mock('@/hooks/useAuth', () => ({
  useAuthActions: () => ({ clearAuth: mockClearAuth }),
}));

jest.mock('@/hooks/useInitialState', () => ({
  useInitialState: () => ({
    initialState: {
      currentUser: {
        id: 1,
        username: 'admin',
        nickname: '管理员',
        phoneNumber: '13800000000',
      },
    },
    setInitialState: mockSetInitialState,
  }),
}));

jest.mock('@/pages/login/components/SliderCaptchaModal', () => {
  return function MockSliderCaptchaModal({ open, onVerified }: any) {
    if (!open) {
      return null;
    }
    return (
      <button type="button" onClick={() => onVerified({ id: 'captcha-verification-id' })}>
        完成滑动验证
      </button>
    );
  };
});

describe('AccountProfilePage password change', () => {
  /**
   * 重置个人资料页测试依赖的 mock 状态。
   */
  beforeEach(() => {
    jest.clearAllMocks();
    mockChangeAdminPassword.mockResolvedValue(undefined);
    mockCurrentUser.mockResolvedValue({
      id: 1,
      username: 'admin',
      nickname: '管理员',
      phoneNumber: '13800000000',
    });
  });

  /**
   * 渲染个人资料页并打开修改密码弹窗。
   *
   * @returns 无返回值。
   */
  async function openPasswordModal(): Promise<void> {
    render(<AccountProfilePage />);

    expect(await screen.findByText('登录密码')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: '修改密码' }));
    expect(await screen.findByText('修改登录密码')).toBeTruthy();
  }

  it('renders password change entry and modal fields', async () => {
    await openPasswordModal();

    expect(screen.getByPlaceholderText('请输入原密码')).toBeTruthy();
    expect(screen.getByPlaceholderText('请输入新密码')).toBeTruthy();
    expect(screen.getByPlaceholderText('请再次输入新密码')).toBeTruthy();
  });

  it('blocks submit when confirm password is different', async () => {
    await openPasswordModal();

    fireEvent.change(screen.getByPlaceholderText('请输入原密码'), {
      target: { value: 'oldPassword123' },
    });
    fireEvent.change(screen.getByPlaceholderText('请输入新密码'), {
      target: { value: 'newPassword123' },
    });
    fireEvent.change(screen.getByPlaceholderText('请再次输入新密码'), {
      target: { value: 'otherPassword' },
    });
    fireEvent.click(screen.getByRole('button', { name: /确\s*定/ }));

    expect(await screen.findByText('两次输入的密码不一致')).toBeTruthy();
    expect(mockChangeAdminPassword).not.toHaveBeenCalled();
  });

  it('submits password change after captcha and redirects to login', async () => {
    await openPasswordModal();

    fireEvent.change(screen.getByPlaceholderText('请输入原密码'), {
      target: { value: 'oldPassword123' },
    });
    fireEvent.change(screen.getByPlaceholderText('请输入新密码'), {
      target: { value: 'newPassword123' },
    });
    fireEvent.change(screen.getByPlaceholderText('请再次输入新密码'), {
      target: { value: 'newPassword123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /确\s*定/ }));
    fireEvent.click(await screen.findByText('完成滑动验证'));

    await waitFor(() =>
      expect(mockChangeAdminPassword).toHaveBeenCalledWith({
        oldPassword: 'oldPassword123',
        newPassword: 'newPassword123',
        captchaVerificationId: 'captcha-verification-id',
      }),
    );
    expect(mockMessageSuccess).toHaveBeenCalledWith('密码修改成功，请重新登录');
    expect(mockClearAuth).toHaveBeenCalled();
    expect(mockSetInitialState).toHaveBeenCalledWith({ currentUser: undefined });
    expect(mockNavigate).toHaveBeenCalledWith('/user/login?redirect=%2Faccount%2Fprofile', {
      replace: true,
    });
  });
});
