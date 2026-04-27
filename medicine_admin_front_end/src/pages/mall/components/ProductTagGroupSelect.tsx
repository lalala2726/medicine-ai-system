import { DeleteOutlined } from '@ant-design/icons';
import { Dropdown, Empty, Modal, Select, Typography, message, theme } from 'antd';
import type { DefaultOptionType } from 'antd/es/select';
import React, { useEffect, useMemo, useState } from 'react';
import {
  addProductTag,
  listProductTagOptions,
  updateProductTagStatus,
} from '@/api/mall/productTag';
import type { ProductTagLike, ProductTagSelectGroup } from './productTagUtils';

const { Text } = Typography;

/**
 * 标签名称最大长度。
 */
const MAX_TAG_NAME_LENGTH = 64;

/**
 * 商品标签选择器属性。
 */
export interface ProductTagGroupSelectProps {
  /** 当前选中的标签 ID 列表 */
  value?: string[];
  /** 标签变化回调 */
  onChange?: (value: string[]) => void;
  /** 标签分组数据 */
  groups: ProductTagSelectGroup[];
  /** 当前商品已绑定的标签详情 */
  selectedTags?: ProductTagLike[];
  /** 标签分组刷新回调 */
  onGroupsRefresh?: () => Promise<void> | void;
  /** 标签名称映射变化回调 */
  onOptionLabelMapChange?: (optionLabelMap: Record<string, string>) => void;
  /** 是否禁用 */
  disabled?: boolean;
  /** 空数据提示 */
  emptyText?: string;
}

/**
 * 可编辑标签选项结构。
 */
interface EditableProductTagOption {
  /** 标签ID */
  value: string;
  /** 标签名称 */
  label: string;
}

/**
 * 可编辑标签分组结构。
 */
interface EditableProductTagGroup {
  /** 标签类型ID */
  typeId: string;
  /** 标签类型编码 */
  typeCode?: string;
  /** 标签类型名称 */
  typeName: string;
  /** 当前类型下的标签选项 */
  options: EditableProductTagOption[];
}

/**
 * 归一化标签名称。
 *
 * @param tagName 原始标签名称
 * @returns 去除首尾空格后的标签名称
 */
function normalizeTagName(tagName?: string): string {
  return tagName?.trim() ?? '';
}

/**
 * 生成用于名称比较的标准化键值。
 *
 * @param tagName 原始标签名称
 * @returns 小写且去除首尾空格后的名称键值
 */
function buildTagNameKey(tagName?: string): string {
  return normalizeTagName(tagName).toLocaleLowerCase();
}

/**
 * 合并启用分组与当前已选标签，确保禁用但已绑定的标签仍能回显。
 *
 * @param groups 启用标签分组
 * @param selectedTags 当前商品已绑定标签详情
 * @returns 合并后的标签分组
 */
function mergeGroupsWithSelectedTags(
  groups: ProductTagSelectGroup[],
  selectedTags: ProductTagLike[],
): EditableProductTagGroup[] {
  const groupMap = new Map<string, EditableProductTagGroup>();
  const groupOrder: string[] = [];

  groups.forEach((group) => {
    groupMap.set(group.typeId, {
      typeId: group.typeId,
      typeCode: group.typeCode,
      typeName: group.typeName,
      options: group.options.map((option) => ({
        value: String(option.value),
        label: option.label,
      })),
    });
    groupOrder.push(group.typeId);
  });

  selectedTags.forEach((selectedTag) => {
    const typeId = selectedTag.typeId ? String(selectedTag.typeId) : '';
    const tagId = selectedTag.id ? String(selectedTag.id) : '';
    const tagName = normalizeTagName(selectedTag.name);
    if (!typeId || !tagId || !tagName) {
      return;
    }
    if (!groupMap.has(typeId)) {
      groupMap.set(typeId, {
        typeId,
        typeCode: selectedTag.typeCode,
        typeName: selectedTag.typeName || selectedTag.typeCode || `类型${typeId}`,
        options: [],
      });
      groupOrder.push(typeId);
    }
    const currentGroup = groupMap.get(typeId)!;
    const exists = currentGroup.options.some((option) => option.value === tagId);
    if (!exists) {
      currentGroup.options.unshift({
        value: tagId,
        label: tagName,
      });
    }
  });

  return groupOrder.map((typeId) => groupMap.get(typeId)!);
}

/**
 * 商品标签按类型分组可编辑组件。
 * 支持按类型搜索、回车新增和禁用移除。
 *
 * @param props 组件属性
 * @returns 商品标签分组编辑组件
 */
