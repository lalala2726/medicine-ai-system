import { PASSWORD_INPUT_AUTOCOMPLETE, TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { PageContainer } from '@ant-design/pro-components';
import { Button, Card, Form, Input, Modal, Space, Spin, Typography, message } from 'antd';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { CaptchaVerificationResult } from '@/api/core/captcha';
import {
  changeAdminPassword,
  changeAdminPhone,
  currentUser as queryCurrentUser,
  sendAdminPhoneCode,
  updateAdminProfile,
} from '@/api/core/login';
import { AvatarUpload } from '@/components';
import { useThemeContext } from '@/contexts/ThemeContext';
import { useAuthActions } from '@/hooks/useAuth';
import { useInitialState } from '@/hooks/useInitialState';
import SliderCaptchaModal from '@/pages/login/components/SliderCaptchaModal';
import { routePaths } from '@/router/paths';
import { normalizeUserData } from '@/utils/userUtils';

const { Title, Text } = Typography;

/**
 * 手机号格式正则。
 */
const PHONE_NUMBER_PATTERN = /^1[3-9]\d{9}$/;

/**
 * 手机验证码倒计时秒数。
 */
const PHONE_CODE_COUNTDOWN_SECONDS = 60;

/**
 * 密码修改输入框禁用常见密码管理器识别的属性。
 */
const PASSWORD_CHANGE_DISABLE_AUTOFILL_PROPS = {
  'data-lpignore': 'true',
  'data-1p-ignore': 'true',
  'data-form-type': 'other',
} as const;

/**
 * 滑动验证码动作类型。
 */
type CaptchaAction = 'send-phone-code' | 'change-password';

/**
 * 个人资料表单值。
 */
interface ProfileFormValues {
  /** 昵称。 */
  nickname: string;
  /** 真实姓名。 */
  realName?: string;
  /** 邮箱。 */
  email?: string;
}

/**
 * 手机号修改表单值。
 */
interface PhoneFormValues {
  /** 新手机号。 */
  phoneNumber: string;
  /** 手机验证码。 */
  verificationCode: string;
}

/**
 * 密码修改表单值。
 */
interface PasswordFormValues {
  /** 原登录密码。 */
  oldPassword: string;
  /** 新登录密码。 */
  newPassword: string;
  /** 确认新登录密码。 */
  confirmPassword: string;
}

/**
 * 规范化可选文本值。
 *
 * @param value 原始文本值。
 * @returns 规范化后的文本值。
 */
function normalizeOptionalText(value?: string): string | undefined {
  const normalizedValue = value?.trim();
  return normalizedValue ? normalizedValue : undefined;
}

/**
 * 管理端个人资料页面。
 *
 * @returns 页面节点。
 */
const AccountProfilePage: React.FC = () => {
  const navigate = useNavigate();
  const { isDark } = useThemeContext();
  const { clearAuth } = useAuthActions();
  const { initialState, setInitialState } = useInitialState();
  const [messageApi, contextHolder] = message.useMessage();
  const [profileForm] = Form.useForm<ProfileFormValues>();
  const [phoneForm] = Form.useForm<PhoneFormValues>();
  const [passwordForm] = Form.useForm<PasswordFormValues>();
  const hasInitializedRef = useRef(false);
  const [profileEditing, setProfileEditing] = useState(false);
  const [phoneModalOpen, setPhoneModalOpen] = useState(false);
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [pageLoading, setPageLoading] = useState(true);
  const [profileSaving, setProfileSaving] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [phoneSaving, setPhoneSaving] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [captchaAction, setCaptchaAction] = useState<CaptchaAction | null>(null);
  const [pendingPhoneNumber, setPendingPhoneNumber] = useState<string | null>(null);
  const [countdownSeconds, setCountdownSeconds] = useState(0);
  const [avatarUrl, setAvatarUrl] = useState<string | undefined>();
  const [currentPhoneNumber, setCurrentPhoneNumber] = useState<string>('');
  const [currentUserSnapshot, setCurrentUserSnapshot] = useState<API.CurrentUser | null>(null);

  /**
   * 将当前用户资料回填到页面状态。
   *
   * @param currentUser 当前登录用户信息。
   * @param resetPhoneForm 是否重置手机号修改表单。
   * @returns 无返回值。
   */
  const applyCurrentUserData = useCallback(
    (currentUser: API.CurrentUser, resetPhoneForm: boolean): void => {
      setCurrentUserSnapshot(currentUser);
      setAvatarUrl(currentUser.avatar);
      setCurrentPhoneNumber(currentUser.phoneNumber || '');
      profileForm.setFieldsValue({
        nickname: currentUser.nickname || currentUser.username || '',
        realName: currentUser.realName,
        email: currentUser.email,
      });
      if (resetPhoneForm) {
        phoneForm.setFieldsValue({
          phoneNumber: '',
          verificationCode: '',
        });
      }
    },
    [phoneForm, profileForm],
  );

  /**
   * 刷新当前登录用户信息并同步全局状态。
   *
   * @param resetPhoneForm 是否重置手机号修改表单。
   * @returns 当前登录用户原始信息。
   */
  const refreshCurrentUserInfo = useCallback(
    async (resetPhoneForm: boolean): Promise<API.CurrentUser> => {
      const currentUser = await queryCurrentUser();
      const normalizedUser = normalizeUserData(currentUser);
      setInitialState({
        currentUser: normalizedUser,
      });
      applyCurrentUserData(currentUser, resetPhoneForm);
      return currentUser;
    },
    [applyCurrentUserData, setInitialState],
  );

  /**
   * 加载个人资料页面数据。
   *
   * @returns 无返回值。
   */
  useEffect(() => {
    if (hasInitializedRef.current) {
      return;
    }
    hasInitializedRef.current = true;

    const initializePage = async (): Promise<void> => {
      try {
        setPageLoading(true);
        if (initialState.currentUser) {
          applyCurrentUserData(initialState.currentUser, true);
          return;
        }
        await refreshCurrentUserInfo(true);
      } catch (error) {
        console.error('加载个人资料失败:', error);
        messageApi.error('加载个人资料失败');
      } finally {
        setPageLoading(false);
      }
    };

    void initializePage();
  }, [applyCurrentUserData, initialState.currentUser, messageApi, refreshCurrentUserInfo]);

  useEffect(() => {
    if (countdownSeconds <= 0) {
      return;
    }

    const timer = window.setTimeout(() => {
      setCountdownSeconds((previousSeconds) => Math.max(previousSeconds - 1, 0));
    }, 1000);

    return () => {
      window.clearTimeout(timer);
    };
  }, [countdownSeconds]);

  /**
   * 处理头像变更。
   *
   * @param nextAvatarUrl 新头像地址。
   * @returns 无返回值。
   */
  const handleAvatarChange = useCallback((nextAvatarUrl: string): void => {
    setAvatarUrl(nextAvatarUrl);
  }, []);

  /**
   * 开启资料编辑态。
   *
   * @returns 无返回值。
   */
  const handleStartProfileEdit = useCallback((): void => {
    setProfileEditing(true);
  }, []);

  /**
   * 取消资料编辑并恢复当前登录用户资料。
   *
   * @returns 无返回值。
   */
  const handleCancelProfileEdit = useCallback((): void => {
    if (currentUserSnapshot) {
      applyCurrentUserData(currentUserSnapshot, false);
    }
    setProfileEditing(false);
  }, [applyCurrentUserData, currentUserSnapshot]);

  /**
   * 保存个人资料。
   *
   * @returns 无返回值。
   */
  const handleSaveProfile = useCallback(async (): Promise<void> => {
    try {
      const values = await profileForm.validateFields();
      setProfileSaving(true);
      await updateAdminProfile({
        avatar: normalizeOptionalText(avatarUrl),
        nickname: values.nickname.trim(),
        realName: normalizeOptionalText(values.realName),
        email: normalizeOptionalText(values.email),
      });
      await refreshCurrentUserInfo(false);
      setProfileEditing(false);
      messageApi.success('个人资料保存成功');
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      console.error('保存个人资料失败:', error);
    } finally {
      setProfileSaving(false);
    }
  }, [avatarUrl, messageApi, profileForm, refreshCurrentUserInfo]);

  /**
   * 校验发送验证码时的新手机号。
   *
   * @returns 合法的新手机号。
   */
  const validatePhoneNumberForSend = useCallback(async (): Promise<string | null> => {
    const values = await phoneForm.validateFields(['phoneNumber']);
    const normalizedPhoneNumber = values.phoneNumber.trim();
    if (!PHONE_NUMBER_PATTERN.test(normalizedPhoneNumber)) {
      return null;
    }
    if (normalizedPhoneNumber === currentPhoneNumber.trim()) {
      messageApi.warning('新手机号不能与当前手机号相同');
      return null;
    }
    return normalizedPhoneNumber;
  }, [currentPhoneNumber, messageApi, phoneForm]);

  /**
   * 打开手机号修改弹窗。
   *
   * @returns 无返回值。
   */
  const handleOpenPhoneModal = useCallback((): void => {
    setPhoneModalOpen(true);
  }, []);

  /**
   * 关闭手机号修改弹窗并清空临时状态。
   *
   * @returns 无返回值。
   */
  const handleCancelPhoneEdit = useCallback((): void => {
    setPhoneModalOpen(false);
    setCountdownSeconds(0);
    setCaptchaAction(null);
    setPendingPhoneNumber(null);
    phoneForm.setFieldsValue({
      phoneNumber: '',
      verificationCode: '',
    });
  }, [phoneForm]);

  /**
   * 打开发送验证码的滑动验证弹层。
   *
   * @returns 无返回值。
   */
  const handleOpenCaptcha = useCallback(async (): Promise<void> => {
    try {
      const normalizedPhoneNumber = await validatePhoneNumberForSend();
      if (!normalizedPhoneNumber) {
        return;
      }
      setPendingPhoneNumber(normalizedPhoneNumber);
      setCaptchaAction('send-phone-code');
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      console.error('打开验证码弹层失败:', error);
    }
  }, [validatePhoneNumberForSend]);

  /**
   * 取消发送验证码的滑动验证。
   *
   * @returns 无返回值。
   */
  const handleCaptchaCancel = useCallback((): void => {
    setCaptchaAction(null);
    setPendingPhoneNumber(null);
  }, []);

  /**
   * 打开密码修改弹窗。
   *
   * @returns 无返回值。
   */
  const handleOpenPasswordModal = useCallback((): void => {
    setPasswordModalOpen(true);
  }, []);

  /**
   * 关闭密码修改弹窗并清空临时状态。
   *
   * @returns 无返回值。
   */
  const handleCancelPasswordEdit = useCallback((): void => {
    setPasswordModalOpen(false);
    setCaptchaAction(null);
    passwordForm.resetFields();
  }, [passwordForm]);

  /**
   * 构建密码修改成功后的登录页地址。
   *
   * @returns 带个人资料页 redirect 参数的登录页地址。
   */
  const buildPasswordChangeLoginPath = useCallback((): string => {
    const searchParams = new URLSearchParams({
      redirect: routePaths.accountProfile,
    });
    return `${routePaths.login}?${searchParams.toString()}`;
  }, []);

  /**
   * 清理本地登录态并跳转登录页。
   *
   * @returns 无返回值。
   */
  const clearAuthAndRedirectToLogin = useCallback((): void => {
    clearAuth();
    setInitialState({ currentUser: undefined });
    navigate(buildPasswordChangeLoginPath(), { replace: true });
  }, [buildPasswordChangeLoginPath, clearAuth, navigate, setInitialState]);

  /**
   * 提交密码修改前打开滑动验证码。
   *
   * @returns 无返回值。
   */
  const handleSubmitPassword = useCallback(async (): Promise<void> => {
    try {
      await passwordForm.validateFields();
      setCaptchaAction('change-password');
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      console.error('打开密码修改验证码失败:', error);
    }
  }, [passwordForm]);

  /**
   * 滑动验证通过后提交密码修改。
   *
   * @param captchaVerificationResult 滑动验证码校验结果。
   * @returns 无返回值。
   */
  const handlePasswordCaptchaVerified = useCallback(
    async (captchaVerificationResult: CaptchaVerificationResult): Promise<void> => {
      try {
        const values = await passwordForm.validateFields();
        setPasswordSaving(true);
        await changeAdminPassword({
          oldPassword: values.oldPassword.trim(),
          newPassword: values.newPassword.trim(),
          captchaVerificationId: captchaVerificationResult.id,
        });
        passwordForm.resetFields();
        setPasswordModalOpen(false);
        messageApi.success('密码修改成功，请重新登录');
        clearAuthAndRedirectToLogin();
      } catch (error: any) {
        if (error?.errorFields) {
          return;
        }
        console.error('修改密码失败:', error);
      } finally {
        setPasswordSaving(false);
        setCaptchaAction(null);
      }
    },
    [clearAuthAndRedirectToLogin, messageApi, passwordForm],
  );

  /**
   * 滑动验证通过后按当前动作继续执行。
   *
   * @param captchaVerificationResult 滑动验证码校验结果。
   * @returns 无返回值。
   */
  const handleCaptchaVerified = useCallback(
    async (captchaVerificationResult: CaptchaVerificationResult): Promise<void> => {
      if (captchaAction === 'change-password') {
        await handlePasswordCaptchaVerified(captchaVerificationResult);
        return;
      }

      if (captchaAction !== 'send-phone-code' || !pendingPhoneNumber) {
        setCaptchaAction(null);
        setPendingPhoneNumber(null);
        return;
      }

      try {
        setSendingCode(true);
        await sendAdminPhoneCode({
          phoneNumber: pendingPhoneNumber,
          captchaVerificationId: captchaVerificationResult.id,
        });
        setCountdownSeconds(PHONE_CODE_COUNTDOWN_SECONDS);
        messageApi.success('验证码已发送，请查看后端日志');
      } catch (error) {
        console.error('发送手机号验证码失败:', error);
      } finally {
        setSendingCode(false);
        setCaptchaAction(null);
        setPendingPhoneNumber(null);
      }
    },
    [captchaAction, handlePasswordCaptchaVerified, messageApi, pendingPhoneNumber],
  );

  /**
   * 提交手机号修改。
   *
   * @returns 无返回值。
   */
  const handleSubmitPhone = useCallback(async (): Promise<void> => {
    try {
      const values = await phoneForm.validateFields();
      const normalizedPhoneNumber = values.phoneNumber.trim();
      if (normalizedPhoneNumber === currentPhoneNumber.trim()) {
        messageApi.warning('新手机号不能与当前手机号相同');
        return;
      }

      setPhoneSaving(true);
      await changeAdminPhone({
        phoneNumber: normalizedPhoneNumber,
        verificationCode: values.verificationCode.trim(),
      });
      setCountdownSeconds(0);
      await refreshCurrentUserInfo(true);
      setPhoneModalOpen(false);
      messageApi.success('手机号修改成功');
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      console.error('修改手机号失败:', error);
    } finally {
      setPhoneSaving(false);
    }
  }, [currentPhoneNumber, messageApi, phoneForm, refreshCurrentUserInfo]);

  /**
   * 获取发送验证码按钮文案。
   *
   * @returns 按钮文案。
   */
  const getSendCodeButtonText = useCallback((): string => {
    if (sendingCode) {
      return '发送中...';
    }
    if (countdownSeconds > 0) {
      return `${countdownSeconds}s 后重新发送`;
    }
    return '发送验证码';
  }, [countdownSeconds, sendingCode]);

  return (
    <PageContainer title="个人资料" onBack={() => navigate(routePaths.analytics)}>
      {contextHolder}
      <Spin spinning={pageLoading}>
        <Card variant="borderless">
          <Space direction="vertical" size={18} style={{ width: '100%' }}>
            <div
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                justifyContent: 'space-between',
                gap: 16,
              }}
            >
              <div>
                <Title level={4} style={{ marginBottom: 8 }}>
                  资料设置
                </Title>
                <Text type="secondary">
                  默认只读，点击编辑后才可修改资料；手机号和登录密码通过单独弹窗修改。
                </Text>
              </div>
              {!profileEditing && (
                <Button onClick={handleStartProfileEdit} type="primary">
                  编辑资料
                </Button>
              )}
            </div>
            <div>
              <Text strong style={{ display: 'block', marginBottom: 12 }}>
                头像
              </Text>
              <AvatarUpload
                value={avatarUrl}
                onChange={handleAvatarChange}
                shapes={['circle']}
                uploadText="上传头像"
                disabled={!profileEditing}
              />
              <Text type="secondary" style={{ display: 'block', marginTop: 12 }}>
                {profileEditing
                  ? '支持 JPG、PNG 等图片格式，大小不超过 2MB。'
                  : '点击“编辑资料”后可更换头像。'}
              </Text>
            </div>
            <Form form={profileForm} layout="vertical">
              <Form.Item
                label="昵称"
                name="nickname"
                rules={[
                  { required: true, message: '请输入昵称' },
                  { max: 50, message: '昵称长度不能超过50个字符' },
                ]}
              >
                <Input
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  placeholder="请输入昵称"
                  maxLength={50}
                  disabled={!profileEditing}
                />
              </Form.Item>
              <Form.Item
                label="真实姓名"
                name="realName"
                rules={[{ max: 50, message: '真实姓名长度不能超过50个字符' }]}
              >
                <Input
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  placeholder="请输入真实姓名"
                  maxLength={50}
                  disabled={!profileEditing}
                />
              </Form.Item>
              <Form.Item
                label="邮箱"
                name="email"
                rules={[{ type: 'email', message: '请输入正确的邮箱地址' }]}
              >
                <Input
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  placeholder="请输入邮箱"
                  maxLength={100}
                  disabled={!profileEditing}
                />
              </Form.Item>
              <Form.Item label="手机号">
                <Space.Compact style={{ width: '100%' }}>
                  <Input
                    autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                    value={currentPhoneNumber || '未绑定手机号'}
                    disabled
                  />
                  <Button onClick={handleOpenPhoneModal}>修改手机号</Button>
                </Space.Compact>
              </Form.Item>
              <Form.Item label="登录密码">
                <Space.Compact style={{ width: '100%' }}>
                  <Input
                    autoComplete={PASSWORD_INPUT_AUTOCOMPLETE}
                    value="已设置登录密码"
                    disabled
                  />
                  <Button onClick={handleOpenPasswordModal}>修改密码</Button>
                </Space.Compact>
              </Form.Item>
              {profileEditing && (
                <Space>
                  <Button onClick={handleCancelProfileEdit}>取消</Button>
                  <Button
                    type="primary"
                    loading={profileSaving}
                    onClick={() => void handleSaveProfile()}
                  >
                    保存资料
                  </Button>
                </Space>
              )}
            </Form>
          </Space>
        </Card>
      </Spin>

      <Modal
        title="修改手机号"
        open={phoneModalOpen}
        onCancel={handleCancelPhoneEdit}
        footer={null}
        forceRender
        destroyOnHidden
      >
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Text type="secondary">发送验证码前需要先完成滑动验证，验证码会打印在后端日志中。</Text>
          <Form form={phoneForm} layout="vertical">
            <Form.Item label="当前手机号">
              <Input
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                value={currentPhoneNumber || '未绑定手机号'}
                disabled
              />
            </Form.Item>
            <Form.Item
              label="新手机号"
              name="phoneNumber"
              rules={[
                { required: true, message: '请输入新手机号' },
                { pattern: PHONE_NUMBER_PATTERN, message: '请输入正确的手机号' },
              ]}
            >
              <Input
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                placeholder="请输入新手机号"
                maxLength={11}
              />
            </Form.Item>
            <Form.Item label="验证码" required>
              <Space.Compact style={{ width: '100%' }}>
                <Form.Item
                  name="verificationCode"
                  noStyle
                  rules={[
                    { required: true, message: '请输入验证码' },
                    { pattern: /^\d{6}$/, message: '请输入6位验证码' },
                  ]}
                >
                  <Input
                    autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                    placeholder="请输入验证码"
                    maxLength={6}
                  />
                </Form.Item>
                <Button
                  onClick={() => void handleOpenCaptcha()}
                  disabled={sendingCode || countdownSeconds > 0}
                >
                  {getSendCodeButtonText()}
                </Button>
              </Space.Compact>
            </Form.Item>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={handleCancelPhoneEdit}>取消</Button>
              <Button type="primary" loading={phoneSaving} onClick={() => void handleSubmitPhone()}>
                确定
              </Button>
            </Space>
          </Form>
        </Space>
      </Modal>

      <Modal
        title="修改登录密码"
        open={passwordModalOpen}
        onCancel={handleCancelPasswordEdit}
        footer={null}
        forceRender
        destroyOnHidden
      >
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Text type="secondary">修改成功后当前账号会退出登录，需要使用新密码重新登录。</Text>
          <Form form={passwordForm} layout="vertical">
            <Form.Item
              label="原密码"
              name="oldPassword"
              rules={[
                { required: true, message: '请输入原密码' },
                { min: 6, max: 20, message: '密码长度为6-20个字符' },
              ]}
            >
              <Input.Password
                autoComplete={PASSWORD_INPUT_AUTOCOMPLETE}
                {...PASSWORD_CHANGE_DISABLE_AUTOFILL_PROPS}
                id="medicine-account-password-field-1"
                name="medicine_account_password_field_1"
                placeholder="请输入原密码"
                maxLength={20}
              />
            </Form.Item>
            <Form.Item
              label="新密码"
              name="newPassword"
              rules={[
                { required: true, message: '请输入新密码' },
                { min: 6, max: 20, message: '密码长度为6-20个字符' },
              ]}
            >
              <Input.Password
                autoComplete={PASSWORD_INPUT_AUTOCOMPLETE}
                {...PASSWORD_CHANGE_DISABLE_AUTOFILL_PROPS}
                id="medicine-account-password-field-2"
                name="medicine_account_password_field_2"
                placeholder="请输入新密码"
                maxLength={20}
              />
            </Form.Item>
            <Form.Item
              label="确认新密码"
              name="confirmPassword"
              dependencies={['newPassword']}
              rules={[
                { required: true, message: '请再次输入新密码' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('newPassword') === value) {
                      return Promise.resolve();
                    }
                    return Promise.reject(new Error('两次输入的密码不一致'));
                  },
                }),
              ]}
            >
              <Input.Password
                autoComplete={PASSWORD_INPUT_AUTOCOMPLETE}
                {...PASSWORD_CHANGE_DISABLE_AUTOFILL_PROPS}
                id="medicine-account-password-field-3"
                name="medicine_account_password_field_3"
                placeholder="请再次输入新密码"
                maxLength={20}
              />
            </Form.Item>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={handleCancelPasswordEdit}>取消</Button>
              <Button
                type="primary"
                loading={passwordSaving}
                disabled={captchaAction === 'change-password'}
                onClick={() => void handleSubmitPassword()}
              >
                确定
              </Button>
            </Space>
          </Form>
        </Space>
      </Modal>

      <SliderCaptchaModal
        open={captchaAction !== null}
        onCancel={handleCaptchaCancel}
        onVerified={handleCaptchaVerified}
        isDark={isDark}
      />
    </PageContainer>
  );
};

export default AccountProfilePage;
