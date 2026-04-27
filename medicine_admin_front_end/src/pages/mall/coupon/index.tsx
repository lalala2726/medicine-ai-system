import type { ActionType, ProColumns, ProFormInstance } from '@ant-design/pro-components';
import {
  ModalForm,
  PageContainer,
  ProFormDigit,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProTable,
} from '@ant-design/pro-components';
import { Button, Descriptions, Modal, Tag, Typography, message, Radio } from 'antd';
import React, { useCallback, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  addCouponTemplate,
  getCouponTemplateById,
  deleteCouponTemplateById,
  listCouponLog,
  listCouponTemplate,
  updateCouponTemplate,
  type MallCouponTypes,
} from '@/api/mall/coupon';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { usePermission } from '@/hooks/usePermission';
import type { SecondaryMenuItem } from '@/components/SecondaryMenu';
import {
  type LayoutSecondaryMenuConfig,
  useLayoutSecondaryMenu,
} from '@/layouts/LayoutSecondaryMenuContext';
import {
  buildMallCouponIssuePath,
  buildMallCouponSecondaryRoutePath,
  isSecondaryMenuRouteEnabled,
  routePaths,
  type MallCouponSecondaryRouteKey,
} from '@/router/paths';
import ActivationCodeManageTab from './components/ActivationCodeManageTab';
import ActivationLogTab from './components/ActivationLogTab';
import styles from './index.module.less';

type TemplateRecord = MallCouponTypes.CouponTemplateVo;
type CouponLogRecord = MallCouponTypes.CouponLogVo;

type TemplateQuery = MallCouponTypes.CouponTemplateListRequest & {
  pageNum?: number;
  pageSize?: number;
};

type CouponLogQuery = MallCouponTypes.CouponLogListRequest & {
  pageNum?: number;
  pageSize?: number;
};

/** 优惠券管理二级菜单 key。 */
type CouponSecondaryMenuKey = MallCouponSecondaryRouteKey;

/**
 * 模板表单值。
 */
interface TemplateFormValues {
  /** 优惠券名称 */
  name: string;
  /** 使用门槛金额 */
  thresholdAmount: number;
  /** 优惠券面额 */
  faceAmount: number;
  /** 是否允许继续使用 */
  continueUseEnabled: number;
  /** 是否允许叠加 */
  stackableEnabled: number;
  /** 模板状态 */
  status: string;
  /** 模板备注 */
  remark?: string;
}

/** 模板状态选项。 */
const TEMPLATE_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'DISABLED' },
];

/** 优惠券类型文本映射。 */
const COUPON_TYPE_TEXT_MAP: Record<string, string> = {
  FULL_REDUCTION: '满减券',
};

/** 模板删除模式选项。 */
const TEMPLATE_DELETE_MODE_OPTIONS = [
  {
    title: '仅删除模板',
    description: '模板会从后台列表消失，但已经发出去的券继续保持原状态有效。',
    value: 'HIDE_ONLY',
  },
  {
    title: '删除模板并使已发可用券失效',
    description: '模板会从后台列表消失，同时该模板下已发且当前可用的券会立即失效。',
    value: 'HIDE_AND_EXPIRE_ISSUED',
  },
] satisfies Array<{
  title: string;
  description: string;
  value: MallCouponTypes.CouponTemplateDeleteMode;
}>;

/** 日志变更类型选项。 */
const COUPON_CHANGE_TYPE_OPTIONS = [
  { label: '发券', value: 'GRANT' },
  { label: '锁券', value: 'LOCK' },
  { label: '消耗', value: 'CONSUME' },
  { label: '释放锁券', value: 'RELEASE' },
  { label: '返还', value: 'RETURN' },
  { label: '过期', value: 'EXPIRE' },
  { label: '手工调整', value: 'MANUAL_ADJUST' },
];

/** 优惠券管理布局二级菜单宽度。 */
const COUPON_SECONDARY_MENU_WIDTH = 136;

