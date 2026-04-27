import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { PageContainer } from '@ant-design/pro-components';
import XMarkdown from '@ant-design/x-markdown';
import {
  Button,
  Card,
  Checkbox,
  Col,
  Form,
  InputNumber,
  Row,
  Space,
  Spin,
  Switch,
  Typography,
  message,
} from 'antd';
import React, { useCallback, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  getAgreementConfig,
  type AgreementConfigTypes,
  updateAgreementConfig,
} from '@/api/system/agreement-config';
import {
  getSecurityConfig,
  type SecurityConfigTypes,
  updateSecurityConfig,
} from '@/api/system/security-config';
import { PermissionButton, RichTextEditor } from '@/components';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { useThemeContext } from '@/contexts/ThemeContext';
import { usePermission } from '@/hooks/usePermission';
import type { SecondaryMenuItem } from '@/components/SecondaryMenu';
import {
  type LayoutSecondaryMenuConfig,
  useLayoutSecondaryMenu,
} from '@/layouts/LayoutSecondaryMenuContext';
import {
  buildSystemConfigSecondaryRoutePath,
  isSecondaryMenuRouteEnabled,
  routePaths,
  type SystemConfigSecondaryRouteKey,
} from '@/router/paths';
import '@ant-design/x-markdown/themes/light.css';
import '@ant-design/x-markdown/themes/dark.css';
import EsIndexSection from './components/EsIndexSection';

/** 系统配置二级菜单 key。 */
type SystemConfigSecondaryMenuKey = SystemConfigSecondaryRouteKey;

/** 系统配置二级菜单宽度。 */
const SYSTEM_CONFIG_SECONDARY_MENU_WIDTH = 156;

/** 系统配置二级菜单项。 */
const SYSTEM_CONFIG_SECONDARY_MENU_ITEMS: SecondaryMenuItem[] = [
  { key: 'security', label: '安全配置', access: ADMIN_PERMISSIONS.systemConfig.securityQuery },
  { key: 'agreement', label: '软件协议', access: ADMIN_PERMISSIONS.systemConfig.agreementQuery },
  { key: 'esIndex', label: '商品索引', access: ADMIN_PERMISSIONS.systemConfig.esIndexQuery },
];

/** 系统配置路由与二级菜单 key 的映射。 */
const SYSTEM_CONFIG_ROUTE_TAB_MAP: Record<string, SystemConfigSecondaryMenuKey> = {
  [routePaths.systemConfig]: 'security',
  [routePaths.systemConfigSecurity]: 'security',
  [routePaths.systemConfigAgreement]: 'agreement',
  [routePaths.systemConfigEsIndex]: 'esIndex',
};

/** 协议编辑页富文本编辑器高度。 */
const AGREEMENT_EDITOR_RICH_TEXT_EDITOR_HEIGHT = 'calc(100vh - 320px)';

/** 协议预览区域高度。 */
const AGREEMENT_PREVIEW_HEIGHT = 'calc(100vh - 270px)';

/** 协议区域最小高度。 */
const AGREEMENT_SECTION_MIN_HEIGHT = 'calc(100vh - 240px)';

/** 管理端水印展示字段选项。 */
const ADMIN_WATERMARK_FIELD_OPTIONS = [
  { label: '用户名', value: 'username' },
  { label: '用户ID', value: 'userId' },
] as const;

/** 管理端水印字段值类型。 */
type AdminWatermarkFieldValue = (typeof ADMIN_WATERMARK_FIELD_OPTIONS)[number]['value'];

/**
 * 登录策略配置表单值。
 */
interface PolicyConfigFormValues {
  /** 管理端连续失败阈值。 */
  adminMaxRetryCount: number;
  /** 管理端锁定分钟数。 */
  adminLockMinutes: number;
  /** 客户端连续失败阈值。 */
  clientMaxRetryCount: number;
  /** 客户端锁定分钟数。 */
  clientLockMinutes: number;
}

/**
 * 水印配置表单值。
 */
