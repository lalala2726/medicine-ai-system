// @ts-ignore

declare namespace API {
  // 基础响应类型
  type BaseResponse<T = any> = {
    code: number;
    message: string;
    timestamp: number;
    data: T;
  };

  // 错误码定义
  type ErrorCode =
    | 200 // 成功
    | 400 // 请求错误
    | 401 // 未授权
    | 4011 // 访问令牌已过期
    | 4012 // 刷新令牌已过期
    | 403 // 禁止访问
    | 404 // 未找到
    | 500 // 服务器错误
    | number; // 其他错误码

  // 登录参数
  type LoginParams = {
    username: string;
    password: string;
    captchaVerificationId?: string | null;
    autoLogin?: boolean;
    type?: string;
  };

  // 登录结果
  type LoginResult = {
    accessToken: string;
    refreshToken: string;
    user?: CurrentUser;
  };

  // 当前用户信息
  type CurrentUser = {
    name?: string;
    avatar?: string;
    userid?: string;
    email?: string;
    signature?: string;
    title?: string;
    group?: string;
    tags?: { key?: string; label?: string }[];
    notifyCount?: number;
    unreadCount?: number;
    country?: string;
    access?: string;
    geographic?: {
      province?: { label?: string; key?: string };
      city?: { label?: string; key?: string };
    };
    address?: string;
    phone?: string;
    // 新增字段
    username?: string;
    realName?: string;
    roles?: string[];
    permissions?: string[];
    // 后端返回的字段
    id?: number;
    nickname?: string;
    phoneNumber?: string;
  };

  // 用户信息响应
  type CurrentUserResult = BaseResponse<CurrentUser>;

  // 分页参数
  type PageParams = {
    current?: number;
    pageSize?: number;
  };

  // 规则列表项
  type RuleListItem = {
    key?: number;
    disabled?: boolean;
    href?: string;
    avatar?: string;
    name?: string;
    owner?: string;
    desc?: string;
    callNo?: number;
    status?: number;
    updatedAt?: string;
    createdAt?: string;
    progress?: number;
  };

  // 规则列表
  type RuleList = {
    data?: RuleListItem[];
    /** 列表的内容总数 */
    total?: number;
    success?: boolean;
  };

  // 验证码
  type FakeCaptcha = {
    code?: number;
    status?: string;
  };

  // 错误响应
  type ErrorResponse = {
    /** 业务约定的错误码 */
    errorCode: string;
    /** 业务上的错误信息 */
    errorMessage?: string;
    /** 业务上的请求是否成功 */
    success?: boolean;
  };

  // 通知图标列表
  type NoticeIconList = {
    data?: NoticeIconItem[];
    /** 列表的内容总数 */
    total?: number;
    success?: boolean;
  };

  // 通知图标项类型
  type NoticeIconItemType = 'notification' | 'message' | 'event';

  // 通知图标项
  type NoticeIconItem = {
    id?: string;
    extra?: string;
    key?: string;
    read?: boolean;
    avatar?: string;
    title?: string;
    status?: string;
    datetime?: string;
    description?: string;
    type?: NoticeIconItemType;
  };
}
