type RouteQueryValue = string | number | null | undefined;

/** 优惠券二级菜单路由 key。 */
export type MallCouponSecondaryRouteKey = 'template' | 'activationCode' | 'log' | 'activationLog';
/** 系统配置二级菜单路由 key。 */
export type SystemConfigSecondaryRouteKey = 'security' | 'agreement' | 'esIndex';
/** 系统模型配置二级菜单路由 key。 */
export type LlmSystemModelsSecondaryRouteKey = 'adminConfig' | 'clientConfig' | 'commonCapability';
/** 智能体观测二级菜单路由 key。 */
export type LlmAgentObservabilitySecondaryRouteKey = 'monitor' | 'trace';

export const routePaths = {
  login: '/user/login',
  accountProfile: '/account/profile',
  analytics: '/analytics',
  smartAssistant: '/smart-assistant',
  llmManage: '/llm-manage',
  llmKnowledgeBase: '/llm-manage/knowledge-base',
  llmKnowledgeBaseDocument: '/llm-manage/knowledge-base-document',
  llmKnowledgeBaseSearch: '/llm-manage/knowledge-base-search',
  llmKnowledgeBaseImport: '/llm-manage/knowledge-base-import',
  llmKnowledgeBaseChunk: '/llm-manage/knowledge-base-chunk',
  llmModelProviders: '/llm-manage/model-providers',
  llmModelProviderCreate: '/llm-manage/model-providers/create',
  llmModelProviderEdit: '/llm-manage/model-providers/edit',
  llmSystemModels: '/llm-manage/system-models',
  llmSystemModelsAdminConfig: '/llm-manage/system-models/admin-config',
  llmSystemModelsClientConfig: '/llm-manage/system-models/client-config',
  llmSystemModelsCommonCapability: '/llm-manage/system-models/common-capability',
  llmPromptManage: '/llm-manage/prompt-manage',
  llmPromptManageEdit: '/llm-manage/prompt-manage/edit',
  llmPromptManageHistory: '/llm-manage/prompt-manage/history',
  llmAgentObservability: '/llm-manage/agent-observability',
  llmAgentMonitor: '/llm-manage/agent-observability/monitor',
  llmAgentMonitorModelDetail: '/llm-manage/agent-observability/monitor/model-detail',
  llmAgentTrace: '/llm-manage/agent-observability/trace',
  mall: '/mall',
  mallProductList: '/mall/product-list',
  mallProductCreate: '/mall/product-create',
  mallProductEdit: '/mall/product-edit',
  mallProductCategory: '/mall/product-category',
  mallProductTag: '/mall/product-tag',
  mallProductTagByType: '/mall/product-tag/type',
  mallCoupon: '/mall/coupon',
  mallCouponTemplate: '/mall/coupon/template',
  mallCouponActivationManage: '/mall/coupon/activation-manage',
  mallCouponLog: '/mall/coupon/log',
  mallCouponActivationLog: '/mall/coupon/activation-log',
  mallCouponIssue: '/mall/coupon/issue',
  mallCouponActivationCodes: '/mall/coupon/activation-codes',
  mallOrderList: '/mall/order-list',
  mallAfterSales: '/mall/after-sales',
  system: '/system',
  systemUser: '/system/user',
  systemUserAssets: '/system/user/assets',
  systemRole: '/system/role',
  systemRolePermission: '/system/role-permission',
  systemPermission: '/system/permission',
  systemConfig: '/system/config',
  systemConfigSecurity: '/system/config/security',
  systemConfigAgreement: '/system/config/agreement',
  systemConfigEsIndex: '/system/config/es-index',
  systemLog: '/system-log',
  systemLogOperation: '/system-log/operation-log',
  systemLogLogin: '/system-log/login-log',
} as const;

/** 开启二级菜单独立路由的一级菜单基路径。 */
export const SECONDARY_MENU_ROUTE_ENABLED_BASE_PATHS = [
  routePaths.mallCoupon,
  routePaths.systemConfig,
  routePaths.llmSystemModels,
  routePaths.llmAgentObservability,
] as const;

