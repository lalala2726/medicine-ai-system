/* eslint-disable react-refresh/only-export-components */
/**
 * 路由配置
 * 将 UmiJS 的配置式路由转换为 React Router v7 格式
 */
import React, { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { Spin } from 'antd';
import AppLayout from '../layouts/AppLayout';
import AuthGuard from '../layouts/AuthGuard';
import PermissionRoute from '../layouts/PermissionRoute';
import {
  AGENT_OBSERVABILITY_ROUTE_PERMISSIONS,
  ADMIN_PERMISSIONS,
  COUPON_ROUTE_PERMISSIONS,
  LLM_SYSTEM_MODELS_ROUTE_PERMISSIONS,
  SYSTEM_CONFIG_ROUTE_PERMISSIONS,
} from '@/constants/permissions';
import { useUserStore } from '@/store';
import { canAccessByPermissions } from '@/utils/permission';
import { routePaths } from './paths';

// 页面懒加载
const Login = lazy(() => import('@/pages/login'));
const AccountProfile = lazy(() => import('@/pages/account/profile'));
const Analytics = lazy(() => import('@/pages/analytics'));
const SmartAssistant = lazy(() => import('@/pages/SmartAssistant'));
const LlmKnowledgeBase = lazy(() => import('@/pages/llm-manage/knowledge-base'));
const LlmKnowledgeBaseDocument = lazy(() => import('@/pages/llm-manage/knowledge-base-document'));
const LlmKnowledgeBaseSearch = lazy(() => import('@/pages/llm-manage/knowledge-base-search'));
const LlmKnowledgeBaseImport = lazy(() => import('@/pages/llm-manage/knowledge-base-import'));
const LlmKnowledgeBaseChunk = lazy(() => import('@/pages/llm-manage/knowledge-base-chunk'));
const LlmModelProviders = lazy(() => import('@/pages/llm-manage/model-providers'));
const LlmModelProviderEditor = lazy(() => import('@/pages/llm-manage/model-providers/editor'));
const LlmSystemModelsAdminConfig = lazy(
  () => import('@/pages/llm-manage/system-models/admin-config'),
);
const LlmSystemModelsClientConfig = lazy(
  () => import('@/pages/llm-manage/system-models/client-config'),
);
const LlmSystemModelsCommonCapability = lazy(
  () => import('@/pages/llm-manage/system-models/common-capability'),
);
const LlmPromptManage = lazy(() => import('@/pages/llm-manage/prompt-manage'));
const LlmPromptManageEdit = lazy(() => import('@/pages/llm-manage/prompt-manage/edit'));
const LlmPromptManageHistory = lazy(() => import('@/pages/llm-manage/prompt-manage/history'));
const LlmAgentTrace = lazy(() => import('@/pages/llm-manage/agent-trace'));
const LlmAgentMonitor = lazy(() => import('@/pages/llm-manage/agent-monitor'));
const LlmAgentMonitorModelDetail = lazy(
  () => import('@/pages/llm-manage/agent-monitor/model-detail'),
);
const ProductManage = lazy(() => import('@/pages/mall/product-list'));
const ProductEdit = lazy(() => import('@/pages/mall/product-form'));
const ProductCategory = lazy(() => import('@/pages/mall/product-category'));
const ProductTag = lazy(() => import('@/pages/mall/product-tag'));
const ProductTagByType = lazy(() => import('@/pages/mall/product-tag/type-tags'));
const MallCoupon = lazy(() => import('@/pages/mall/coupon'));
const MallCouponIssue = lazy(() => import('@/pages/mall/coupon/issue'));
const MallCouponActivationCodes = lazy(() => import('@/pages/mall/coupon/activation-codes'));
const OrderList = lazy(() => import('@/pages/mall/order-list'));
const AfterSales = lazy(() => import('@/pages/mall/after-sales'));
const SystemUser = lazy(() => import('@/pages/system/user'));
const SystemUserAssets = lazy(() => import('@/pages/system/user/assets'));
const SystemRole = lazy(() => import('@/pages/system/role'));
const SystemRolePermission = lazy(() => import('@/pages/system/role-permission'));
const SystemPermission = lazy(() => import('@/pages/system/permission'));
const SystemConfig = lazy(() => import('@/pages/system/config'));
const SystemLogOperation = lazy(() => import('@/pages/system-log/operation-log'));
const SystemLogLogin = lazy(() => import('@/pages/system-log/login-log'));
const NotFound = lazy(() => import('@/pages/404'));

// 懒加载包装器（路由切换不显示加载动画，避免闪烁）
function LazyLoad({ children }: { children: React.ReactNode }) {
  return <Suspense fallback={null}>{children}</Suspense>;
}

// 支持自定义文字的懒加载包装器（解决进入时白屏体验不好的问题）
interface CustomLazyLoadProps {
  children: React.ReactNode;
  title?: string;
  subTitle?: string;
}

function CustomLazyLoad({ children, title = '正在加载中', subTitle }: CustomLazyLoadProps) {
  // 考虑到首次加载时，CSS变量可能还未注入，我们通过 localStorage 判断主题，作为 fallback 色
  const isDark = localStorage.getItem('app-nav-theme') === 'realDark';

  return (
    <Suspense
      fallback={
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100%',
            minHeight: '362px',
            backgroundColor: isDark ? '#141414' : 'var(--ant-color-bg-layout, #f5f5f5)',
            color: isDark ? 'rgba(255,255,255,0.85)' : 'var(--ant-color-text, rgba(0,0,0,0.85))',
          }}
        >
          <div style={{ padding: 26 }}>
            <Spin size="large" />
          </div>
          <div style={{ fontSize: '1.1rem' }}>{title}</div>
          {subTitle && (
            <div
              style={{
                marginTop: 20,
                fontSize: '1rem',
                color: isDark ? 'rgba(255,255,255,0.45)' : 'var(--ant-color-text-secondary, #888)',
              }}
            >
              {subTitle}
            </div>
          )}
        </div>
      }
    >
      {children}
    </Suspense>
  );
}

