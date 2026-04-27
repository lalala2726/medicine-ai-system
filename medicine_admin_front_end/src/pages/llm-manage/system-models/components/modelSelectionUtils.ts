import type { SystemModelTypes } from '@/api/llm-manage/systemModels';

/**
 * 模型槽位表单值。
 */
export interface SlotValue {
  /** 当前槽位选择的模型名称。 */
  modelName?: string;
  /** 当前槽位是否启用深度思考。 */
  reasoningEnabled: boolean;
}

/**
 * 模型槽位清理结果。
 */
export interface SanitizedChatModelSelection {
  /** 可回填到表单的槽位值。 */
  value: SlotValue;
  /** 当前选择不在可选项中时展示的提示。 */
  invalidSelectionHint?: string;
}

/**
 * 创建模型槽位表单值。
 *
 * @param slot 后端返回的模型槽位配置。
 * @returns 可回填到表单的模型槽位值。
 */
export function createSlotValue(slot?: SystemModelTypes.ModelSelection | null): SlotValue {
  return {
    modelName: slot?.modelName,
    reasoningEnabled: slot?.reasoningEnabled ?? false,
  };
}

/**
 * 清理聊天模型选择值。
 *
 * @param slot 当前配置中的聊天模型槽位。
 * @param options 当前主配置下可选的聊天模型选项列表。
 * @returns 校验后的表单值与提示文案。
 */
export function sanitizeChatModelSelection(
  slot: SystemModelTypes.ModelSelection | null | undefined,
  options: SystemModelTypes.ModelOption[],
): SanitizedChatModelSelection {
  const value = createSlotValue(slot);
  if (!slot?.modelName || options.some((item) => item.value === slot.modelName)) {
    return { value };
  }

  return {
    value,
    invalidSelectionHint: `当前主配置下模型 ${slot.modelName} 不在可选聊天模型中，请确认模型提供商后重新选择`,
  };
}

/**
 * 清理重排模型选择值，避免主配置切换后残留无效选项。
 *
 * @param slot 当前配置中的重排模型选择值。
 * @param options 当前主配置下可用的重排模型选项列表。
 * @returns 校验后的表单值与提示文案。
 */
export function sanitizeRerankModelSelection(
  slot: SystemModelTypes.ModelSelection | null | undefined,
  options: SystemModelTypes.ModelOption[],
): SanitizedChatModelSelection {
  const value = createSlotValue(slot);
  if (!slot?.modelName || options.some((item) => item.value === slot.modelName)) {
    return { value };
  }

  return {
    value,
    invalidSelectionHint: `当前主配置下模型 ${slot.modelName} 不在可选重排模型中，请确认模型提供商后重新选择`,
  };
}