/** 优惠券二级菜单路由映射。 */
const MALL_COUPON_SECONDARY_ROUTE_PATH_MAP: Record<MallCouponSecondaryRouteKey, string> = {
  template: routePaths.mallCouponTemplate,
  activationCode: routePaths.mallCouponActivationManage,
  log: routePaths.mallCouponLog,
  activationLog: routePaths.mallCouponActivationLog,
};

/** 系统配置二级菜单路由映射。 */
const SYSTEM_CONFIG_SECONDARY_ROUTE_PATH_MAP: Record<SystemConfigSecondaryRouteKey, string> = {
  security: routePaths.systemConfigSecurity,
  agreement: routePaths.systemConfigAgreement,
  esIndex: routePaths.systemConfigEsIndex,
};

/** 系统模型配置二级菜单路由映射。 */
const LLM_SYSTEM_MODELS_SECONDARY_ROUTE_PATH_MAP: Record<LlmSystemModelsSecondaryRouteKey, string> =
  {
    adminConfig: routePaths.llmSystemModelsAdminConfig,
    clientConfig: routePaths.llmSystemModelsClientConfig,
    commonCapability: routePaths.llmSystemModelsCommonCapability,
  };

/** 智能体观测二级菜单路由映射。 */
const LLM_AGENT_OBSERVABILITY_SECONDARY_ROUTE_PATH_MAP: Record<
  LlmAgentObservabilitySecondaryRouteKey,
  string
> = {
  monitor: routePaths.llmAgentMonitor,
  trace: routePaths.llmAgentTrace,
};

function buildPathWithQuery(pathname: string, query: Record<string, RouteQueryValue>): string {
  const searchParams = new URLSearchParams();

  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      searchParams.set(key, String(value));
    }
  });

  const search = searchParams.toString();
  return search ? `${pathname}?${search}` : pathname;
}

export function buildSmartAssistantConversationPath(conversationId: string | number): string {
  return `${routePaths.smartAssistant}/${conversationId}`;
}

export function buildMallProductEditPath(id: string | number): string {
  return `${routePaths.mallProductEdit}/${id}`;
}

/**
 * 构建标签类型下的标签列表页面路径。
 * @param params 页面参数。
 * @returns 页面路径。
 */
export function buildMallProductTagByTypePath(params: {
  typeId: string | number;
  typeName?: string;
}): string {
  return buildPathWithQuery(`${routePaths.mallProductTagByType}/${params.typeId}`, {
    typeName: params.typeName,
  });
}

export function buildSystemRolePermissionPath(id: string | number): string {
  return `${routePaths.systemRolePermission}/${id}`;
}

/**
 * 构建用户资产明细页面路径。
 * @param id 用户ID。
 * @returns 用户资产明细页面路径。
 */
export function buildSystemUserAssetPath(id: string | number): string {
  return `${routePaths.systemUserAssets}/${id}`;
}

/**
 * 构建优惠券发券页面路径。
 * @param params 页面参数。
 * @returns 发券页面路径。
 */
export function buildMallCouponIssuePath(params?: { templateId?: string | number }): string {
  return buildPathWithQuery(routePaths.mallCouponIssue, {
    templateId: params?.templateId,
  });
}

/**
 * 判断指定一级菜单是否开启二级菜单独立路由。
 * @param basePath 一级菜单基路径。
 * @returns 是否开启。
 */
export function isSecondaryMenuRouteEnabled(basePath: string): boolean {
  return SECONDARY_MENU_ROUTE_ENABLED_BASE_PATHS.includes(
    basePath as (typeof SECONDARY_MENU_ROUTE_ENABLED_BASE_PATHS)[number],
  );
}

/**
 * 构建优惠券二级菜单路由路径。
 * @param key 二级菜单 key。
 * @returns 二级菜单页面路径。
 */
export function buildMallCouponSecondaryRoutePath(key: MallCouponSecondaryRouteKey): string {
  return MALL_COUPON_SECONDARY_ROUTE_PATH_MAP[key];
}

/**
 * 构建系统配置二级菜单路由路径。
 * @param key 二级菜单 key。
 * @returns 二级菜单页面路径。
 */
export function buildSystemConfigSecondaryRoutePath(key: SystemConfigSecondaryRouteKey): string {
  return SYSTEM_CONFIG_SECONDARY_ROUTE_PATH_MAP[key];
}