/**
 * 构建普通权限路由节点。
 * @param children 页面节点。
 * @param access 页面需要的权限编码。
 * @returns 包含懒加载与权限守卫的路由节点。
 */
function buildPermissionElement(children: React.ReactNode, access?: string | string[]) {
  return (
    <PermissionRoute access={access}>
      <LazyLoad>{children}</LazyLoad>
    </PermissionRoute>
  );
}

/**
 * 构建自定义加载提示的权限路由节点。
 * @param children 页面节点。
 * @param access 页面需要的权限编码。
 * @param title 加载标题。
 * @returns 包含自定义懒加载与权限守卫的路由节点。
 */
function buildCustomPermissionElement(
  children: React.ReactNode,
  access?: string | string[],
  title = '正在加载中',
) {
  return (
    <PermissionRoute access={access}>
      <CustomLazyLoad title={title}>{children}</CustomLazyLoad>
    </PermissionRoute>
  );
}

/**
 * 菜单路由配置
 * 用于 ProLayout 渲染菜单
 */
export interface MenuRoute {
  path: string;
  name?: string;
  icon?: string;
  hideInMenu?: boolean;
  access?: string | string[];
  redirect?: string;
  children?: MenuRoute[];
}

export const menuRoutes: MenuRoute[] = [
  {
    path: routePaths.analytics,
    name: '运营分析',
    icon: 'dashboard',
    access: ADMIN_PERMISSIONS.analytics.query,
  },
  {
    path: routePaths.mall,
    name: '商城管理',
    icon: 'shop',
    access: 'mall',
    children: [
      {
        path: routePaths.mallProductList,
        name: '商品列表',
        icon: 'shopping',
        access: ADMIN_PERMISSIONS.mallProduct.list,
      },
      {
        path: routePaths.mallProductCreate,
        name: '新增商品',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.mallProduct.add,
      },
      {
        path: `${routePaths.mallProductEdit}/:id`,
        name: '编辑商品',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.mallProduct.edit,
      },
      {
        path: routePaths.mallProductCategory,
        name: '商品分类',
        icon: 'product-category',
        access: ADMIN_PERMISSIONS.mallCategory.tree,
      },
      {
        path: routePaths.mallOrderList,
        name: '订单列表',
        icon: 'order',
        access: ADMIN_PERMISSIONS.mallOrder.list,
      },
      {
        path: routePaths.mallAfterSales,
        name: '售后管理',
        icon: 'after-sales-management',
        access: ADMIN_PERMISSIONS.mallAfterSale.list,
      },
      {
        path: routePaths.mallProductTag,
        name: '商品标签',
        icon: 'tag',
        access: ADMIN_PERMISSIONS.mallProductTag.list,
      },
      {
        path: `${routePaths.mallProductTagByType}/:id`,
        name: '类型标签列表',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.mallProductTag.list,
      },
      {
        path: routePaths.mallCoupon,
        name: '优惠券管理',
        icon: 'coupon',
        access: COUPON_ROUTE_PERMISSIONS,
      },
      {
        path: routePaths.mallCouponIssue,
        name: '优惠券发券',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.mallCoupon.issue,
      },
      {
        path: `${routePaths.mallCouponActivationCodes}/:batchId`,
        name: '激活码列表',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.mallCoupon.activationBatchQuery,
      },
    ],
  },
  {
    path: routePaths.system,
    name: '系统管理',
    icon: 'setting',
    access: 'system',
    children: [
      {
        path: routePaths.systemUser,
        name: '用户管理',
        icon: 'user',
        access: ADMIN_PERMISSIONS.systemUser.list,
      },
      {
        path: `${routePaths.systemUserAssets}/:id`,
        name: '用户资产明细',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.systemUser.query,
      },
      {
        path: routePaths.systemRole,
        name: '角色管理',
        icon: 'team',
        access: ADMIN_PERMISSIONS.systemRole.list,
      },
      {
        path: `${routePaths.systemRolePermission}/:id`,
        name: '分配权限',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.systemRole.update,
      },
      {
        path: routePaths.systemPermission,
        name: '权限管理',
        icon: 'lock',
        access: ADMIN_PERMISSIONS.systemPermission.list,
      },
      {
        path: routePaths.systemConfig,
        name: '系统配置',
        icon: 'setting',
        access: SYSTEM_CONFIG_ROUTE_PERMISSIONS,
      },
    ],
  },
  {
    path: routePaths.systemLog,
    name: '系统日志',
    icon: 'file-search',
    access: 'system:log',
    children: [
      {
        path: routePaths.systemLogOperation,
        name: '操作日志',
        icon: 'operation-log',
        access: ADMIN_PERMISSIONS.systemLog.operationList,
      },
      {
        path: routePaths.systemLogLogin,
        name: '登录日志',
        icon: 'login-log',
        access: ADMIN_PERMISSIONS.systemLog.loginList,
      },
    ],
  },
  {
    path: routePaths.llmManage,
    name: '大模型管理',
    icon: 'api',
    access: 'system:llm',
    children: [
      {
        path: routePaths.llmKnowledgeBase,
        name: '知识库',
        icon: 'book',
        access: ADMIN_PERMISSIONS.knowledgeBase.list,
      },
      {
        path: routePaths.llmKnowledgeBaseDocument,
        name: '知识库文档',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.kbDocument.list,
      },
      {
        path: routePaths.llmKnowledgeBaseSearch,
        name: '知识检索',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.knowledgeBase.query,
      },
      {
        path: routePaths.llmKnowledgeBaseImport,
        name: '导入知识',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.kbDocument.import,
      },
      {
        path: routePaths.llmKnowledgeBaseChunk,
        name: '切片列表',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.kbDocumentChunk.list,
      },
      {
        path: routePaths.llmModelProviders,
        name: '模型提供商',
        icon: 'provider',
        access: ADMIN_PERMISSIONS.llmProvider.list,
      },
      {
        path: routePaths.llmModelProviderCreate,
        name: '新增模型提供商',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.llmProvider.add,
      },
      {
        path: `${routePaths.llmModelProviderEdit}/:id`,
        name: '编辑模型提供商',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.llmProvider.update,
      },
      {
        path: routePaths.llmSystemModels,
        name: '系统模型配置',
        icon: 'llms-settings',
        access: LLM_SYSTEM_MODELS_ROUTE_PERMISSIONS,
      },
      {
        path: routePaths.llmPromptManage,
        name: '提示词管理',
        icon: 'prompt',
        access: ADMIN_PERMISSIONS.agentPrompt.list,
      },
      {
        path: routePaths.llmPromptManageEdit,
        name: '编辑提示词',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.agentPrompt.update,
      },
      {
        path: routePaths.llmPromptManageHistory,
        name: '提示词历史',
        hideInMenu: true,
        access: ADMIN_PERMISSIONS.agentPrompt.query,
      },
      {
        path: routePaths.llmAgentObservability,
        name: '智能体观测',
        icon: 'dashboard',
        access: AGENT_OBSERVABILITY_ROUTE_PERMISSIONS,
      },
    ],
  },
];