interface WatermarkConfigFormValues {
  /** 是否启用管理端水印。 */
  adminWatermarkEnabled: boolean;
  /** 管理端水印展示字段列表。 */
  adminWatermarkFields: AdminWatermarkFieldValue[];
}

/**
 * 软件协议配置表单值。
 */
interface AgreementConfigFormValues {
  /** 协议 Markdown 内容。 */
  content: string;
}

/** 软件协议编辑类型。 */
type AgreementEditorType = 'software' | 'privacy';

/**
 * 根据当前路径解析系统配置二级菜单 key。
 * @param pathname 当前页面路径。
 * @returns 二级菜单 key。
 */
function systemConfigTabFromPathname(pathname: string): SystemConfigSecondaryMenuKey {
  return SYSTEM_CONFIG_ROUTE_TAB_MAP[pathname] || 'security';
}

/**
 * 将协议编辑类型映射为展示标题。
 * @param editorType 协议编辑类型。
 * @returns 协议展示标题。
 */
function resolveAgreementEditorTitle(editorType: AgreementEditorType): string {
  return editorType === 'software' ? '软件协议' : '隐私协议';
}

/**
 * 系统配置页面。
 * @returns 页面节点。
 */
const SystemConfigPage: React.FC = () => {
  const { isDark } = useThemeContext();
  const [messageApi, contextHolder] = message.useMessage();
  const [securityLoading, setSecurityLoading] = useState(false);
  const [policySaving, setPolicySaving] = useState(false);
  const [policyEditable, setPolicyEditable] = useState(false);
  const [watermarkSaving, setWatermarkSaving] = useState(false);
  const [agreementLoading, setAgreementLoading] = useState(false);
  const [agreementSaving, setAgreementSaving] = useState(false);
  const [agreementEditorType, setAgreementEditorType] = useState<AgreementEditorType | null>(null);
  const [agreementConfig, setAgreementConfig] =
    useState<AgreementConfigTypes.AgreementConfigVo | null>(null);
  const [policyForm] = Form.useForm<PolicyConfigFormValues>();
  const [watermarkForm] = Form.useForm<WatermarkConfigFormValues>();
  const [agreementForm] = Form.useForm<AgreementConfigFormValues>();
  const location = useLocation();
  const navigate = useNavigate();
  const pathname = location.pathname;
  const [manualActiveTab, setManualActiveTab] = useState<SystemConfigSecondaryMenuKey>('security');
  const secondaryRouteEnabled = isSecondaryMenuRouteEnabled(routePaths.systemConfig);
  const activeTab = secondaryRouteEnabled ? systemConfigTabFromPathname(pathname) : manualActiveTab;
  const markdownThemeClassName = isDark ? 'x-markdown-dark' : 'x-markdown-light';
  const { canAccess } = usePermission();

  React.useEffect(() => {
    const activeItem = SYSTEM_CONFIG_SECONDARY_MENU_ITEMS.find((item) => item.key === activeTab);
    if (activeItem && canAccess(activeItem.access)) {
      return;
    }

    const nextItem = SYSTEM_CONFIG_SECONDARY_MENU_ITEMS.find((item) => canAccess(item.access));
    if (!nextItem) {
      return;
    }

    const nextTab = nextItem.key as SystemConfigSecondaryMenuKey;
    if (secondaryRouteEnabled) {
      const nextPath = buildSystemConfigSecondaryRoutePath(nextTab);
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
      const nextTab = key as SystemConfigSecondaryMenuKey;
      if (secondaryRouteEnabled) {
        const nextPath = buildSystemConfigSecondaryRoutePath(nextTab);
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
   * 系统配置页面布局二级菜单配置。
   */
  const secondaryMenuConfig = useMemo<LayoutSecondaryMenuConfig>(
    () => ({
      items: SYSTEM_CONFIG_SECONDARY_MENU_ITEMS,
      activeKey: activeTab,
      onChange: handleSecondaryMenuChange,
      width: SYSTEM_CONFIG_SECONDARY_MENU_WIDTH,
    }),
    [activeTab, handleSecondaryMenuChange],
  );

  useLayoutSecondaryMenu(secondaryMenuConfig);

  /**
   * 加载安全配置并回填表单。
   * @returns 无返回值。
   */
  const loadSecurityConfig = useCallback(async () => {
    setSecurityLoading(true);
    try {
      const config = await getSecurityConfig();
      policyForm.setFieldsValue({
        adminMaxRetryCount: config.admin?.maxRetryCount,
        adminLockMinutes: config.admin?.lockMinutes,
        clientMaxRetryCount: config.client?.maxRetryCount,
        clientLockMinutes: config.client?.lockMinutes,
      });
      watermarkForm.setFieldsValue({
        adminWatermarkEnabled: Boolean(config.adminWatermark?.enabled),
        adminWatermarkFields: [
          ...(config.adminWatermark?.showUsername
            ? (['username'] as AdminWatermarkFieldValue[])
            : []),
          ...(config.adminWatermark?.showUserId ? (['userId'] as AdminWatermarkFieldValue[]) : []),
        ],
      });
    } catch (error) {
      console.error('加载安全配置失败:', error);
      messageApi.error('加载安全配置失败，请稍后重试');
    } finally {
      setSecurityLoading(false);
    }
  }, [messageApi, policyForm, watermarkForm]);

  /**
   * 加载软件协议配置并回填表单。
   * @returns 无返回值。
   */
  const loadAgreementConfig = useCallback(async () => {
    setAgreementLoading(true);
    try {
      const config = await getAgreementConfig();
      setAgreementConfig(config);
    } catch (error) {
      console.error('加载软件协议配置失败:', error);
      messageApi.error('加载软件协议配置失败，请稍后重试');
    } finally {
      setAgreementLoading(false);
    }
  }, [messageApi]);

  React.useEffect(() => {
    if (activeTab === 'security') {
      setPolicyEditable(false);
      setAgreementEditorType(null);
      void loadSecurityConfig();
      return;
    }
    if (activeTab === 'agreement') {
      setPolicyEditable(false);
      void loadAgreementConfig();
      return;
    }
    setPolicyEditable(false);
    setAgreementEditorType(null);
  }, [activeTab, loadAgreementConfig, loadSecurityConfig]);

  /**
   * 将表单值转换为更新请求。
   * @returns 安全配置更新请求。
   */
  const buildSecurityUpdateRequest = useCallback(
    (
      policyValues: PolicyConfigFormValues,
      watermarkValues: WatermarkConfigFormValues,
    ): SecurityConfigTypes.SecurityConfigUpdateRequest => {
      return {
        admin: {
          maxRetryCount: policyValues.adminMaxRetryCount,
          lockMinutes: policyValues.adminLockMinutes,
        },
        client: {
          maxRetryCount: policyValues.clientMaxRetryCount,
          lockMinutes: policyValues.clientLockMinutes,
        },
        adminWatermark: {
          enabled: watermarkValues.adminWatermarkEnabled,
          showUsername: watermarkValues.adminWatermarkFields.includes('username'),
          showUserId: watermarkValues.adminWatermarkFields.includes('userId'),
        },
      };
    },
    [],
  );

  /**
   * 保存登录策略配置。
   * @returns 无返回值。
   */
  const handleSavePolicyConfig = useCallback(async () => {
    const policyValues = await policyForm.validateFields();
    // 使用当前表单中最新的水印配置值（无论是原始数据还是未保存的编辑数据）
    const watermarkValues = watermarkForm.getFieldsValue();
    setPolicySaving(true);
    try {
      await updateSecurityConfig(buildSecurityUpdateRequest(policyValues, watermarkValues));
      messageApi.success('登录策略保存成功');
      setPolicyEditable(false);
      await loadSecurityConfig();
    } catch (error) {
      console.error('保存登录策略失败:', error);
    } finally {
      setPolicySaving(false);
    }
  }, [buildSecurityUpdateRequest, loadSecurityConfig, messageApi, policyForm, watermarkForm]);

  /**
   * 保存水印配置。
   * @returns 无返回值。
   */
  const handleSaveWatermarkConfig = useCallback(async () => {
    // 使用当前表单中最新的登录策略配置值
    const policyValues = policyForm.getFieldsValue();
    const watermarkValues = await watermarkForm.validateFields();
    setWatermarkSaving(true);
    try {
      await updateSecurityConfig(buildSecurityUpdateRequest(policyValues, watermarkValues));
      messageApi.success('水印配置保存成功');
      await loadSecurityConfig();
    } catch (error) {
      console.error('保存水印配置失败:', error);
    } finally {
      setWatermarkSaving(false);
    }
  }, [buildSecurityUpdateRequest, loadSecurityConfig, messageApi, policyForm, watermarkForm]);

  /**
   * 打开协议编辑页。
   * @param editorType 协议编辑类型。
   * @returns 无返回值。
   */
  const handleOpenAgreementEditor = useCallback(
    (editorType: AgreementEditorType) => {
      if (!agreementConfig) {
        return;
      }
      setAgreementEditorType(editorType);
      agreementForm.setFieldsValue({
        content:
          editorType === 'software'
            ? agreementConfig.softwareAgreementMarkdown
            : agreementConfig.privacyAgreementMarkdown,
      });
    },
    [agreementConfig, agreementForm],
  );

  /**
   * 关闭协议编辑页。
   * @returns 无返回值。
   */
  const handleCloseAgreementEditor = useCallback(() => {
    setAgreementEditorType(null);
  }, []);

  /**
   * 构建软件协议更新请求。
   * @param editorType 当前编辑类型。
   * @param markdownContent 编辑后的 Markdown 内容。
   * @returns 软件协议更新请求。
   */
  const buildAgreementUpdateRequest = useCallback(
    (
      editorType: AgreementEditorType,
      markdownContent: string,
    ): AgreementConfigTypes.AgreementConfigUpdateRequest => {
      return {
        softwareAgreementMarkdown:
          editorType === 'software' ? markdownContent : agreementConfig!.softwareAgreementMarkdown,
        privacyAgreementMarkdown:
          editorType === 'privacy' ? markdownContent : agreementConfig!.privacyAgreementMarkdown,
      };
    },
    [agreementConfig],
  );

  /**
   * 保存当前协议编辑内容。
   * @returns 无返回值。
   */
  const handleSaveAgreementEditor = useCallback(async () => {
    if (!agreementEditorType) {
      return;
    }
    const values = await agreementForm.validateFields();
    setAgreementSaving(true);
    try {
      await updateAgreementConfig(buildAgreementUpdateRequest(agreementEditorType, values.content));
      messageApi.success(`${resolveAgreementEditorTitle(agreementEditorType)}保存成功`);
      setAgreementEditorType(null);
      await loadAgreementConfig();
    } catch (error) {
      console.error('保存软件协议配置失败:', error);
      messageApi.error('保存软件协议配置失败，请稍后重试');
    } finally {
      setAgreementSaving(false);
    }
  }, [
    agreementEditorType,
    agreementForm,
    buildAgreementUpdateRequest,
    loadAgreementConfig,
    messageApi,
  ]);

  return (
    <PageContainer title={false}>
      {contextHolder}
      {activeTab === 'security' ? (
        <Spin spinning={securityLoading}>
          <Space direction="vertical" style={{ width: '100%' }} size={16}>
            <Card
              title="登录策略配置"
              extra={
                policyEditable ? (
                  <Space>
                    <Button
                      onClick={() => {
                        setPolicyEditable(false);
                        policyForm.resetFields();
                      }}
                    >
                      取消
                    </Button>
                    <PermissionButton
                      type="primary"
                      loading={policySaving}
                      access={ADMIN_PERMISSIONS.systemConfig.securityUpdate}
                      onClick={() => void handleSavePolicyConfig()}
                    >
                      保存
                    </PermissionButton>
                  </Space>
                ) : (
                  <PermissionButton
                    access={ADMIN_PERMISSIONS.systemConfig.securityUpdate}
                    onClick={() => setPolicyEditable(true)}
                  >
                    编辑
                  </PermissionButton>
                )
              }
            >
              <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
                连续失败达到阈值后将锁定对应账号；管理端和客户端独立计数、独立锁定。
              </Typography.Text>
              <Form layout="vertical" form={policyForm}>
                <Row gutter={48}>
                  <Col xs={24} lg={12}>
                    <Typography.Title level={5} style={{ marginTop: 0, marginBottom: 16 }}>
                      管理端安全策略
                    </Typography.Title>
                    <Form.Item
                      label="连续失败阈值"
                      name="adminMaxRetryCount"
                      rules={[
                        { required: true, message: '请输入管理端连续失败阈值' },
                        {
                          type: 'number',
                          min: 1,
                          max: 20,
                          message: '管理端连续失败阈值范围为1-20',
                        },
                      ]}
                    >
                      <InputNumber
                        autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                        min={1}
                        max={20}
                        precision={0}
                        disabled={!policyEditable}
                        style={{ width: '100%' }}
                      />
                    </Form.Item>
                    <Form.Item
                      label="锁定时长（分钟）"
                      name="adminLockMinutes"
                      rules={[
                        { required: true, message: '请输入管理端锁定时长' },
                        {
                          type: 'number',
                          min: 1,
                          max: 1440,
                          message: '管理端锁定时长范围为1-1440分钟',
                        },
                      ]}
                    >
                      <InputNumber
                        autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                        min={1}
                        max={1440}
                        precision={0}
                        disabled={!policyEditable}
                        style={{ width: '100%' }}
                      />
                    </Form.Item>
                  </Col>

                  <Col xs={24} lg={12}>
                    <Typography.Title level={5} style={{ marginTop: 0, marginBottom: 16 }}>
                      客户端安全策略
                    </Typography.Title>
                    <Form.Item
                      label="连续失败阈值"
                      name="clientMaxRetryCount"
                      rules={[
                        { required: true, message: '请输入客户端连续失败阈值' },
                        {
                          type: 'number',
                          min: 1,
                          max: 20,
                          message: '客户端连续失败阈值范围为1-20',
                        },
                      ]}
                    >
                      <InputNumber
                        autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                        min={1}
                        max={20}
                        precision={0}
                        disabled={!policyEditable}
                        style={{ width: '100%' }}
                      />
                    </Form.Item>
                    <Form.Item
                      label="锁定时长（分钟）"
                      name="clientLockMinutes"
                      rules={[
                        { required: true, message: '请输入客户端锁定时长' },
                        {
                          type: 'number',
                          min: 1,
                          max: 1440,
                          message: '客户端锁定时长范围为1-1440分钟',
                        },
                      ]}
                    >
                      <InputNumber
                        autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                        min={1}
                        max={1440}
                        precision={0}
                        disabled={!policyEditable}
                        style={{ width: '100%' }}
                      />
                    </Form.Item>
                  </Col>
                </Row>
              </Form>
            </Card>

            <Card
              title="管理端水印配置"
              extra={
                <PermissionButton
                  type="primary"
                  loading={watermarkSaving}
                  access={ADMIN_PERMISSIONS.systemConfig.securityUpdate}
                  onClick={() => void handleSaveWatermarkConfig()}
                >
                  保存
                </PermissionButton>
              }
            >
              <Form layout="vertical" form={watermarkForm} style={{ maxWidth: 600 }}>
                <Form.Item
                  label="启用管理端水印"
                  name="adminWatermarkEnabled"
                  valuePropName="checked"
                  tooltip="开启后将在管理端页面按配置显示登录人信息水印"
                >
                  <Switch checkedChildren="开启" unCheckedChildren="关闭" />
                </Form.Item>
                <Form.Item
                  noStyle
                  shouldUpdate={(prevValues, nextValues) =>
                    prevValues.adminWatermarkEnabled !== nextValues.adminWatermarkEnabled
                  }
                >
                  {({ getFieldValue }) => {
                    const enabled = getFieldValue('adminWatermarkEnabled');
                    return (
                      <Form.Item
                        label="水印显示内容"
                        name="adminWatermarkFields"
                        extra="可选择只显示用户ID、只显示用户名，或同时显示二者"
                        rules={[
                          {
                            validator: async (_, value: AdminWatermarkFieldValue[] | undefined) => {
                              if (!enabled) {
                                return;
                              }
                              if (value && value.length > 0) {
                                return;
                              }
                              throw new Error('请至少选择一种水印显示内容');
                            },
                          },
                        ]}
                      >
                        <Checkbox.Group
                          options={ADMIN_WATERMARK_FIELD_OPTIONS.map((option) => ({
                            ...option,
                            disabled: !enabled,
                          }))}
                        />
                      </Form.Item>
                    );
                  }}
                </Form.Item>
              </Form>
            </Card>
          </Space>
        </Spin>
      ) : activeTab === 'esIndex' ? (
        <EsIndexSection />
      ) : agreementEditorType ? (
        <div style={{ minHeight: AGREEMENT_SECTION_MIN_HEIGHT }}>
          <Space direction="vertical" style={{ width: '100%' }} size={16}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Typography.Title level={5} style={{ margin: 0 }}>
                {resolveAgreementEditorTitle(agreementEditorType)}编辑
              </Typography.Title>
              <Space>
                <Button onClick={handleCloseAgreementEditor}>取消</Button>
                <PermissionButton
                  type="primary"
                  loading={agreementSaving}
                  access={ADMIN_PERMISSIONS.systemConfig.agreementUpdate}
                  onClick={() => void handleSaveAgreementEditor()}
                >
                  保存
                </PermissionButton>
              </Space>
            </div>
            <Form layout="vertical" form={agreementForm}>
              <Form.Item
                label={`${resolveAgreementEditorTitle(agreementEditorType)}内容`}
                name="content"
                rules={[
                  {
                    required: true,
                    message: `请输入${resolveAgreementEditorTitle(agreementEditorType)}内容`,
                  },
                ]}
              >
                <RichTextEditor
                  contentFormat="markdown"
                  placeholder={`请输入${resolveAgreementEditorTitle(agreementEditorType)}内容（Markdown）`}
                  height={AGREEMENT_EDITOR_RICH_TEXT_EDITOR_HEIGHT}
                />
              </Form.Item>
            </Form>
          </Space>
        </div>
      ) : (
        <Spin spinning={agreementLoading}>
          <div style={{ minHeight: AGREEMENT_SECTION_MIN_HEIGHT }}>
            <Space direction="vertical" style={{ width: '100%' }} size={16}>
              <Row gutter={16}>
                <Col xs={24} lg={12}>
                  <Card
                    title="软件协议"
                    extra={
                      <PermissionButton
                        access={ADMIN_PERMISSIONS.systemConfig.agreementUpdate}
                        onClick={() => handleOpenAgreementEditor('software')}
                      >
                        编辑
                      </PermissionButton>
                    }
                    type="inner"
                  >
                    <div style={{ height: AGREEMENT_PREVIEW_HEIGHT, overflow: 'auto' }}>
                      {agreementConfig ? (
                        <XMarkdown
                          className={markdownThemeClassName}
                          content={agreementConfig.softwareAgreementMarkdown}
                          paragraphTag="div"
                          openLinksInNewTab
                        />
                      ) : null}
                    </div>
                  </Card>
                </Col>
                <Col xs={24} lg={12}>
                  <Card
                    title="隐私协议"
                    extra={
                      <PermissionButton
                        access={ADMIN_PERMISSIONS.systemConfig.agreementUpdate}
                        onClick={() => handleOpenAgreementEditor('privacy')}
                      >
                        编辑
                      </PermissionButton>
                    }
                    type="inner"
                  >
                    <div style={{ height: AGREEMENT_PREVIEW_HEIGHT, overflow: 'auto' }}>
                      {agreementConfig ? (
                        <XMarkdown
                          className={markdownThemeClassName}
                          content={agreementConfig.privacyAgreementMarkdown}
                          paragraphTag="div"
                          openLinksInNewTab
                        />
                      ) : null}
                    </div>
                  </Card>
                </Col>
              </Row>
            </Space>
          </div>
        </Spin>
      )}
    </PageContainer>
  );
};

export default SystemConfigPage;