const ProductTagGroupSelect: React.FC<ProductTagGroupSelectProps> = ({
  value = [],
  onChange,
  groups,
  selectedTags = [],
  onGroupsRefresh,
  onOptionLabelMapChange,
  disabled = false,
  emptyText = '暂无可选标签',
}) => {
  const { token } = theme.useToken();
  const [messageApi, contextHolder] = message.useMessage();
  const [editableGroups, setEditableGroups] = useState<EditableProductTagGroup[]>([]);
  const [searchValueMap, setSearchValueMap] = useState<Record<string, string>>({});
  const [creatingTypeId, setCreatingTypeId] = useState<string>();

  /**
   * 按标签类型替换单个分组的选项。
   *
   * @param typeId 标签类型ID
   * @param options 新的选项列表
   */
  const replaceGroupOptions = (typeId: string, options: EditableProductTagOption[]) => {
    setEditableGroups((previousGroups) =>
      previousGroups.map((group) =>
        group.typeId === typeId
          ? {
              ...group,
              options,
            }
          : group,
      ),
    );
  };

  /**
   * 处理单个类型下的标签变更。
   *
   * @param group 当前标签分组
   * @param nextTypeTagIds 当前类型下的已选标签ID列表
   */
  const handleTypeChange = (group: EditableProductTagGroup, nextTypeTagIds: string[]) => {
    const currentValue = (value || []).map(String);
    const currentTypeTagIdSet = new Set(group.options.map((option) => String(option.value)));
    const preservedIds = currentValue.filter((tagId) => !currentTypeTagIdSet.has(tagId));
    onChange?.([...preservedIds, ...nextTypeTagIds]);
  };

  /**
   * 刷新指定标签类型下的选项列表。
   *
   * @param group 当前标签分组
   * @returns 刷新后的标签选项列表
   */
  const refreshGroupOptions = async (
    group: EditableProductTagGroup,
  ): Promise<EditableProductTagOption[]> => {
    const latestOptions = await listProductTagOptions(group.typeCode);
    const normalizedLatestOptions = (latestOptions || []).map((option) => ({
      value: String(option.id),
      label: normalizeTagName(option.name),
    }));
    const currentSelectedTags = selectedTags.filter(
      (selectedTag) => String(selectedTag.typeId || '') === group.typeId,
    );
    const mergedGroup = mergeGroupsWithSelectedTags(
      [
        {
          typeId: group.typeId,
          typeCode: group.typeCode,
          typeName: group.typeName,
          options: normalizedLatestOptions,
        },
      ],
      currentSelectedTags,
    )[0];
    replaceGroupOptions(group.typeId, mergedGroup.options);
    return mergedGroup.options;
  };

  /**
   * 处理标签新增。
   *
   * @param group 当前标签分组
   */
  const handleCreateTag = async (group: EditableProductTagGroup) => {
    const currentSearchValue = normalizeTagName(searchValueMap[group.typeId]);
    if (!currentSearchValue) {
      return;
    }
    if (currentSearchValue.length > MAX_TAG_NAME_LENGTH) {
      messageApi.error(`标签名称不能超过${MAX_TAG_NAME_LENGTH}个字符`);
      return;
    }

    const existingOption = group.options.find(
      (option) => buildTagNameKey(option.label) === buildTagNameKey(currentSearchValue),
    );
    if (existingOption) {
      setSearchValueMap((previousMap) => ({
        ...previousMap,
        [group.typeId]: '',
      }));
      handleTypeChange(
        group,
        Array.from(new Set([...(value || []).map(String), existingOption.value])),
      );
      return;
    }

    try {
      setCreatingTypeId(group.typeId);
      await addProductTag({
        name: currentSearchValue,
        typeId: group.typeId,
        sort: 0,
        status: 1,
      });
      const refreshedOptions = await refreshGroupOptions(group);
      await onGroupsRefresh?.();
      const createdOption = refreshedOptions.find(
        (option) => buildTagNameKey(option.label) === buildTagNameKey(currentSearchValue),
      );
      if (createdOption) {
        handleTypeChange(
          group,
          Array.from(new Set([...(value || []).map(String), createdOption.value])),
        );
      }
      setSearchValueMap((previousMap) => ({
        ...previousMap,
        [group.typeId]: '',
      }));
      messageApi.success(`已新增${group.typeName}标签`);
    } catch (error) {
      console.error('handleCreateTag error:', error);
      messageApi.error(`新增${group.typeName}标签失败`);
    } finally {
      setCreatingTypeId(undefined);
    }
  };

  /**
   * 处理标签移除。
   *
   * @param group 当前标签分组
   * @param tagId 标签ID
   * @param tagName 标签名称
   */
  const handleDisableTag = async (
    group: EditableProductTagGroup,
    tagId: string,
    tagName: string,
  ) => {
    try {
      const currentValue = (value || []).map(String);
      onChange?.(currentValue.filter((currentTagId) => currentTagId !== tagId));
      await updateProductTagStatus({
        id: tagId,
        status: 0,
      });
      replaceGroupOptions(
        group.typeId,
        group.options.filter((option) => option.value !== tagId),
      );
      await onGroupsRefresh?.();
      messageApi.success(`已移除标签“${tagName}”`);
    } catch (error) {
      console.error('handleDisableTag error:', error);
      messageApi.error(`移除标签“${tagName}”失败`);
    }
  };

  /**
   * 处理输入框回车事件。
   *
   * @param group 当前标签分组
   * @param event 键盘事件对象
   */
  const handleInputKeyDown = (
    group: EditableProductTagGroup,
    event: React.KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    if (event.key !== 'Enter' || event.nativeEvent.isComposing || creatingTypeId === group.typeId) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    void handleCreateTag(group);
  };

  useEffect(() => {
    setEditableGroups(mergeGroupsWithSelectedTags(groups, selectedTags));
  }, [groups, selectedTags]);

  useEffect(() => {
    if (!onOptionLabelMapChange) {
      return;
    }
    const optionLabelMap: Record<string, string> = {};
    editableGroups.forEach((group) => {
      group.options.forEach((option) => {
        optionLabelMap[option.value] = option.label;
      });
    });
    onOptionLabelMapChange(optionLabelMap);
  }, [editableGroups, onOptionLabelMapChange]);

  const selectedIds = useMemo(() => new Set((value || []).map(String)), [value]);

  if (!editableGroups.length) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={emptyText} />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {contextHolder}
      {editableGroups.map((group) => {
        const currentTypeValue = group.options
          .map((option) => String(option.value))
          .filter((optionValue) => selectedIds.has(optionValue));
        const currentSearchValue = searchValueMap[group.typeId] || '';
        const currentTypeOptions: DefaultOptionType[] = group.options.map((option) => ({
          value: option.value,
          label: option.label,
          labelText: option.label,
          tagId: option.value,
        }));

        return (
          <div key={group.typeId} style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <div
                style={{
                  width: '4px',
                  height: '14px',
                  backgroundColor: token.colorPrimary,
                  borderRadius: '2px',
                }}
              />
              <Text strong style={{ fontSize: '14px', color: token.colorTextHeading }}>
                {group.typeName}
              </Text>
            </div>
            <Select
              mode="multiple"
              allowClear
              showSearch
              disabled={disabled}
              maxTagCount="responsive"
              options={currentTypeOptions}
              optionFilterProp="labelText"
              placeholder={`请选择或输入${group.typeName}`}
              style={{ width: '100%' }}
              value={currentTypeValue}
              searchValue={currentSearchValue}
              onSearch={(nextSearchValue) =>
                setSearchValueMap((previousMap) => ({
                  ...previousMap,
                  [group.typeId]: nextSearchValue,
                }))
              }
              onChange={(nextValue) => handleTypeChange(group, nextValue)}
              onInputKeyDown={(event) => handleInputKeyDown(group, event)}
              optionRender={(option) => {
                const optionData = option.data as DefaultOptionType & {
                  labelText?: string;
                  tagId?: string;
                };
                const optionLabel = String(optionData.labelText || '');
                return optionData.tagId ? (
                  <Dropdown
                    trigger={['contextMenu']}
                    menu={{
                      items: [
                        {
                          key: 'delete',
                          danger: true,
                          label: '移除此标签',
                          icon: <DeleteOutlined />,
                          onClick: ({ domEvent }) => {
                            domEvent.stopPropagation();
                            Modal.confirm({
                              title: '确认移除',
                              content: (
                                <div style={{ marginTop: 4 }}>
                                  确定要移除标签 <strong>{optionLabel}</strong> 吗？
                                </div>
                              ),
                              okText: '移除',
                              okButtonProps: { danger: true },
                              cancelText: '取消',
                              onOk: () =>
                                handleDisableTag(group, String(optionData.tagId), optionLabel),
                            });
                          },
                        },
                      ],
                    }}
                  >
                    <div
                      style={{
                        width: '100%',
                        padding: '2px 0',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {optionLabel}
                    </div>
                  </Dropdown>
                ) : (
                  <div
                    style={{
                      width: '100%',
                      padding: '2px 0',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {optionLabel}
                  </div>
                );
              }}
              notFoundContent={
                currentSearchValue ? (
                  <span
                    style={{
                      color: token.colorPrimary,
                      cursor: 'pointer',
                      padding: '4px 0',
                      display: 'block',
                    }}
                    onMouseDown={(e) => {
                      e.preventDefault();
                      void handleCreateTag(group);
                    }}
                  >
                    + 点击或回车新增 <strong>{normalizeTagName(currentSearchValue)}</strong>
                  </span>
                ) : (
                  <span style={{ color: token.colorTextTertiary }}>暂无{group.typeName}标签</span>
                )
              }
              listHeight={250}
              popupMatchSelectWidth={true}
            />
          </div>
        );
      })}
    </div>
  );
};

ProductTagGroupSelect.displayName = 'ProductTagGroupSelect';

export default ProductTagGroupSelect;