/**
 * 构建系统模型配置二级菜单路由路径。
 * @param key 二级菜单 key。
 * @returns 二级菜单页面路径。
 */
export function buildLlmSystemModelsSecondaryRoutePath(
  key: LlmSystemModelsSecondaryRouteKey,
): string {
  return LLM_SYSTEM_MODELS_SECONDARY_ROUTE_PATH_MAP[key];
}

/**
 * 构建智能体观测二级菜单路由路径。
 * @param key 二级菜单 key。
 * @returns 二级菜单页面路径。
 */
export function buildLlmAgentObservabilitySecondaryRoutePath(
  key: LlmAgentObservabilitySecondaryRouteKey,
): string {
  return LLM_AGENT_OBSERVABILITY_SECONDARY_ROUTE_PATH_MAP[key];
}

/**
 * 构建智能体监控单模型详情页面路径。
 * @param params 页面查询参数。
 * @returns 单模型详情页面路径。
 */
export function buildAgentMonitorModelDetailPath(params: {
  provider?: string;
  modelName?: string;
  startTime?: string;
  endTime?: string;
  rangeMinutes?: number;
  conversationType?: string;
  slot?: string;
  status?: string;
  bucketMinutes?: number;
  backProvider?: string;
  backModelName?: string;
}): string {
  return buildPathWithQuery(routePaths.llmAgentMonitorModelDetail, params);
}

/**
 * 构建激活码批次查看页面路径。
 * @param batchId 批次ID。
 * @returns 激活码列表页面路径。
 */
export function buildMallCouponActivationCodesPath(batchId: string | number): string {
  return `${routePaths.mallCouponActivationCodes}/${batchId}`;
}

export function buildKnowledgeBaseDocumentPath(params: {
  knowledgeBaseId: string | number;
  knowledgeBaseName: string;
}): string {
  return buildPathWithQuery(routePaths.llmKnowledgeBaseDocument, {
    id: params.knowledgeBaseId,
    name: params.knowledgeBaseName,
  });
}

export function buildKnowledgeBaseImportPath(params: {
  knowledgeBaseId: string | number;
  knowledgeBaseName: string;
}): string {
  return buildPathWithQuery(routePaths.llmKnowledgeBaseImport, {
    id: params.knowledgeBaseId,
    name: params.knowledgeBaseName,
  });
}

/**
 * 构建知识库检索页面路径。
 * @param params 页面参数。
 * @returns 页面路径。
 */
export function buildKnowledgeBaseSearchPath(params?: {
  knowledgeBaseId?: string | number;
  knowledgeBaseName?: string;
}): string {
  return buildPathWithQuery(routePaths.llmKnowledgeBaseSearch, {
    id: params?.knowledgeBaseId,
    name: params?.knowledgeBaseName,
  });
}

export function buildKnowledgeBaseChunkPath(params: {
  documentId: string | number;
  documentName: string;
  knowledgeBaseId: string | number;
  knowledgeBaseName: string;
}): string {
  return buildPathWithQuery(routePaths.llmKnowledgeBaseChunk, {
    documentId: params.documentId,
    documentName: params.documentName,
    knowledgeBaseId: params.knowledgeBaseId,
    knowledgeBaseName: params.knowledgeBaseName,
  });
}

export function buildModelProviderCreatePath(params?: {
  source?: string;
  providerKey?: string;
}): string {
  return buildPathWithQuery(routePaths.llmModelProviderCreate, {
    source: params?.source,
    providerKey: params?.providerKey,
  });
}

export function buildModelProviderEditPath(id: string | number): string {
  return `${routePaths.llmModelProviderEdit}/${id}`;
}

/**
 * 构建提示词历史页面路径。
 * @param promptKey 提示词业务键。
 * @returns 提示词历史页面路径。
 */
export function buildPromptManageHistoryPath(promptKey: string): string {
  return buildPathWithQuery(routePaths.llmPromptManageHistory, {
    promptKey,
  });
}

/**
 * 构建提示词编辑页面路径。
 * @param promptKey 提示词业务键。
 * @returns 提示词编辑页面路径。
 */
export function buildPromptManageEditPath(promptKey: string): string {
  return buildPathWithQuery(routePaths.llmPromptManageEdit, {
    promptKey,
  });
}