/** 优惠券管理二级菜单项。 */
const COUPON_SECONDARY_MENU_ITEMS: SecondaryMenuItem[] = [
  { key: 'template', label: '模板管理', access: ADMIN_PERMISSIONS.mallCoupon.templateList },
  {
    key: 'activationCode',
    label: '激活码管理',
    access: ADMIN_PERMISSIONS.mallCoupon.activationBatchList,
  },
  { key: 'log', label: '优惠券日志', access: ADMIN_PERMISSIONS.mallCoupon.logList },
  {
    key: 'activationLog',
    label: '激活码日志',
    access: ADMIN_PERMISSIONS.mallCoupon.activationLogList,
  },
];

/** 开启独立路由的优惠券二级菜单 key。 */
const COUPON_ROUTE_ENABLED_MENU_KEYS: CouponSecondaryMenuKey[] = [
  'template',
  'activationCode',
  'log',
  'activationLog',
];

/** 优惠券路由与二级菜单 key 的映射。 */
const COUPON_ROUTE_TAB_MAP: Record<string, CouponSecondaryMenuKey> = {
  [routePaths.mallCoupon]: 'template',
  [routePaths.mallCouponTemplate]: 'template',
  [routePaths.mallCouponActivationManage]: 'activationCode',
  [routePaths.mallCouponLog]: 'log',
  [routePaths.mallCouponActivationLog]: 'activationLog',
};

/** 模板状态到颜色的映射。 */
const TEMPLATE_STATUS_COLOR_MAP: Record<string, string> = {
  DRAFT: 'default',
  ACTIVE: 'success',
  DISABLED: 'error',
};

/** 日志变更类型到颜色的映射。 */
const COUPON_CHANGE_TYPE_COLOR_MAP: Record<string, string> = {
  GRANT: 'success',
  LOCK: 'processing',
  CONSUME: 'error',
  RELEASE: 'warning',
  RETURN: 'cyan',
  EXPIRE: 'default',
  MANUAL_ADJUST: 'purple',
};

/**
 * 金额格式化工具。
 * @param value 原始金额。
 * @returns 带人民币前缀的金额文本。
 */
function formatCurrency(value?: string | number): string {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return `¥${Number(value).toFixed(2)}`;
}

/**
 * 获取日志变更类型文本。
 * @param value 变更类型值。
 * @returns 变更类型文本。
 */
function changeTypeText(value?: string): string {
  const option = COUPON_CHANGE_TYPE_OPTIONS.find((item) => item.value === value);
  return option?.label || value || '-';
}

/**
 * 获取优惠券类型中文名称。
 * @param value 优惠券类型编码。
 * @returns 优惠券类型中文名称。
 */
function couponTypeText(value?: string): string {
  if (!value) {
    return COUPON_TYPE_TEXT_MAP.FULL_REDUCTION;
  }
  return COUPON_TYPE_TEXT_MAP[value] || value;
}

/**
 * 根据当前路径解析优惠券二级菜单 key。
 * @param pathname 当前页面路径。
 * @returns 二级菜单 key。
 */
function couponTabFromPathname(pathname: string): CouponSecondaryMenuKey {
  return COUPON_ROUTE_TAB_MAP[pathname] || 'template';
}

/**
 * 优惠券管理页面。
 * @returns 页面节点。
 */
