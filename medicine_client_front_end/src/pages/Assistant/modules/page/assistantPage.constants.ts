import { CHAT_MESSAGE_TYPES } from '../messages/chatTypes'

/** Assistant 页面顶部标题。 */
export const ASSISTANT_PAGE_TITLE = '智能助手'

/** Assistant 输入框默认占位文案。 */
export const ASSISTANT_INPUT_PLACEHOLDER = '发消息或按住说话...'

/** Assistant 页面历史记录加载中的状态文案。 */
export const ASSISTANT_HISTORY_LOADING_TEXT = '正在加载历史消息...'

/** 历史记录加载失败时的兜底文案。 */
export const ASSISTANT_HISTORY_LOAD_ERROR_TEXT = '加载聊天记录失败，请稍后重试'

/** 流式回复未结束时，阻止发送新问题的提示文案。 */
export const ASSISTANT_REPLY_PENDING_WARNING = '请等待当前回复完成'

/** submit 命中运行中任务冲突后恢复 attach 的提示文案。 */
export const ASSISTANT_REPLY_RESUME_WARNING = '当前会话已有回答进行中，已为你恢复连接'

/** 历史记录加载过程中，阻止其他交互的提示文案。 */
export const ASSISTANT_HISTORY_LOADING_WARNING = '历史消息加载中，请稍候'

/** 成功开启新会话后的提示文案。 */
export const ASSISTANT_NEW_SESSION_SUCCESS_TEXT = '已开启新会话'

/** STT 未识别到有效文本时的提示文案。 */
export const ASSISTANT_STT_EMPTY_TEXT = '未识别到有效语音，请重试'

/** STT 识别过程中阻止重复开始的提示文案。 */
export const ASSISTANT_STT_PENDING_TEXT = '语音识别进行中，请稍候'

/** 图片上传按钮当前仅展示时的提示文案。 */
export const ASSISTANT_IMAGE_UPLOAD_PENDING_TEXT = '图片上传功能开发中'

/** 历史消息无法适配为可渲染消息时的占位文案。 */
export const ASSISTANT_UNSUPPORTED_MESSAGE_TEXT = '暂不支持展示这条消息'

/** 工具栏图片按钮插入的演示图片地址。 */
export const ASSISTANT_TOOLBAR_IMAGE_URL = 'https://gw.alicdn.com/tfs/TB1p_nirYr1gK0jSZR0XXbP8XXa-300-300.png'

/** 页面切换或发送前需要清理的临时消息类型集合。 */
export const ASSISTANT_TRANSIENT_MESSAGE_TYPES = [CHAT_MESSAGE_TYPES.TYPING]
