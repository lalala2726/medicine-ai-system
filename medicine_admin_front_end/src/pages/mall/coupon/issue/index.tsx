import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { PageContainer } from '@ant-design/pro-components';
import {
  Alert,
  Avatar,
  Button,
  Col,
  DatePicker,
  Empty,
  Form,
  Input,
  Modal,
  Radio,
  Row,
  Space,
  Typography,
  message,
  Steps,
  Divider,
  theme,
} from 'antd';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import { createStyles } from 'antd-style';
import { type Dayjs } from 'dayjs';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import type { CaptchaVerificationResult } from '@/api/core/captcha';
import { issueCouponToUser, type MallCouponTypes } from '@/api/mall/coupon';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import SliderCaptchaModal from '@/pages/login/components/SliderCaptchaModal';
import { type UserTypes } from '@/api/system/user';
import {
  buildMallCouponSecondaryRoutePath,
  isSecondaryMenuRouteEnabled,
  routePaths,
} from '@/router/paths';
import CouponTemplateSelector from '../components/CouponTemplateSelector';
import CouponUserDrawerSelect from '../components/CouponUserDrawerSelect';

/**
 * 模板ID查询参数名称。
 */
const TEMPLATE_ID_QUERY_KEY = 'templateId';

/**
 * 默认发券目标类型。
 */
const DEFAULT_ISSUE_TARGET_TYPE: MallCouponTypes.CouponIssueTargetType = 'SPECIFIED';

/**
 * 发券目标类型选项。
 */
const ISSUE_TARGET_TYPE_OPTIONS = [
  { label: '发给指定用户', value: 'SPECIFIED' },
  { label: '发给全部用户', value: 'ALL' },
] satisfies Array<{
  label: string;
  value: MallCouponTypes.CouponIssueTargetType;
}>;

/**
 * 发券表单值。
 */
interface IssueFormValues {
  /** 模板ID */
  templateId: number;
  /** 发券目标类型 */
  issueTargetType: MallCouponTypes.CouponIssueTargetType;
  /** 用户ID列表 */
  userIds: number[];
  /** 生效时间 */
  effectiveTime: Dayjs;
  /** 失效时间 */
  expireTime: Dayjs;
  /** 发券备注 */
  remark?: string;
}

/**
 * 解析模板ID查询参数。
 * @param value 查询参数值。
 * @returns 模板ID。
 */
function parseTemplateId(value: string | null): number | undefined {
  if (!value) {
    return undefined;
  }
  const templateId = Number(value);
  if (!Number.isInteger(templateId) || templateId <= 0) {
    return undefined;
  }
  return templateId;
}

/**
 * 将 Dayjs 转换为后端接收的日期时间字符串。
 * @param value 日期对象。
 * @returns 标准日期时间字符串。
 */
function toDateTimeString(value: Dayjs): string {
  return value.format('YYYY-MM-DDTHH:mm:ss.SSSZ');
}

/**
 * 生成用户头像占位文本。
 * @param user 用户信息。
 * @returns 头像占位文本。
 */
function getAvatarFallbackText(user: UserTypes.UserListVo): string {
  return user.nickname?.trim()?.charAt(0) || user.username?.trim()?.charAt(0) || 'U';
}

