import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import {
  ExclamationCircleOutlined,
  MinusCircleOutlined,
  PlusCircleOutlined,
  UnlockOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Col,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Space,
  Spin,
  Tag,
  Typography,
  message,
  theme,
} from 'antd';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import type { CaptchaVerificationResult } from '@/api/core/captcha';
import {
  changeUserWallet,
  freezeUserWallet,
  getUserWallet,
  type UserTypes,
  unfreezeUserWallet,
} from '@/api/system/user';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import SliderCaptchaModal from '@/pages/login/components/SliderCaptchaModal';
import styles from './WalletBalanceTab.module.less';

const { Title, Text } = Typography;

/**
 * 钱包余额区域属性。
 */
interface WalletBalanceTabProps {
  /** 用户ID。 */
  userId: number;
  /** 外层容器内边距。 */
  containerPadding?: React.CSSProperties['padding'];
}

/**
 * 钱包状态说明映射。
 */
const WALLET_STATUS_META_MAP = {
  normal: {
    tagColor: 'success',
    tagText: '正常',
    title: '钱包状态正常',
    description: '当前钱包可以正常进行余额相关操作。',
  },
  frozen: {
    tagColor: 'error',
    tagText: '已冻结',
    title: '钱包已冻结',
    description: '冻结期间用户无法使用钱包完成交易，请先解冻后再恢复正常使用。',
  },
} as const;

/**
 * 管理端单次钱包调账最大金额。
 */
const WALLET_CHANGE_MAX_AMOUNT = 100000;

/**
 * 钱包敏感操作类型。
 */
type WalletCaptchaAction = 'add' | 'deduct' | 'freeze' | 'unfreeze';

/**
 * 钱包调账表单值。
 */
interface WalletChangeFormValues {
  /** 调账金额。 */
  amount: number;
  /** 操作原因。 */
  reason: string;
}

/**
 * 钱包冻结或解冻表单值。
 */
interface WalletReasonFormValues {
  /** 操作原因。 */
  reason: string;
}

/**
 * 等待滑块验证码通过后执行的钱包操作。
 */
interface PendingWalletOperation {
  /** 钱包操作类型。 */
  action: WalletCaptchaAction;
  /** 操作金额，仅调账操作需要。 */
  amount?: number;
  /** 操作原因。 */
  reason: string;
}

/**
 * 格式化金额文本。
 * @param value 原始金额。
 * @returns 金额展示文本。
 */
function formatCurrency(value?: string | number): string {
  if (value === undefined || value === null || value === '') {
    return '0.00';
  }
  const amountValue = Number(value);
  if (Number.isNaN(amountValue)) {
    return '0.00';
  }
  return amountValue.toFixed(2);
}

/**
 * 钱包余额组件。
 * @param props 组件属性。
 * @returns 钱包余额组件节点。
 */