/**
 * 查找第一个有权限进入的可见菜单路径。
 * @param routes 菜单路由列表。
 * @param user 当前用户信息。
 * @returns 第一个可访问菜单路径。
 */
function findFirstAccessibleMenuPath(
  routes: MenuRoute[],
  user: API.CurrentUser | null | undefined,
): string | null {
  for (const route of routes) {
    const childPath = route.children?.length
      ? findFirstAccessibleMenuPath(route.children, user)
      : null;

    if (childPath) {
      return childPath;
    }

    if (
      !route.hideInMenu &&
      !route.children?.length &&
      canAccessByPermissions(user, route.access)
    ) {
      return route.path;
    }
  }

  return null;
}

/**
 * 首页权限跳转组件。
 * @returns 跳转到第一个可访问菜单或 403。
 */
const IndexRedirect: React.FC = () => {
  const user = useUserStore((state) => state.user);
  const firstAccessiblePath = findFirstAccessibleMenuPath(menuRoutes, user);
  /** 当前用户可访问顶部智能助手时的首页跳转路径。 */
  const smartAssistantEntryPath = canAccessByPermissions(
    user,
    ADMIN_PERMISSIONS.smartAssistant.access,
  )
    ? routePaths.smartAssistant
    : null;

  if (!firstAccessiblePath) {
    if (smartAssistantEntryPath) {
      return <Navigate to={smartAssistantEntryPath} replace />;
    }

    return buildPermissionElement(<NotFound />, '__no_menu_permission__');
  }

  return <Navigate to={firstAccessiblePath} replace />;
};