const MallCouponPage: React.FC = () => {
  const [messageApi, contextHolder] = message.useMessage();
  const location = useLocation();
  const pathname = location.pathname;
  const [manualActiveTab, setManualActiveTab] = useState<CouponSecondaryMenuKey>('template');
  const [activationLogQuerySeed, setActivationLogQuerySeed] = useState<
    Partial<MallCouponTypes.ActivationLogListRequest> | undefined
  >(undefined);
  const [templateModalOpen, setTemplateModalOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<TemplateRecord | undefined>(undefined);
  const [deletingTemplate, setDeletingTemplate] = useState<TemplateRecord | undefined>(undefined);
  const [deleteMode, setDeleteMode] =
    useState<MallCouponTypes.CouponTemplateDeleteMode>('HIDE_ONLY');
  const [templateDeleteSubmittingMode, setTemplateDeleteSubmittingMode] =
    useState<MallCouponTypes.CouponTemplateDeleteMode | null>(null);

  const navigate = useNavigate();
  const templateActionRef = useRef<ActionType | null>(null);
  const logActionRef = useRef<ActionType | null>(null);
  const templateFormRef = useRef<ProFormInstance<TemplateFormValues>>(undefined);
  const secondaryRouteEnabled = isSecondaryMenuRouteEnabled(routePaths.mallCoupon);
  const activeTab = secondaryRouteEnabled ? couponTabFromPathname(pathname) : manualActiveTab;
  const { canAccess } = usePermission();

  React.useEffect(() => {
    const activeItem = COUPON_SECONDARY_MENU_ITEMS.find((item) => item.key === activeTab);
    if (activeItem && canAccess(activeItem.access)) {
      return;
    }

    const nextItem = COUPON_SECONDARY_MENU_ITEMS.find((item) => canAccess(item.access));
    if (!nextItem) {
      return;
    }

    const nextTab = nextItem.key as CouponSecondaryMenuKey;
    if (secondaryRouteEnabled && COUPON_ROUTE_ENABLED_MENU_KEYS.includes(nextTab)) {
      const nextPath = buildMallCouponSecondaryRoutePath(nextTab);
      if (nextPath !== pathname) {
        navigate(nextPath, { replace: true });
      }
      return;
    }

    setManualActiveTab(nextTab);
  }, [activeTab, canAccess, navigate, pathname, secondaryRouteEnabled]);

  /**
   * 处理布局二级菜单切换。
   * @param key 二级菜单 key。
   * @returns 无返回值。
   */
  const handleSecondaryMenuChange = useCallback(
    (key: string) => {
      const nextTab = key as CouponSecondaryMenuKey;
      if (nextTab === 'activationLog') {
        setActivationLogQuerySeed(undefined);
      }
      if (secondaryRouteEnabled && COUPON_ROUTE_ENABLED_MENU_KEYS.includes(nextTab)) {
        const nextPath = buildMallCouponSecondaryRoutePath(nextTab);
        if (nextPath !== pathname) {
          navigate(nextPath);
        }
        return;
      }
      setManualActiveTab(nextTab);
    },
    [navigate, pathname, secondaryRouteEnabled],
  );

  /**
   * 优惠券页面布局二级菜单配置。
   */
  const secondaryMenuConfig = useMemo<LayoutSecondaryMenuConfig>(
    () => ({
      items: COUPON_SECONDARY_MENU_ITEMS,
      activeKey: activeTab,
      onChange: handleSecondaryMenuChange,
      width: COUPON_SECONDARY_MENU_WIDTH,
    }),
    [activeTab, handleSecondaryMenuChange],
  );

  useLayoutSecondaryMenu(secondaryMenuConfig);

  /**
   * 模板弹窗开关变化处理。
   * @param open 是否打开。
   * @returns 无返回值。
   */
  const handleTemplateModalOpenChange = useCallback((open: boolean) => {
    setTemplateModalOpen(open);
    if (!open) {
      setEditingTemplate(undefined);
    }
  }, []);

  /**
   * 打开新增模板弹窗。
   * @returns 无返回值。
   */
  const handleCreateTemplate = useCallback(() => {
    setEditingTemplate(undefined);
    templateFormRef.current?.resetFields();
    setTemplateModalOpen(true);
  }, []);

  /**
   * 按模板ID加载详情并打开编辑模板弹窗。
   * @param record 模板记录。
   * @returns 无返回值。
   */
  const handleEditTemplate = useCallback(
    async (record: TemplateRecord) => {
      if (!record.id) {
        messageApi.warning('缺少模板ID，无法编辑');
        return;
      }
      try {
        const detail = await getCouponTemplateById(record.id);
        setEditingTemplate(detail);
        setTemplateModalOpen(true);
      } catch (error) {
        console.error('加载优惠券模板详情失败:', error);
        messageApi.error('加载优惠券模板详情失败，请稍后重试');
      }
    },
    [messageApi],
  );

  /**
   * 跳转到发券页面。
   * @param templateId 模板ID。
   * @returns 无返回值。
   */
  const handleGoToIssuePage = useCallback(
    (templateId?: number) => {
      navigate(buildMallCouponIssuePath({ templateId }));
    },
    [navigate],
  );

  /**
   * 处理模板删除。
   * @param record 模板记录。
   * @returns 无返回值。
   */
  const handleDeleteTemplate = useCallback(
    (record: TemplateRecord) => {
      if (!record.id) {
        messageApi.warning('缺少模板ID，无法删除');
        return;
      }
      setDeleteMode('HIDE_ONLY');
      setDeletingTemplate(record);
    },
    [messageApi],
  );

  /**
   * 关闭模板删除弹窗。
   * @returns 无返回值。
   */
  const handleDeleteTemplateModalClose = useCallback(() => {
    if (templateDeleteSubmittingMode) {
      return;
    }
    setDeletingTemplate(undefined);
  }, [templateDeleteSubmittingMode]);

  /**
   * 确认模板删除。
   * @param deleteMode 删除模式。
   * @returns 无返回值。
   */
  const handleConfirmDeleteTemplate = useCallback(
    async (deleteMode: MallCouponTypes.CouponTemplateDeleteMode) => {
      if (!deletingTemplate?.id) {
        messageApi.warning('缺少模板ID，无法删除');
        return;
      }
      setTemplateDeleteSubmittingMode(deleteMode);
      try {
        await deleteCouponTemplateById(deletingTemplate.id, deleteMode);
        messageApi.success(
          deleteMode === 'HIDE_AND_EXPIRE_ISSUED'
            ? '模板已删除，已发可用券已失效'
            : '模板已删除，已发券保持有效',
        );
        setDeletingTemplate(undefined);
        templateActionRef.current?.reload();
      } catch (error) {
        console.error('删除优惠券模板失败:', error);
        messageApi.error('模板删除失败，请稍后重试');
      } finally {
        setTemplateDeleteSubmittingMode(null);
      }
    },
    [deletingTemplate, messageApi],
  );

  /**
   * 处理模板表单提交。
   * @param values 表单值。
   * @returns 提交是否成功。
   */
  const handleTemplateSubmit = useCallback(
    async (values: TemplateFormValues) => {
      try {
        const payload: MallCouponTypes.CouponTemplateSaveRequest = {
          name: values.name.trim(),
          thresholdAmount: String(values.thresholdAmount),
          faceAmount: String(values.faceAmount),
          continueUseEnabled: values.continueUseEnabled,
          stackableEnabled: values.stackableEnabled,
          status: values.status,
          remark: values.remark?.trim() || undefined,
        };

        if (editingTemplate?.id) {
          await updateCouponTemplate({
            ...payload,
            id: editingTemplate.id,
          });
          messageApi.success('模板修改成功');
        } else {
          await addCouponTemplate(payload);
          messageApi.success('模板新增成功');
        }

        templateActionRef.current?.reload();
        return true;
      } catch (error) {
        console.error('提交优惠券模板失败:', error);
        messageApi.error('提交优惠券模板失败，请检查表单后重试');
        return false;
      }
    },
    [editingTemplate, messageApi],
  );

  /**
   * 模板表格列定义。
   */
  const templateColumns: ProColumns<TemplateRecord>[] = useMemo(
    () => [
      {
        title: '优惠券名称',
        dataIndex: 'name',
        ellipsis: true,
      },
      {
        title: '类型',
        dataIndex: 'couponType',
        hideInSearch: true,
        render: (value) => couponTypeText(value as string | undefined),
      },
      {
        title: '使用门槛',
        dataIndex: 'thresholdAmount',
        hideInSearch: true,
        render: (value) => formatCurrency(value as string),
      },
      {
        title: '面额',
        dataIndex: 'faceAmount',
        hideInSearch: true,
        render: (value) => formatCurrency(value as string),
      },
      {
        title: '可继续使用',
        dataIndex: 'continueUseEnabled',
        hideInSearch: true,
        render: (value) =>
          Number(value) === 1 ? <Tag color="success">允许</Tag> : <Tag>不允许</Tag>,
      },
      {
        title: '可叠加',
        dataIndex: 'stackableEnabled',
        hideInSearch: true,
        render: (value) =>
          Number(value) === 1 ? <Tag color="success">允许</Tag> : <Tag>不允许</Tag>,
      },
      {
        title: '状态',
        dataIndex: 'status',
        width: 100,
        valueType: 'select',
        valueEnum: {
          DRAFT: { text: '草稿' },
          ACTIVE: { text: '启用' },
          DISABLED: { text: '停用' },
        },
        render: (_, record) => {
          const status = record.status || 'DRAFT';
          const option = TEMPLATE_STATUS_OPTIONS.find((item) => item.value === status);
          return (
            <Tag color={TEMPLATE_STATUS_COLOR_MAP[status] || 'default'}>
              {option?.label || status}
            </Tag>
          );
        },
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        width: 180,
        valueType: 'dateTime',
        hideInSearch: true,
      },
      {
        title: '操作',
        dataIndex: 'option',
        valueType: 'option',
        width: 220,
        fixed: 'right',
        align: 'center',
        render: (_, record) => (
          <TableActionGroup>
            <PermissionButton
              type="link"
              access={ADMIN_PERMISSIONS.mallCoupon.issue}
              onClick={() => {
                handleGoToIssuePage(record.id);
              }}
            >
              去发券
            </PermissionButton>
            <PermissionButton
              type="link"
              access={ADMIN_PERMISSIONS.mallCoupon.templateEdit}
              onClick={() => {
                void handleEditTemplate(record);
              }}
            >
              编辑
            </PermissionButton>
            <PermissionButton
              type="link"
              danger
              access={ADMIN_PERMISSIONS.mallCoupon.templateDelete}
              onClick={() => {
                void handleDeleteTemplate(record);
              }}
            >
              删除
            </PermissionButton>
          </TableActionGroup>
        ),
      },
    ],
    [handleDeleteTemplate, handleEditTemplate, handleGoToIssuePage],
  );

  /**
   * 优惠券日志表格列定义。
   */
  const couponLogColumns: ProColumns<CouponLogRecord>[] = useMemo(
    () => [
      {
        title: '订单号',
        dataIndex: 'orderNo',
        width: 190,
      },
      {
        title: '变更类型',
        dataIndex: 'changeType',
        width: 130,
        valueType: 'select',
        valueEnum: {
          GRANT: { text: '发券' },
          LOCK: { text: '锁券' },
          CONSUME: { text: '消耗' },
          RELEASE: { text: '释放锁券' },
          RETURN: { text: '返还' },
          EXPIRE: { text: '过期' },
          MANUAL_ADJUST: { text: '手工调整' },
        },
        render: (_, record) => {
          const type = record.changeType || '-';
          const option = COUPON_CHANGE_TYPE_OPTIONS.find((item) => item.value === type);
          return (
            <Tag color={COUPON_CHANGE_TYPE_COLOR_MAP[type] || 'default'}>
              {option?.label || type}
            </Tag>
          );
        },
      },
      {
        title: '变更金额',
        dataIndex: 'changeAmount',
        width: 120,
        hideInSearch: true,
        render: (value) => formatCurrency(value as string),
      },
      {
        title: '操作人',
        dataIndex: 'operatorName',
        width: 120,
        hideInSearch: true,
        render: (_, record) => record.operatorName || record.operatorId || '-',
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        width: 180,
        valueType: 'dateTime',
        hideInSearch: true,
      },
      {
        title: '操作',
        dataIndex: 'option',
        valueType: 'option',
        width: 90,
        fixed: 'right',
        align: 'center',
        render: (_, record) => (
          <TableActionGroup>
            <Button
              type="link"
              onClick={() => {
                Modal.info({
                  title: '优惠券日志详情',
                  width: 760,
                  icon: null,
                  okText: '关闭',
                  content: (
                    <div style={{ marginTop: 16 }}>
                      <Descriptions bordered column={2} size="small">
                        <Descriptions.Item label="日志ID">{record.id ?? '-'}</Descriptions.Item>
                        <Descriptions.Item label="用户券ID">
                          {record.couponId ?? '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="优惠券名称" span={2}>
                          {record.couponName || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="用户名">
                          {record.userName || record.userId || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="订单号">
                          {record.orderNo ? (
                            <Typography.Text copyable={{ text: record.orderNo }}>
                              {record.orderNo}
                            </Typography.Text>
                          ) : (
                            '-'
                          )}
                        </Descriptions.Item>
                        <Descriptions.Item label="变更类型">
                          <Tag
                            color={
                              COUPON_CHANGE_TYPE_COLOR_MAP[record.changeType || ''] || 'default'
                            }
                          >
                            {changeTypeText(record.changeType)}
                          </Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="操作人">
                          {record.operatorName || record.operatorId || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="变更金额">
                          <Typography.Text strong>
                            {formatCurrency(record.changeAmount as string)}
                          </Typography.Text>
                        </Descriptions.Item>
                        <Descriptions.Item label="抵扣金额">
                          {formatCurrency(record.deductAmount as string)}
                        </Descriptions.Item>
                        <Descriptions.Item label="浪费金额">
                          {formatCurrency(record.wasteAmount as string)}
                        </Descriptions.Item>
                        <Descriptions.Item label="来源类型">
                          {record.sourceType || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="变更前可用金额">
                          {formatCurrency(record.beforeAvailableAmount as string)}
                        </Descriptions.Item>
                        <Descriptions.Item label="变更后可用金额">
                          {formatCurrency(record.afterAvailableAmount as string)}
                        </Descriptions.Item>
                        <Descriptions.Item label="来源业务号" span={2}>
                          {record.sourceBizNo ? (
                            <Typography.Text copyable={{ text: record.sourceBizNo }}>
                              {record.sourceBizNo}
                            </Typography.Text>
                          ) : (
                            '-'
                          )}
                        </Descriptions.Item>
                        <Descriptions.Item label="创建时间" span={2}>
                          {record.createTime || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="备注" span={2}>
                          {record.remark || (
                            <Typography.Text type="secondary">暂无备注</Typography.Text>
                          )}
                        </Descriptions.Item>
                      </Descriptions>
                    </div>
                  ),
                });
              }}
            >
              详情
            </Button>
          </TableActionGroup>
        ),
      },
    ],
    [],
  );

  /**
   * 模板弹窗初始值。
   */
  const templateInitialValues = useMemo<Partial<TemplateFormValues>>(() => {
    if (!editingTemplate) {
      return {
        continueUseEnabled: 1,
        stackableEnabled: 0,
        status: 'ACTIVE',
      };
    }

    return {
      name: editingTemplate.name || '',
      thresholdAmount: Number(editingTemplate.thresholdAmount || 0),
      faceAmount: Number(editingTemplate.faceAmount || 0),
      continueUseEnabled: Number(editingTemplate.continueUseEnabled ?? 1),
      stackableEnabled: Number(editingTemplate.stackableEnabled ?? 0),
      status: editingTemplate.status || 'ACTIVE',
      remark: editingTemplate.remark,
    };
  }, [editingTemplate]);

  return (
    <PageContainer title="优惠券">
      {contextHolder}

      {activeTab === 'template' && (
        <ProTable<TemplateRecord, TemplateQuery>
          rowKey="id"
          actionRef={templateActionRef}
          headerTitle="优惠券模板列表"
          search={{ labelWidth: 98 }}
          request={async (params) => {
            const { current, pageSize, ...rest } = params;
            const query: TemplateQuery = {
              ...(rest as TemplateQuery),
              pageNum: Number(current ?? 1),
              pageSize: Number(pageSize ?? 10),
            };

            const result = await listCouponTemplate(query);
            return {
              data: result?.rows ?? [],
              success: true,
              total: Number(result?.total ?? 0),
            };
          }}
          columns={templateColumns}
          pagination={{
            showQuickJumper: true,
            showSizeChanger: true,
            defaultPageSize: 10,
          }}
          toolBarRender={() => [
            <PermissionButton
              key="create"
              type="primary"
              access={ADMIN_PERMISSIONS.mallCoupon.templateAdd}
              onClick={handleCreateTemplate}
            >
              新增优惠券
            </PermissionButton>,
            <PermissionButton
              key="issue"
              access={ADMIN_PERMISSIONS.mallCoupon.issue}
              onClick={() => {
                handleGoToIssuePage();
              }}
            >
              去发券
            </PermissionButton>,
          ]}
        />
      )}

      {activeTab === 'log' && (
        <ProTable<CouponLogRecord, CouponLogQuery>
          rowKey="id"
          actionRef={logActionRef}
          headerTitle="优惠券变更日志"
          search={{ labelWidth: 108 }}
          request={async (params) => {
            const { current, pageSize, ...rest } = params;
            const query: CouponLogQuery = {
              ...(rest as CouponLogQuery),
              couponId: rest.couponId ? Number(rest.couponId) : undefined,
              userId: rest.userId ? Number(rest.userId) : undefined,
              pageNum: Number(current ?? 1),
              pageSize: Number(pageSize ?? 10),
            };

            const result = await listCouponLog(query);
            return {
              data: result?.rows ?? [],
              success: true,
              total: Number(result?.total ?? 0),
            };
          }}
          columns={couponLogColumns}
          pagination={{
            showQuickJumper: true,
            showSizeChanger: true,
            defaultPageSize: 10,
          }}
        />
      )}

      {activeTab === 'activationCode' && (
        <ActivationCodeManageTab
          onViewLogs={(querySeed) => {
            setActivationLogQuerySeed(querySeed);
            if (secondaryRouteEnabled) {
              navigate(buildMallCouponSecondaryRoutePath('activationLog'));
              return;
            }
            setManualActiveTab('activationLog');
          }}
        />
      )}

      {activeTab === 'activationLog' && <ActivationLogTab querySeed={activationLogQuerySeed} />}

      <ModalForm<TemplateFormValues>
        title={editingTemplate?.id ? '编辑优惠券模板' : '新增优惠券'}
        open={templateModalOpen}
        onOpenChange={handleTemplateModalOpenChange}
        initialValues={templateInitialValues}
        formRef={templateFormRef}
        width={620}
        autoFocusFirstInput
        modalProps={{
          destroyOnClose: true,
          onCancel: () => {
            handleTemplateModalOpenChange(false);
          },
        }}
        onFinish={handleTemplateSubmit}
      >
        <ProFormText
          label="优惠券名称"
          name="name"
          rules={[{ required: true, message: '请输入优惠券名称' }]}
          fieldProps={{ autoComplete: TEXT_INPUT_AUTOCOMPLETE, maxLength: 128, showCount: true }}
        />

        <ProFormDigit
          label="使用门槛金额"
          name="thresholdAmount"
          min={0}
          rules={[{ required: true, message: '请输入使用门槛金额' }]}
          fieldProps={{ autoComplete: TEXT_INPUT_AUTOCOMPLETE, precision: 2 }}
        />

        <ProFormDigit
          label="优惠券面额"
          name="faceAmount"
          min={0.01}
          rules={[{ required: true, message: '请输入优惠券面额' }]}
          fieldProps={{ autoComplete: TEXT_INPUT_AUTOCOMPLETE, precision: 2 }}
        />

        <ProFormSelect
          label="是否允许继续使用"
          name="continueUseEnabled"
          options={[
            { label: '允许', value: 1 },
            { label: '不允许', value: 0 },
          ]}
          rules={[{ required: true, message: '请选择是否允许继续使用' }]}
        />

        <ProFormSelect
          label="是否允许叠加"
          name="stackableEnabled"
          options={[
            { label: '允许', value: 1 },
            { label: '不允许', value: 0 },
          ]}
          rules={[{ required: true, message: '请选择是否允许叠加' }]}
        />

        <ProFormSelect
          label="模板状态"
          name="status"
          options={TEMPLATE_STATUS_OPTIONS}
          rules={[{ required: true, message: '请选择模板状态' }]}
        />

        <ProFormTextArea
          label="模板备注"
          name="remark"
          fieldProps={{
            autoComplete: TEXT_INPUT_AUTOCOMPLETE,
            maxLength: 200,
            showCount: true,
            autoSize: { minRows: 2, maxRows: 4 },
          }}
        />
      </ModalForm>

      <Modal
        title={<div style={{ fontSize: 16, fontWeight: 500, color: '#1f2329' }}>删除模板配置</div>}
        open={Boolean(deletingTemplate)}
        onCancel={handleDeleteTemplateModalClose}
        maskClosable={!templateDeleteSubmittingMode}
        closable={!templateDeleteSubmittingMode}
        okText="确认删除"
        okButtonProps={{
          danger: deleteMode === 'HIDE_AND_EXPIRE_ISSUED',
          type: 'primary',
          loading: Boolean(templateDeleteSubmittingMode),
        }}
        cancelButtonProps={{
          disabled: Boolean(templateDeleteSubmittingMode),
        }}
        onOk={() => {
          Modal.confirm({
            title: '高危操作确认',
            content: (
              <div>
                您确定要执行{' '}
                <Typography.Text
                  strong
                  type={deleteMode === 'HIDE_AND_EXPIRE_ISSUED' ? 'danger' : 'warning'}
                >
                  {deleteMode === 'HIDE_AND_EXPIRE_ISSUED'
                    ? '删除模板并使已发券失效'
                    : '仅删除模板'}
                </Typography.Text>{' '}
                操作吗？
                {deleteMode === 'HIDE_AND_EXPIRE_ISSUED' && (
                  <div style={{ marginTop: 8, color: '#cf1322' }}>
                    这可能导致大量普通用户的优惠券直接失效，引发严重客诉，请务必三思！
                  </div>
                )}
              </div>
            ),
            okText: '确认执行',
            cancelText: '取消',
            okButtonProps: { danger: deleteMode === 'HIDE_AND_EXPIRE_ISSUED' },
            onOk: () => {
              void handleConfirmDeleteTemplate(deleteMode);
            },
          });
        }}
        width={480}
      >
        <div className={styles.deleteModalWarning}>
          <Typography.Text type="secondary" className={styles.warningText}>
            即将删除模板{' '}
            <Typography.Text strong className={styles.templateName}>
              「{deletingTemplate?.name || deletingTemplate?.id || '-'}」
            </Typography.Text>
            。<br />
            此操作不可撤销，请仔细选择对后续存量优惠券的处理方式。
          </Typography.Text>
        </div>

        <Radio.Group
          onChange={(e) => setDeleteMode(e.target.value)}
          value={deleteMode}
          className={styles.deleteOptionsGroup}
        >
          {TEMPLATE_DELETE_MODE_OPTIONS.map((option) => {
            const checked = deleteMode === option.value;
            const isDanger = option.value === 'HIDE_AND_EXPIRE_ISSUED';

            let cardClass = styles.optionCard;
            if (checked) {
              cardClass += isDanger ? ` ${styles.dangerChecked}` : ` ${styles.checked}`;
            }

            return (
              <label key={option.value} className={cardClass}>
                <Radio value={option.value} className={styles.radioControl} />
                <div className={styles.optionContent}>
                  <div className={styles.optionTitle}>{option.title}</div>
                  <div className={styles.optionDesc}>{option.description}</div>
                </div>
              </label>
            );
          })}
        </Radio.Group>
      </Modal>
    </PageContainer>
  );
};

export default MallCouponPage;