const WalletBalanceTab: React.FC<WalletBalanceTabProps> = ({
  userId,
  containerPadding = '24px 0',
}) => {
  const { token } = theme.useToken();
  const [loading, setLoading] = useState(false);
  const [walletData, setWalletData] = useState<UserTypes.UserWalletVo | null>(null);
  const [addModalVisible, setAddModalVisible] = useState(false);
  const [deductModalVisible, setDeductModalVisible] = useState(false);
  const [freezeModalVisible, setFreezeModalVisible] = useState(false);
  const [unfreezeModalVisible, setUnfreezeModalVisible] = useState(false);
  const [captchaOpen, setCaptchaOpen] = useState(false);
  const [pendingOperation, setPendingOperation] = useState<PendingWalletOperation | null>(null);
  const [walletActionSubmitting, setWalletActionSubmitting] = useState(false);
  const [addForm] = Form.useForm();
  const [deductForm] = Form.useForm();
  const [freezeForm] = Form.useForm();
  const [unfreezeForm] = Form.useForm();
  const [messageApi, contextHolder] = message.useMessage();

  const addAmount = Form.useWatch('amount', addForm);
  const deductAmount = Form.useWatch('amount', deductForm);

  /**
   * 加载用户钱包数据。
   * @returns 无返回值。
   */
  const fetchWalletData = useCallback(async () => {
    try {
      setLoading(true);
      const result = await getUserWallet(userId);
      setWalletData(result);
    } catch (error) {
      console.error('获取钱包信息失败:', error);
      messageApi.error('获取钱包信息失败');
    } finally {
      setLoading(false);
    }
  }, [messageApi, userId]);

  useEffect(() => {
    if (userId) {
      void fetchWalletData();
    }
  }, [fetchWalletData, userId]);

  /**
   * 打开增加余额弹窗。
   * @returns 无返回值。
   */
  const handleOpenAddModal = () => {
    addForm.resetFields();
    addForm.setFieldsValue({
      amount: undefined,
      reason: undefined,
    });
    setAddModalVisible(true);
  };

  /**
   * 打开扣减余额弹窗。
   * @returns 无返回值。
   */
  const handleOpenDeductModal = () => {
    deductForm.resetFields();
    deductForm.setFieldsValue({
      amount: undefined,
      reason: undefined,
    });
    setDeductModalVisible(true);
  };

  /**
   * 关闭增加余额弹窗。
   * @returns 无返回值。
   */
  const handleCloseAddModal = useCallback(() => {
    setAddModalVisible(false);
    setPendingOperation((current) => (current?.action === 'add' ? null : current));
    addForm.resetFields();
  }, [addForm]);

  /**
   * 关闭扣减余额弹窗。
   * @returns 无返回值。
   */
  const handleCloseDeductModal = useCallback(() => {
    setDeductModalVisible(false);
    setPendingOperation((current) => (current?.action === 'deduct' ? null : current));
    deductForm.resetFields();
  }, [deductForm]);

  /**
   * 关闭冻结钱包弹窗。
   * @returns 无返回值。
   */
  const handleCloseFreezeModal = useCallback(() => {
    setFreezeModalVisible(false);
    setPendingOperation((current) => (current?.action === 'freeze' ? null : current));
    freezeForm.resetFields();
  }, [freezeForm]);

  /**
   * 关闭解冻钱包弹窗。
   * @returns 无返回值。
   */
  const handleCloseUnfreezeModal = useCallback(() => {
    setUnfreezeModalVisible(false);
    setPendingOperation((current) => (current?.action === 'unfreeze' ? null : current));
    unfreezeForm.resetFields();
  }, [unfreezeForm]);

  /**
   * 校验表单后打开钱包敏感操作滑块验证码。
   * @param action 钱包操作类型。
   * @returns 无返回值。
   */
  const handleRequestWalletCaptcha = async (action: WalletCaptchaAction) => {
    try {
      if (action === 'add') {
        const values = (await addForm.validateFields()) as WalletChangeFormValues;
        setPendingOperation({ action, amount: values.amount, reason: values.reason });
      }
      if (action === 'deduct') {
        const values = (await deductForm.validateFields()) as WalletChangeFormValues;
        setPendingOperation({ action, amount: values.amount, reason: values.reason });
      }
      if (action === 'freeze') {
        const values = (await freezeForm.validateFields()) as WalletReasonFormValues;
        setPendingOperation({ action, reason: values.reason });
      }
      if (action === 'unfreeze') {
        const values = (await unfreezeForm.validateFields()) as WalletReasonFormValues;
        setPendingOperation({ action, reason: values.reason });
      }
      setCaptchaOpen(true);
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      console.error('钱包操作表单校验失败:', error);
      messageApi.error(error?.message || '请检查钱包操作表单');
    }
  };

  /**
   * 执行已经通过滑块验证码的钱包操作。
   * @param operation 待执行的钱包操作。
   * @param captchaVerificationId 验证码校验凭证。
   * @returns 无返回值。
   */
  const executeVerifiedWalletOperation = useCallback(
    async (operation: PendingWalletOperation, captchaVerificationId: string) => {
      try {
        setWalletActionSubmitting(true);
        if (operation.action === 'add') {
          const data: UserTypes.WalletChangeRequest = {
            userId: String(userId),
            amount: String(operation.amount),
            operationType: 1,
            reason: operation.reason,
            captchaVerificationId,
          };
          await changeUserWallet(data);
          messageApi.success('增加余额成功');
          handleCloseAddModal();
        }
        if (operation.action === 'deduct') {
          const data: UserTypes.WalletChangeRequest = {
            userId: String(userId),
            amount: String(operation.amount),
            operationType: 2,
            reason: operation.reason,
            captchaVerificationId,
          };
          await changeUserWallet(data);
          messageApi.success('扣减余额成功');
          handleCloseDeductModal();
        }
        if (operation.action === 'freeze') {
          const data: UserTypes.FreezeOrUnUserWalletRequest = {
            userId: String(userId),
            reason: operation.reason,
            captchaVerificationId,
          };
          await freezeUserWallet(data);
          messageApi.success('冻结钱包成功');
          handleCloseFreezeModal();
        }
        if (operation.action === 'unfreeze') {
          const data: UserTypes.FreezeOrUnUserWalletRequest = {
            userId: String(userId),
            reason: operation.reason,
            captchaVerificationId,
          };
          await unfreezeUserWallet(data);
          messageApi.success('解冻钱包成功');
          handleCloseUnfreezeModal();
        }
        await fetchWalletData();
      } catch (error: any) {
        console.error('钱包操作失败:', error);
        messageApi.error(error?.message || '钱包操作失败');
      } finally {
        setWalletActionSubmitting(false);
        setPendingOperation(null);
      }
    },
    [
      fetchWalletData,
      handleCloseAddModal,
      handleCloseDeductModal,
      handleCloseFreezeModal,
      handleCloseUnfreezeModal,
      messageApi,
      userId,
    ],
  );

  /**
   * 取消钱包敏感操作滑块验证码。
   * @returns 无返回值。
   */
  const handleCaptchaCancel = useCallback(() => {
    setCaptchaOpen(false);
    setPendingOperation(null);
  }, []);

  /**
   * 滑块验证码通过后执行钱包敏感操作。
   * @param captchaVerificationResult 验证码校验结果。
   * @returns 无返回值。
   */
  const handleCaptchaVerified = useCallback(
    async (captchaVerificationResult: CaptchaVerificationResult) => {
      const operation = pendingOperation;
      setCaptchaOpen(false);
      if (!operation) {
        return;
      }
      await executeVerifiedWalletOperation(operation, captchaVerificationResult.id);
    },
    [executeVerifiedWalletOperation, pendingOperation],
  );

  /** 当前钱包是否已冻结。 */
  const isFrozen = walletData?.status === 1;
  /** 当前钱包可用余额数值。 */
  const currentBalance = Number(walletData?.balance || 0);
  /** 增加余额弹窗内输入的金额。 */
  const addAmountValue = Number(addAmount || 0);
  /** 扣减余额弹窗内输入的金额。 */
  const deductAmountValue = Number(deductAmount || 0);

  /**
   * 计算增加余额后的余额。
   * @returns 增加余额后的余额。
   */
  const nextAddBalance = useMemo(() => {
    if (!addAmountValue) {
      return currentBalance;
    }
    return currentBalance + addAmountValue;
  }, [addAmountValue, currentBalance]);

  /**
   * 计算扣减余额后的余额。
   * @returns 扣减余额后的余额。
   */
  const nextDeductBalance = useMemo(() => {
    if (!deductAmountValue) {
      return currentBalance;
    }
    return currentBalance - deductAmountValue;
  }, [deductAmountValue, currentBalance]);

  /** 当前钱包状态展示信息。 */
  const walletStatusMeta = isFrozen ? WALLET_STATUS_META_MAP.frozen : WALLET_STATUS_META_MAP.normal;

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '56px 0' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <>
      {contextHolder}
      <div className={styles.walletPage} style={{ padding: containerPadding }}>
        <section className={styles.summaryPanel}>
          <div className={styles.summaryHeader}>
            <div>
              <Text type="secondary">用户钱包</Text>
              <Title level={4} className={styles.summaryTitle}>
                余额概览
              </Title>
            </div>
            <Tag color={walletStatusMeta.tagColor} className={styles.statusTag}>
              {walletStatusMeta.tagText}
            </Tag>
          </div>

          <div className={styles.balanceBlock}>
            <Text type="secondary">可用余额</Text>
            <div className={styles.balanceLine}>
              <span className={styles.balanceValue}>{formatCurrency(walletData?.balance)}</span>
              <span className={styles.balanceUnit}>元</span>
            </div>
            <Text type="secondary" className={styles.statusDescription}>
              {walletStatusMeta.description}
            </Text>
          </div>

          <div className={styles.statList}>
            <div className={styles.statItem}>
              <Text type="secondary">累计入账</Text>
              <div className={styles.metricValue}>¥{formatCurrency(walletData?.totalIncome)}</div>
            </div>
            <div className={styles.statItem}>
              <Text type="secondary">累计支出</Text>
              <div className={styles.metricValue}>¥{formatCurrency(walletData?.totalExpend)}</div>
            </div>
            <div className={styles.statItem}>
              <Text type="secondary">最近更新时间</Text>
              <div className={styles.metricValue}>{walletData?.updatedAt || '-'}</div>
            </div>
          </div>
        </section>

        <section className={styles.sectionPanel}>
          <div className={styles.sectionHeader}>
            <div>
              <Title level={5} className={styles.sectionTitle}>
                快捷操作
              </Title>
            </div>
          </div>

          <div className={styles.actionBar}>
            <PermissionButton
              type="primary"
              className={styles.actionButton}
              access={ADMIN_PERMISSIONS.systemUser.update}
              onClick={handleOpenAddModal}
            >
              增加余额
            </PermissionButton>
            <PermissionButton
              danger
              className={styles.actionButton}
              access={ADMIN_PERMISSIONS.systemUser.update}
              onClick={handleOpenDeductModal}
            >
              扣减余额
            </PermissionButton>
            <PermissionButton
              danger={!isFrozen}
              type="default"
              className={styles.actionButton}
              access={ADMIN_PERMISSIONS.systemUser.update}
              onClick={() =>
                isFrozen ? setUnfreezeModalVisible(true) : setFreezeModalVisible(true)
              }
            >
              {isFrozen ? '解冻钱包' : '冻结钱包'}
            </PermissionButton>
          </div>
        </section>

        <section className={styles.sectionPanel}>
          <div className={styles.sectionHeader}>
            <div>
              <Title level={5} className={styles.sectionTitle}>
                钱包详情
              </Title>
            </div>
          </div>

          <Alert
            className={styles.statusAlert}
            type={isFrozen ? 'warning' : 'success'}
            showIcon
            message={walletStatusMeta.title}
            description={walletStatusMeta.description}
          />

          <Descriptions className={styles.detailDescriptions} column={2} bordered size="small">
            <Descriptions.Item label="钱包编号">{walletData?.walletNo || '-'}</Descriptions.Item>
            <Descriptions.Item label="币种">{walletData?.currency || '人民币'}</Descriptions.Item>
            <Descriptions.Item label="累计入账">
              <Text style={{ color: token.colorPrimary, fontWeight: 600 }}>
                ¥{formatCurrency(walletData?.totalIncome)}
              </Text>
            </Descriptions.Item>
            <Descriptions.Item label="累计支出">
              <Text style={{ color: token.colorError, fontWeight: 600 }}>
                ¥{formatCurrency(walletData?.totalExpend)}
              </Text>
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={walletStatusMeta.tagColor}>{walletStatusMeta.tagText}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="更新时间">{walletData?.updatedAt || '-'}</Descriptions.Item>
            {isFrozen && (
              <>
                <Descriptions.Item label="冻结原因" span={2}>
                  {walletData?.freezeReason || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="冻结时间" span={2}>
                  {walletData?.freezeTime || '-'}
                </Descriptions.Item>
              </>
            )}
          </Descriptions>
        </section>
      </div>

      <Modal
        title={
          <Space>
            <PlusCircleOutlined style={{ color: token.colorPrimary }} />
            <span>增加余额</span>
          </Space>
        }
        open={addModalVisible}
        onCancel={handleCloseAddModal}
        onOk={() => void handleRequestWalletCaptcha('add')}
        okText="确认增加余额"
        confirmLoading={walletActionSubmitting && pendingOperation?.action === 'add'}
        width={620}
        destroyOnHidden
      >
        <Space direction="vertical" size={20} style={{ width: '100%', marginTop: 12 }}>
          <Alert
            type="info"
            showIcon
            message="余额增加会立即生效"
            description="建议先确认金额和原因，再提交本次余额变动。"
          />

          <div
            style={{
              borderRadius: token.borderRadiusLG,
              padding: '20px 0',
            }}
          >
            <Row gutter={[12, 12]}>
              <Col span={8}>
                <div
                  style={{
                    padding: '0 14px',
                  }}
                >
                  <Text type="secondary">当前余额</Text>
                  <div
                    style={{
                      marginTop: 10,
                      fontSize: 20,
                      fontWeight: 700,
                      color: token.colorText,
                      whiteSpace: 'nowrap',
                    }}
                  >
                    ¥{formatCurrency(currentBalance)}
                  </div>
                </div>
              </Col>
              <Col span={8}>
                <div
                  style={{
                    padding: '0 14px',
                    borderLeft: `1px solid ${token.colorBorderSecondary}`,
                    borderRight: `1px solid ${token.colorBorderSecondary}`,
                  }}
                >
                  <Text type="secondary">本次增加</Text>
                  <div
                    style={{
                      marginTop: 10,
                      fontSize: 20,
                      fontWeight: 700,
                      color: token.colorPrimary,
                      whiteSpace: 'nowrap',
                    }}
                  >
                    +¥{formatCurrency(addAmountValue)}
                  </div>
                </div>
              </Col>
              <Col span={8}>
                <div
                  style={{
                    padding: '0 14px',
                  }}
                >
                  <Text type="secondary">调整后余额</Text>
                  <div
                    style={{
                      marginTop: 10,
                      fontSize: 20,
                      fontWeight: 700,
                      color: nextAddBalance >= 0 ? token.colorText : token.colorError,
                      whiteSpace: 'nowrap',
                    }}
                  >
                    ¥{formatCurrency(nextAddBalance)}
                  </div>
                </div>
              </Col>
            </Row>
          </div>

          <Form form={addForm} layout="vertical">
            <Form.Item
              label="增加金额"
              name="amount"
              rules={[
                { required: true, message: '请输入增加金额' },
                {
                  validator: (_rule, value) => {
                    if (value && value <= 0) {
                      return Promise.reject(new Error('金额必须大于 0'));
                    }
                    if (value && value > WALLET_CHANGE_MAX_AMOUNT) {
                      return Promise.reject(new Error('金额最大不能超过 100,000'));
                    }
                    return Promise.resolve();
                  },
                },
              ]}
            >
              <InputNumber
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                style={{ width: '100%' }}
                placeholder="请输入本次增加金额"
                prefix="¥"
                precision={2}
                min={0}
                max={WALLET_CHANGE_MAX_AMOUNT}
                size="large"
              />
            </Form.Item>
            <Form.Item
              label="操作原因"
              name="reason"
              rules={[{ required: true, message: '请输入操作原因' }]}
            >
              <Input.TextArea
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                rows={4}
                placeholder="请输入增加余额的原因，例如线下补款、活动赠送等"
                maxLength={200}
                showCount
              />
            </Form.Item>
          </Form>
        </Space>
      </Modal>

      <Modal
        title={
          <Space>
            <MinusCircleOutlined style={{ color: token.colorError }} />
            <span>扣减余额</span>
          </Space>
        }
        open={deductModalVisible}
        onCancel={handleCloseDeductModal}
        onOk={() => void handleRequestWalletCaptcha('deduct')}
        okText="确认扣减余额"
        okButtonProps={{ danger: true }}
        confirmLoading={walletActionSubmitting && pendingOperation?.action === 'deduct'}
        width={620}
        destroyOnHidden
      >
        <Space direction="vertical" size={20} style={{ width: '100%', marginTop: 12 }}>
          <Alert
            type="warning"
            showIcon
            message="余额扣减会立即生效"
            description="建议先确认金额和原因，再提交本次余额变动。"
          />

          <div
            style={{
              borderRadius: token.borderRadiusLG,
              padding: '20px 0',
            }}
          >
            <Row gutter={[12, 12]}>
              <Col span={8}>
                <div
                  style={{
                    padding: '0 14px',
                  }}
                >
                  <Text type="secondary">当前余额</Text>
                  <div
                    style={{
                      marginTop: 10,
                      fontSize: 20,
                      fontWeight: 700,
                      color: token.colorText,
                      whiteSpace: 'nowrap',
                    }}
                  >
                    ¥{formatCurrency(currentBalance)}
                  </div>
                </div>
              </Col>
              <Col span={8}>
                <div
                  style={{
                    padding: '0 14px',
                    borderLeft: `1px solid ${token.colorBorderSecondary}`,
                    borderRight: `1px solid ${token.colorBorderSecondary}`,
                  }}
                >
                  <Text type="secondary">本次扣减</Text>
                  <div
                    style={{
                      marginTop: 10,
                      fontSize: 20,
                      fontWeight: 700,
                      color: token.colorError,
                      whiteSpace: 'nowrap',
                    }}
                  >
                    -¥{formatCurrency(deductAmountValue)}
                  </div>
                </div>
              </Col>
              <Col span={8}>
                <div
                  style={{
                    padding: '0 14px',
                  }}
                >
                  <Text type="secondary">调整后余额</Text>
                  <div
                    style={{
                      marginTop: 10,
                      fontSize: 20,
                      fontWeight: 700,
                      color: nextDeductBalance >= 0 ? token.colorText : token.colorError,
                      whiteSpace: 'nowrap',
                    }}
                  >
                    ¥{formatCurrency(nextDeductBalance)}
                  </div>
                </div>
              </Col>
            </Row>
          </div>

          <Form form={deductForm} layout="vertical">
            <Form.Item
              label="扣减金额"
              name="amount"
              rules={[
                { required: true, message: '请输入扣减金额' },
                {
                  validator: (_rule, value) => {
                    if (value && value <= 0) {
                      return Promise.reject(new Error('金额必须大于 0'));
                    }
                    if (value && value > WALLET_CHANGE_MAX_AMOUNT) {
                      return Promise.reject(new Error('金额最大不能超过 100,000'));
                    }
                    if (value && value > currentBalance) {
                      return Promise.reject(new Error('扣减金额不能超过当前可用余额'));
                    }
                    return Promise.resolve();
                  },
                },
              ]}
            >
              <InputNumber
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                style={{ width: '100%' }}
                placeholder="请输入本次扣减金额"
                prefix="¥"
                precision={2}
                min={0}
                max={Math.min(WALLET_CHANGE_MAX_AMOUNT, currentBalance)}
                size="large"
              />
            </Form.Item>
            <Form.Item
              label="操作原因"
              name="reason"
              rules={[{ required: true, message: '请输入操作原因' }]}
            >
              <Input.TextArea
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                rows={4}
                placeholder="请输入扣减余额的原因，例如人工纠偏、异常扣款等"
                maxLength={200}
                showCount
              />
            </Form.Item>
          </Form>
        </Space>
      </Modal>

      <Modal
        title={
          <Space>
            <ExclamationCircleOutlined style={{ color: token.colorWarning }} />
            <span>冻结钱包</span>
          </Space>
        }
        open={freezeModalVisible}
        onCancel={handleCloseFreezeModal}
        onOk={() => void handleRequestWalletCaptcha('freeze')}
        okText="确认冻结"
        okButtonProps={{ danger: true }}
        confirmLoading={walletActionSubmitting && pendingOperation?.action === 'freeze'}
        width={560}
        destroyOnHidden
      >
        <Space direction="vertical" size={18} style={{ width: '100%', marginTop: 12 }}>
          <Alert
            type="warning"
            showIcon
            message="冻结后将暂停钱包交易能力"
            description="建议在确认风险或异常场景后再冻结，避免影响正常用户使用。"
          />
          <Form form={freezeForm} layout="vertical">
            <Form.Item
              label="冻结原因"
              name="reason"
              rules={[{ required: true, message: '请输入冻结原因' }]}
            >
              <Input.TextArea
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                rows={4}
                placeholder="请输入冻结原因，例如异常交易、人工风控处理等"
                maxLength={200}
                showCount
              />
            </Form.Item>
          </Form>
        </Space>
      </Modal>

      <Modal
        title={
          <Space>
            <UnlockOutlined style={{ color: token.colorSuccess }} />
            <span>解冻钱包</span>
          </Space>
        }
        open={unfreezeModalVisible}
        onCancel={handleCloseUnfreezeModal}
        onOk={() => void handleRequestWalletCaptcha('unfreeze')}
        okText="确认解冻"
        confirmLoading={walletActionSubmitting && pendingOperation?.action === 'unfreeze'}
        width={560}
        destroyOnHidden
      >
        <Space direction="vertical" size={18} style={{ width: '100%', marginTop: 12 }}>
          <Alert
            type="success"
            showIcon
            message="解冻后钱包会恢复正常使用"
            description="确认当前风险已处理完成后，再恢复用户的钱包交易能力。"
          />
          <Form form={unfreezeForm} layout="vertical">
            <Form.Item
              label="解冻原因"
              name="reason"
              rules={[{ required: true, message: '请输入解冻原因' }]}
            >
              <Input.TextArea
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                rows={4}
                placeholder="请输入解冻原因，例如风控解除、人工复核通过等"
                maxLength={200}
                showCount
              />
            </Form.Item>
          </Form>
        </Space>
      </Modal>

      <SliderCaptchaModal
        open={captchaOpen}
        onCancel={handleCaptchaCancel}
        onVerified={handleCaptchaVerified}
      />
    </>
  );
};

export default WalletBalanceTab;