/**
 * 菜单分组首页跳转组件属性。
 */
interface MenuGroupRedirectProps {
  /** 分组菜单路径。 */
  basePath: string;
}

/**
 * 菜单分组首页权限跳转组件。
 * @param props 分组跳转参数。
 * @returns 跳转到分组内第一个可访问菜单或 403。
 */
const MenuGroupRedirect: React.FC<MenuGroupRedirectProps> = ({ basePath }) => {
  const user = useUserStore((state) => state.user);
  const groupRoute = menuRoutes.find((route) => route.path === basePath);
  const firstAccessiblePath = groupRoute?.children
    ? findFirstAccessibleMenuPath(groupRoute.children, user)
    : null;

  if (!firstAccessiblePath) {
    return buildPermissionElement(<NotFound />, '__no_group_permission__');
  }

  return <Navigate to={firstAccessiblePath} replace />;
};

/**
 * 权限跳转选项。
 */
interface PermissionRedirectOption {
  /** 跳转目标路径。 */
  path: string;
  /** 跳转目标需要的权限编码。 */
  access: string | string[];
}

/**
 * 按权限跳转到第一个可访问目标。
 * @param options 跳转选项集合。
 * @param user 当前用户信息。
 * @returns 第一个可访问目标路径。
 */
function findFirstAccessibleRedirectPath(
  options: PermissionRedirectOption[],
  user: API.CurrentUser | null | undefined,
): string | null {
  return options.find((option) => canAccessByPermissions(user, option.access))?.path ?? null;
}

