import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { PageContainer } from '@ant-design/pro-components';
import { Button, Card, Form, Input, Modal, Space, Spin, Typography, message } from 'antd';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { CaptchaVerificationResult } from '@/api/core/captcha';
import {
  changeAdminPhone,
  currentUser as queryCurrentUser,
  sendAdminPhoneCode,
  updateAdminProfile,
} from '@/api/core/login';
import { AvatarUpload } from '@/components';
import { useThemeContext } from '@/contexts/ThemeContext';
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
  const { initialState, setInitialState } = useInitialState();
  const [messageApi, contextHolder] = message.useMessage();
  const [profileForm] = Form.useForm<ProfileFormValues>();
  const [phoneForm] = Form.useForm<PhoneFormValues>();
  const hasInitializedRef = useRef(false);
  const [profileEditing, setProfileEditing] = useState(false);
  const [phoneModalOpen, setPhoneModalOpen] = useState(false);
  const [pageLoading, setPageLoading] = useState(true);
  const [profileSaving, setProfileSaving] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [phoneSaving, setPhoneSaving] = useState(false);
  const [captchaOpen, setCaptchaOpen] = useState(false);
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
    setCaptchaOpen(false);
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
      setCaptchaOpen(true);
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
    setCaptchaOpen(false);
    setPendingPhoneNumber(null);
  }, []);

  /**
   * 滑动验证通过后发送手机验证码。
   *
   * @param captchaVerificationResult 滑动验证码校验结果。
   * @returns 无返回值。
   */
  const handleCaptchaVerified = useCallback(
    async (captchaVerificationResult: CaptchaVerificationResult): Promise<void> => {
      if (!pendingPhoneNumber) {
        setCaptchaOpen(false);
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
        setCaptchaOpen(false);
        setPendingPhoneNumber(null);
      }
    },
    [messageApi, pendingPhoneNumber],
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
        <Card bordered={false}>
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
                  默认只读，点击编辑后才可修改资料；手机号通过单独弹窗修改。
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

      <SliderCaptchaModal
        open={captchaOpen}
        onCancel={handleCaptchaCancel}
        onVerified={handleCaptchaVerified}
        isDark={isDark}
      />
    </PageContainer>
  );
};

export default AccountProfilePage;