const useStyles = createStyles(({ token }) => ({
  container: {
    paddingBottom: 80,
  },
  stepsWrapper: {
    padding: '20px 0',
    background: token.colorBgContainer,
    borderRadius: token.borderRadiusLG,
    marginBottom: 16,
    boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
    border: `1px solid ${token.colorBorderSecondary}`,
  },
  steps: {
    maxWidth: 800,
    margin: '0 auto',
  },
  formContent: {
    background: token.colorBgContainer,
    borderRadius: token.borderRadiusLG,
    padding: '24px',
    minHeight: 400,
    boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
    border: `1px solid ${token.colorBorderSecondary}`,
  },
  footer: {
    position: 'fixed',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: token.colorBgElevated,
    borderTop: `1px solid ${token.colorBorderSecondary}`,
    padding: '16px 24px',
    zIndex: 1000,
    backdropFilter: 'blur(8px)',
    boxShadow: '0 -2px 8px rgba(0,0,0,0.05)',
  },
  footerContent: {
    maxWidth: 1200,
    margin: '0 auto',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  sectionTitle: {
    marginBottom: 24,
    display: 'flex',
    alignItems: 'center',
    gap: 8,
  },
}));

/**
 * 防抖提交最小间隔（毫秒）。
 */
const SUBMIT_DEBOUNCE_MS = 1500;

/**
 * 优惠券发券页面。
 * @returns 发券页面节点。
 */
const MallCouponIssuePage: React.FC = () => {
  const { token } = theme.useToken();
  const { styles } = useStyles();
  const [activeTab, setActiveTab] = useState('template');
  const [messageApi, contextHolder] = message.useMessage();
  const [form] = Form.useForm<IssueFormValues>();
  const [submitting, setSubmitting] = useState(false);
  const [captchaOpen, setCaptchaOpen] = useState(false);
  const [pendingIssueValues, setPendingIssueValues] = useState<IssueFormValues | null>(null);
  const [selectedUsers, setSelectedUsers] = useState<UserTypes.UserListVo[]>([]);
  const lastSubmitTimeRef = useRef<number>(0);
  const navigate = useNavigate();
  const couponSecondaryRouteEnabled = isSecondaryMenuRouteEnabled(routePaths.mallCoupon);
  const [searchParams] = useSearchParams();
  const issueTargetType = Form.useWatch('issueTargetType', form) ?? DEFAULT_ISSUE_TARGET_TYPE;

  /**
   * 初始模板ID。
   */
  const initialTemplateId = useMemo(
    () => parseTemplateId(searchParams.get(TEMPLATE_ID_QUERY_KEY)),
    [searchParams],
  );

  /**
   * 初始化表单值。
   * @returns 无返回值。
   */
  const applyInitialFormValues = useCallback(() => {
    form.setFieldsValue({
      issueTargetType: DEFAULT_ISSUE_TARGET_TYPE,
      userIds: [],
      templateId: initialTemplateId,
    });
    setSelectedUsers([]);
  }, [form, initialTemplateId]);

  /**
   * 返回优惠券管理页面。
   * @returns 无返回值。
   */
  const handleBack = useCallback(() => {
    navigate(
      couponSecondaryRouteEnabled
        ? buildMallCouponSecondaryRoutePath('template')
        : routePaths.mallCoupon,
    );
  }, [couponSecondaryRouteEnabled, navigate]);

  /**
   * 处理发券提交。
   * @param values 表单值。
   * @param captchaVerificationId 验证码校验凭证。
   * @returns 无返回值。
   */
  const handleSubmit = useCallback(
    async (values: IssueFormValues, captchaVerificationId: string) => {
      const now = Date.now();
      if (now - lastSubmitTimeRef.current < SUBMIT_DEBOUNCE_MS) {
        return;
      }
      lastSubmitTimeRef.current = now;
      setSubmitting(true);
      try {
        const payload: MallCouponTypes.CouponIssueRequest = {
          templateId: Number(values.templateId),
          issueTargetType: values.issueTargetType,
          userIds:
            values.issueTargetType === 'SPECIFIED'
              ? values.userIds.map((userId) => Number(userId))
              : undefined,
          effectiveTime: toDateTimeString(values.effectiveTime),
          expireTime: toDateTimeString(values.expireTime),
          remark: values.remark?.trim() || undefined,
          captchaVerificationId,
        };
        await issueCouponToUser(payload);
        messageApi.success('发券任务已提交，系统正在后台为您派发');
        setTimeout(() => {
          navigate(
            couponSecondaryRouteEnabled
              ? buildMallCouponSecondaryRoutePath('template')
              : routePaths.mallCoupon,
          );
        }, 1500);
      } catch (error) {
        console.error('提交发券任务失败:', error);
        messageApi.error('提交发券任务失败，请稍后重试');
      } finally {
        setSubmitting(false);
        setPendingIssueValues(null);
      }
    },
    [couponSecondaryRouteEnabled, messageApi, navigate],
  );

  /**
   * 取消滑块验证。
   * @returns 无返回值。
   */
  const handleCaptchaCancel = useCallback(() => {
    setCaptchaOpen(false);
    setPendingIssueValues(null);
  }, []);

  /**
   * 滑块验证通过后提交发券请求。
   * @param captchaVerificationResult 验证码结果。
   * @returns 无返回值。
   */
  const handleCaptchaVerified = useCallback(
    async (captchaVerificationResult: CaptchaVerificationResult) => {
      const values = pendingIssueValues;
      setCaptchaOpen(false);
      if (!values) {
        return;
      }
      await handleSubmit(values, captchaVerificationResult.id);
    },
    [handleSubmit, pendingIssueValues],
  );

  /**
   * 同步指定用户列表。
   * @param users 用户基础信息列表。
   * @returns 无返回值。
   */
  const syncSelectedUsers = useCallback(
    (users: UserTypes.UserListVo[]) => {
      const normalizedUsers = users.filter((user) => user.id);
      setSelectedUsers(normalizedUsers);
      form.setFieldValue(
        'userIds',
        normalizedUsers
          .map((user) => Number(user.id))
          .filter((userId) => Number.isInteger(userId) && userId > 0),
      );
    },
    [form],
  );

  /**
   * 删除指定已选用户。
   * @param userId 用户ID。
   * @returns 无返回值。
   */
  const handleRemoveSelectedUser = useCallback(
    (userId: number) => {
      syncSelectedUsers(selectedUsers.filter((user) => user.id !== userId));
    },
    [selectedUsers, syncSelectedUsers],
  );

  /**
   * 清空全部已选用户。
   * @returns 无返回值。
   */
  const handleClearSelectedUsers = useCallback(() => {
    syncSelectedUsers([]);
  }, [syncSelectedUsers]);

  useEffect(() => {
    applyInitialFormValues();
  }, [applyInitialFormValues]);

  const stepItems = [
    { key: 'template', title: '选择模板', description: '选择待发放的优惠券' },
    { key: 'target', title: '发券对象', description: '设置发券的用户范围' },
    { key: 'rule', title: '有效期与备注', description: '设置有效时长及描述' },
  ];
  const activeStepIndex = stepItems.findIndex((item) => item.key === activeTab);
  const isFirstTab = activeTab === 'template';
  const isLastTab = activeTab === 'rule';

  const handlePrevious = () => {
    if (activeTab === 'rule') setActiveTab('target');
    else if (activeTab === 'target') setActiveTab('template');
  };

  const handleNext = async () => {
    try {
      if (activeTab === 'template') {
        await form.validateFields(['templateId']);
        setActiveTab('target');
      } else if (activeTab === 'target') {
        const type = form.getFieldValue('issueTargetType');
        if (type === 'SPECIFIED') {
          await form.validateFields(['userIds']);
        }
        setActiveTab('rule');
      }
    } catch {
      // 触发表单自带错误提示
    }
  };

  const manuallySubmit = async () => {
    if (submitting) {
      return;
    }
    try {
      const values = await form.validateFields();
      Modal.confirm({
        title: '确认提交发券任务',
        content: '提交后系统将在后台自动为目标用户派发优惠券，是否确认提交？',
        okText: '确认提交',
        cancelText: '取消',
        onOk: () => {
          setPendingIssueValues(values);
          setCaptchaOpen(true);
        },
      });
    } catch (e: any) {
      const errorFields = e.errorFields || [];
      if (errorFields.length > 0) {
        const name = errorFields[0].name[0];
        if (name === 'templateId') setActiveTab('template');
        else if (name === 'issueTargetType' || name === 'userIds') setActiveTab('target');
      }
    }
  };

  return (
    <PageContainer
      title="优惠券发券"
      subTitle="优惠券有效期在发券时明确设置，提交后系统将在后台自动为您完成派发"
      extra={[
        <Button key="back" onClick={handleBack}>
          返回优惠券管理
        </Button>,
      ]}
    >
      {contextHolder}

      <div className={styles.container}>
        <div className={styles.stepsWrapper}>
          <Steps
            current={activeStepIndex}
            items={stepItems}
            className={styles.steps}
            size="small"
            onChange={(current) => setActiveTab(stepItems[current].key)}
          />
        </div>

        <div className={styles.formContent}>
          <Form<IssueFormValues>
            form={form}
            layout="vertical"
            initialValues={{
              issueTargetType: DEFAULT_ISSUE_TARGET_TYPE,
              userIds: [],
            }}
          >
            <div
              className="step-content fade-in"
              style={{ display: activeTab === 'template' ? 'block' : 'none' }}
            >
              <Typography.Title level={5} className={styles.sectionTitle}>
                选择优惠券模板
              </Typography.Title>
              <Divider style={{ margin: '0 0 24px 0' }} />
              <Form.Item
                name="templateId"
                rules={[{ required: true, message: '请选择优惠券模板' }]}
              >
                <CouponTemplateSelector />
              </Form.Item>
            </div>

            <div
              className="step-content fade-in"
              style={{ display: activeTab === 'target' ? 'block' : 'none' }}
            >
              <Typography.Title level={5} className={styles.sectionTitle}>
                发券对象设置
              </Typography.Title>
              <Divider style={{ margin: '0 0 24px 0' }} />

              <Form.Item
                label="发券范围"
                name="issueTargetType"
                rules={[{ required: true, message: '请选择发券范围' }]}
              >
                <Radio.Group
                  optionType="button"
                  buttonStyle="solid"
                  options={ISSUE_TARGET_TYPE_OPTIONS}
                />
              </Form.Item>

              {issueTargetType === 'ALL' ? (
                <Alert
                  type="success"
                  showIcon
                  message="当前操作将发给全平台所有正常状态的用户，大批量发放等重度处理将由系统后台全自动完成。"
                />
              ) : (
                <div style={{ marginTop: 16 }}>
                  <Space style={{ marginBottom: 16 }}>
                    <CouponUserDrawerSelect
                      selectedUsers={selectedUsers}
                      onUsersChange={syncSelectedUsers}
                    />
                    <Button
                      type="link"
                      disabled={selectedUsers.length === 0}
                      onClick={handleClearSelectedUsers}
                    >
                      清空已选用户
                    </Button>
                    {selectedUsers.length > 0 && (
                      <Typography.Text type="secondary">
                        已选择 {selectedUsers.length} 名用户
                      </Typography.Text>
                    )}
                  </Space>

                  <Form.Item
                    name="userIds"
                    rules={[
                      { required: true, type: 'array', min: 1, message: '请至少选择一个用户' },
                    ]}
                  >
                    {selectedUsers.length > 0 ? (
                      <div
                        style={{
                          display: 'grid',
                          gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
                          gap: 16,
                        }}
                      >
                        {selectedUsers.map((user) => (
                          <div
                            key={user.id}
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: 12,
                              padding: '12px 16px',
                              border: '1px solid #f0f0f0',
                              borderRadius: 8,
                              background: '#fafafa',
                            }}
                          >
                            <Avatar src={user.avatar} size={48}>
                              {getAvatarFallbackText(user)}
                            </Avatar>
                            <div style={{ flex: 1, minWidth: 0 }}>
                              <Typography.Text strong ellipsis style={{ display: 'block' }}>
                                {user.nickname || user.username || `用户${user.id}`}
                              </Typography.Text>
                              <Typography.Text
                                type="secondary"
                                ellipsis
                                style={{ display: 'block', fontSize: 12 }}
                              >
                                账号：{user.username || '-'}
                              </Typography.Text>
                              <Typography.Text
                                type="secondary"
                                style={{ display: 'block', fontSize: 12 }}
                              >
                                ID：{user.id ?? '-'}
                              </Typography.Text>
                            </div>
                            <Button
                              type="text"
                              danger
                              onClick={() => {
                                handleRemoveSelectedUser(Number(user.id));
                              }}
                            >
                              删除
                            </Button>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div
                        style={{
                          padding: '32px 0',
                          border: '1px dashed #e8e8e8',
                          borderRadius: 8,
                          textAlign: 'center',
                          background: '#ffffff',
                        }}
                      >
                        <Empty
                          image={Empty.PRESENTED_IMAGE_SIMPLE}
                          description="暂未选择指定用户"
                        />
                      </div>
                    )}
                  </Form.Item>
                </div>
              )}
            </div>

            <div
              className="step-content fade-in"
              style={{ display: activeTab === 'rule' ? 'block' : 'none' }}
            >
              <Typography.Title level={5} className={styles.sectionTitle}>
                有效期与备注配置
              </Typography.Title>
              <Divider style={{ margin: '0 0 24px 0' }} />

              <Row gutter={24}>
                <Col xs={24} md={12}>
                  <Form.Item
                    label="生效时间"
                    name="effectiveTime"
                    rules={[{ required: true, message: '请选择生效时间' }]}
                  >
                    <DatePicker
                      showTime
                      format="YYYY-MM-DD HH:mm:ss"
                      style={{ width: '100%' }}
                      placeholder="请选择生效时间"
                    />
                  </Form.Item>
                </Col>

                <Col xs={24} md={12}>
                  <Form.Item
                    label="失效时间"
                    name="expireTime"
                    dependencies={['effectiveTime']}
                    rules={[
                      { required: true, message: '请选择失效时间' },
                      ({ getFieldValue }) => ({
                        validator(_, value?: Dayjs) {
                          const effectiveTime = getFieldValue('effectiveTime') as Dayjs | undefined;
                          if (!effectiveTime || !value) {
                            return Promise.resolve();
                          }
                          if (value.isAfter(effectiveTime)) {
                            return Promise.resolve();
                          }
                          return Promise.reject(new Error('失效时间必须晚于生效时间'));
                        },
                      }),
                    ]}
                  >
                    <DatePicker
                      showTime
                      format="YYYY-MM-DD HH:mm:ss"
                      style={{ width: '100%' }}
                      placeholder="请选择失效时间"
                    />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item label="发券备注" name="remark">
                <Input.TextArea
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  maxLength={200}
                  showCount
                  autoSize={{ minRows: 3, maxRows: 5 }}
                  placeholder="请输入发券备注，最多 200 字"
                />
              </Form.Item>
            </div>
          </Form>
        </div>
      </div>

      <div className={styles.footer}>
        <div className={styles.footerContent}>
          <Space>
            <Typography.Text type="secondary">
              当前步骤：
              <Typography.Text strong style={{ color: token.colorPrimary, marginLeft: 8 }}>
                第 {activeStepIndex + 1} 步 - {stepItems[activeStepIndex].title}
              </Typography.Text>
            </Typography.Text>
          </Space>
          <Space size="middle">
            <Button icon={<LeftOutlined />} onClick={handlePrevious} disabled={isFirstTab}>
              上一步
            </Button>

            {isLastTab ? (
              <PermissionButton
                type="primary"
                access={ADMIN_PERMISSIONS.mallCoupon.issue}
                onClick={manuallySubmit}
                loading={submitting}
                disabled={submitting}
              >
                提交发券任务
              </PermissionButton>
            ) : (
              <Button type="primary" onClick={handleNext} icon={<RightOutlined />}>
                下一步
              </Button>
            )}

            <Divider type="vertical" />
            <Button onClick={handleBack}>取消</Button>
          </Space>
        </div>
      </div>
      <SliderCaptchaModal
        open={captchaOpen}
        onCancel={handleCaptchaCancel}
        onVerified={handleCaptchaVerified}
      />
    </PageContainer>
  );
};

export default MallCouponIssuePage;