/**
 * 权限优先跳转组件属性。
 */
interface PermissionRedirectProps {
  /** 跳转选项集合。 */
  options: PermissionRedirectOption[];
}

/**
 * 权限优先跳转组件。
 * @param props 跳转配置。
 * @returns 跳转节点或 403。
 */
const PermissionRedirect: React.FC<PermissionRedirectProps> = ({ options }) => {
  const user = useUserStore((state) => state.user);
  const firstAccessiblePath = findFirstAccessibleRedirectPath(options, user);

  if (!firstAccessiblePath) {
    return buildPermissionElement(<NotFound />, '__no_redirect_permission__');
  }

  return <Navigate to={firstAccessiblePath} replace />;
};

/** 系统模型配置默认跳转选项。 */
const LLM_SYSTEM_MODEL_REDIRECT_OPTIONS: PermissionRedirectOption[] = [
  {
    path: routePaths.llmSystemModelsAdminConfig,
    access: ADMIN_PERMISSIONS.agentConfig.adminQuery,
  },
  {
    path: routePaths.llmSystemModelsClientConfig,
    access: ADMIN_PERMISSIONS.agentConfig.clientQuery,
  },
  {
    path: routePaths.llmSystemModelsCommonCapability,
    access: ADMIN_PERMISSIONS.agentConfig.commonQuery,
  },
];

/** 智能体观测默认跳转选项。 */
const LLM_AGENT_OBSERVABILITY_REDIRECT_OPTIONS: PermissionRedirectOption[] = [
  {
    path: routePaths.llmAgentMonitor,
    access: ADMIN_PERMISSIONS.agentTrace.monitor,
  },
  {
    path: routePaths.llmAgentTrace,
    access: ADMIN_PERMISSIONS.agentTrace.list,
  },
];

/**
 * React Router 路由表
 */
export const router = createBrowserRouter([
  // 登录页（无 Layout）
  {
    path: '/user/login',
    element: (
      <LazyLoad>
        <Login />
      </LazyLoad>
    ),
  },
  // 主应用（带 Layout 和权限守卫）
  {
    path: '/',
    element: (
      <AuthGuard>
        <AppLayout />
      </AuthGuard>
    ),
    children: [
      { index: true, element: <IndexRedirect /> },
      {
        path: 'account/profile',
        element: buildPermissionElement(<AccountProfile />),
      },
      {
        path: 'analytics',
        element: buildPermissionElement(<Analytics />, ADMIN_PERMISSIONS.analytics.query),
      },
      {
        path: 'smart-assistant',
        element: buildCustomPermissionElement(
          <SmartAssistant />,
          ADMIN_PERMISSIONS.smartAssistant.access,
        ),
      },
      {
        path: 'smart-assistant/:conversationId',
        element: buildCustomPermissionElement(
          <SmartAssistant />,
          ADMIN_PERMISSIONS.smartAssistant.access,
        ),
      },
      {
        path: 'llm-manage',
        children: [
          {
            index: true,
            element: <MenuGroupRedirect basePath={routePaths.llmManage} />,
          },
          {
            path: 'knowledge-base',
            element: buildPermissionElement(
              <LlmKnowledgeBase />,
              ADMIN_PERMISSIONS.knowledgeBase.list,
            ),
          },
          {
            path: 'knowledge-base-document',
            element: buildPermissionElement(
              <LlmKnowledgeBaseDocument />,
              ADMIN_PERMISSIONS.kbDocument.list,
            ),
          },
          {
            path: 'knowledge-base-search',
            element: buildPermissionElement(
              <LlmKnowledgeBaseSearch />,
              ADMIN_PERMISSIONS.knowledgeBase.query,
            ),
          },
          {
            path: 'knowledge-base-import',
            element: buildPermissionElement(
              <LlmKnowledgeBaseImport />,
              ADMIN_PERMISSIONS.kbDocument.import,
            ),
          },
          {
            path: 'knowledge-base-chunk',
            element: buildPermissionElement(
              <LlmKnowledgeBaseChunk />,
              ADMIN_PERMISSIONS.kbDocumentChunk.list,
            ),
          },
          {
            path: 'model-providers',
            element: buildPermissionElement(
              <LlmModelProviders />,
              ADMIN_PERMISSIONS.llmProvider.list,
            ),
          },
          {
            path: 'model-providers/create',
            element: buildPermissionElement(
              <LlmModelProviderEditor />,
              ADMIN_PERMISSIONS.llmProvider.add,
            ),
          },
          {
            path: 'model-providers/edit/:id',
            element: buildPermissionElement(
              <LlmModelProviderEditor />,
              ADMIN_PERMISSIONS.llmProvider.update,
            ),
          },
          {
            path: 'system-models',
            children: [
              {
                index: true,
                element: <PermissionRedirect options={LLM_SYSTEM_MODEL_REDIRECT_OPTIONS} />,
              },
              {
                path: 'admin-config',
                element: buildPermissionElement(
                  <LlmSystemModelsAdminConfig />,
                  ADMIN_PERMISSIONS.agentConfig.adminQuery,
                ),
              },
              {
                path: 'client-config',
                element: buildPermissionElement(
                  <LlmSystemModelsClientConfig />,
                  ADMIN_PERMISSIONS.agentConfig.clientQuery,
                ),
              },
              {
                path: 'common-capability',
                element: buildPermissionElement(
                  <LlmSystemModelsCommonCapability />,
                  ADMIN_PERMISSIONS.agentConfig.commonQuery,
                ),
              },
            ],
          },
          {
            path: 'prompt-manage',
            element: buildPermissionElement(
              <LlmPromptManage />,
              ADMIN_PERMISSIONS.agentPrompt.list,
            ),
          },
          {
            path: 'prompt-manage/edit',
            element: buildPermissionElement(
              <LlmPromptManageEdit />,
              ADMIN_PERMISSIONS.agentPrompt.update,
            ),
          },
          {
            path: 'prompt-manage/history',
            element: buildPermissionElement(
              <LlmPromptManageHistory />,
              ADMIN_PERMISSIONS.agentPrompt.query,
            ),
          },
          {
            path: 'agent-observability',
            children: [
              {
                index: true,
                element: <PermissionRedirect options={LLM_AGENT_OBSERVABILITY_REDIRECT_OPTIONS} />,
              },
              {
                path: 'monitor',
                element: buildPermissionElement(
                  <LlmAgentMonitor />,
                  ADMIN_PERMISSIONS.agentTrace.monitor,
                ),
              },
              {
                path: 'monitor/model-detail',
                element: buildPermissionElement(
                  <LlmAgentMonitorModelDetail />,
                  ADMIN_PERMISSIONS.agentTrace.monitor,
                ),
              },
              {
                path: 'trace',
                element: buildPermissionElement(
                  <LlmAgentTrace />,
                  ADMIN_PERMISSIONS.agentTrace.list,
                ),
              },
            ],
          },
        ],
      },
      {
        path: 'mall',
        children: [
          { index: true, element: <MenuGroupRedirect basePath={routePaths.mall} /> },
          {
            path: 'product-list',
            element: buildPermissionElement(<ProductManage />, ADMIN_PERMISSIONS.mallProduct.list),
          },
          {
            path: 'product-create',
            element: buildPermissionElement(<ProductEdit />, ADMIN_PERMISSIONS.mallProduct.add),
          },
          {
            path: 'product-edit/:id',
            element: buildPermissionElement(<ProductEdit />, ADMIN_PERMISSIONS.mallProduct.edit),
          },
          {
            path: 'product-category',
            element: buildPermissionElement(
              <ProductCategory />,
              ADMIN_PERMISSIONS.mallCategory.tree,
            ),
          },
          {
            path: 'product-tag',
            element: buildPermissionElement(<ProductTag />, ADMIN_PERMISSIONS.mallProductTag.list),
          },
          {
            path: 'product-tag/type/:id',
            element: buildPermissionElement(
              <ProductTagByType />,
              ADMIN_PERMISSIONS.mallProductTag.list,
            ),
          },
          {
            path: 'coupon',
            element: buildPermissionElement(<MallCoupon />, COUPON_ROUTE_PERMISSIONS),
          },
          {
            path: 'coupon/template',
            element: buildPermissionElement(
              <MallCoupon />,
              ADMIN_PERMISSIONS.mallCoupon.templateList,
            ),
          },
          {
            path: 'coupon/activation-manage',
            element: buildPermissionElement(
              <MallCoupon />,
              ADMIN_PERMISSIONS.mallCoupon.activationBatchList,
            ),
          },
          {
            path: 'coupon/log',
            element: buildPermissionElement(<MallCoupon />, ADMIN_PERMISSIONS.mallCoupon.logList),
          },
          {
            path: 'coupon/activation-log',
            element: buildPermissionElement(
              <MallCoupon />,
              ADMIN_PERMISSIONS.mallCoupon.activationLogList,
            ),
          },
          {
            path: 'coupon/issue',
            element: buildPermissionElement(
              <MallCouponIssue />,
              ADMIN_PERMISSIONS.mallCoupon.issue,
            ),
          },
          {
            path: 'coupon/activation-codes/:batchId',
            element: buildPermissionElement(
              <MallCouponActivationCodes />,
              ADMIN_PERMISSIONS.mallCoupon.activationBatchQuery,
            ),
          },
          {
            path: 'order-list',
            element: buildPermissionElement(<OrderList />, ADMIN_PERMISSIONS.mallOrder.list),
          },
          {
            path: 'after-sales',
            element: buildPermissionElement(<AfterSales />, ADMIN_PERMISSIONS.mallAfterSale.list),
          },
        ],
      },
      {
        path: 'system',
        children: [
          { index: true, element: <MenuGroupRedirect basePath={routePaths.system} /> },
          {
            path: 'user',
            element: buildPermissionElement(<SystemUser />, ADMIN_PERMISSIONS.systemUser.list),
          },
          {
            path: 'user/assets/:id',
            element: buildPermissionElement(
              <SystemUserAssets />,
              ADMIN_PERMISSIONS.systemUser.query,
            ),
          },
          {
            path: 'role',
            element: buildPermissionElement(<SystemRole />, ADMIN_PERMISSIONS.systemRole.list),
          },
          {
            path: 'role-permission/:id',
            element: buildPermissionElement(
              <SystemRolePermission />,
              ADMIN_PERMISSIONS.systemRole.update,
            ),
          },
          {
            path: 'permission',
            element: buildPermissionElement(
              <SystemPermission />,
              ADMIN_PERMISSIONS.systemPermission.list,
            ),
          },
          {
            path: 'config',
            element: buildPermissionElement(<SystemConfig />, SYSTEM_CONFIG_ROUTE_PERMISSIONS),
          },
          {
            path: 'config/security',
            element: buildPermissionElement(
              <SystemConfig />,
              ADMIN_PERMISSIONS.systemConfig.securityQuery,
            ),
          },
          {
            path: 'config/agreement',
            element: buildPermissionElement(
              <SystemConfig />,
              ADMIN_PERMISSIONS.systemConfig.agreementQuery,
            ),
          },
          {
            path: 'config/es-index',
            element: buildPermissionElement(
              <SystemConfig />,
              ADMIN_PERMISSIONS.systemConfig.esIndexQuery,
            ),
          },
        ],
      },
      {
        path: 'system-log',
        children: [
          { index: true, element: <MenuGroupRedirect basePath={routePaths.systemLog} /> },
          {
            path: 'operation-log',
            element: buildPermissionElement(
              <SystemLogOperation />,
              ADMIN_PERMISSIONS.systemLog.operationList,
            ),
          },
          {
            path: 'login-log',
            element: buildPermissionElement(
              <SystemLogLogin />,
              ADMIN_PERMISSIONS.systemLog.loginList,
            ),
          },
        ],
      },
      // 404
      {
        path: '*',
        element: (
          <LazyLoad>
            <NotFound />
          </LazyLoad>
        ),
      },
    ],
  },
]);
