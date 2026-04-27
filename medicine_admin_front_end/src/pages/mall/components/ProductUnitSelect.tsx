import { DeleteOutlined } from '@ant-design/icons';
import { Dropdown, Modal, Select, message } from 'antd';
import type { BaseOptionType } from 'antd/es/select';
import React, { useEffect, useState } from 'react';
import {
  addProductUnit,
  deleteProductUnit,
  listProductUnitOptions,
  type MallProductUnitTypes,
} from '@/api/mall/productUnit';

/**
 * 商品单位名称最大长度。
 */
const MAX_UNIT_NAME_LENGTH = 20;

/**
 * 商品单位选择组件属性。
 */
interface ProductUnitSelectProps {
  /** 当前选中的单位值 */
  value?: string;
  /** 单位变更回调 */
  onChange?: (value: string) => void;
  /** 占位文案 */
  placeholder?: string;
  /** 是否禁用 */
  disabled?: boolean;
}

/**
 * 商品单位选择器的选项结构。
 */
interface ProductUnitSelectOption extends BaseOptionType {
  /** Select 内部使用的值 */
  value: string;
  /** 下拉项完整内容 */
  label: React.ReactNode;
  /** 输入框内展示名称 */
  unitName: string;
  /** 单位主键 ID */
  unitId?: string;
}

/**
 * 归一化商品单位名称。
 *
 * @param unitName 原始单位名称
 * @returns 去除首尾空格后的单位名称
 */
function normalizeUnitName(unitName?: string): string {
  return unitName?.trim() ?? '';
}

/**
 * 生成用于名称比较的标准化键值。
 *
 * @param unitName 原始单位名称
 * @returns 小写且去除首尾空格后的名称键值
 */
function buildUnitNameKey(unitName?: string): string {
  return normalizeUnitName(unitName).toLocaleLowerCase();
}

/**
 * 商品单位选择组件。
 *
 * @param props 组件属性
 * @returns 商品单位选择组件节点
 */
const ProductUnitSelect: React.FC<ProductUnitSelectProps> = ({
  value,
  onChange,
  placeholder = '请选择或输入单位',
  disabled = false,
}) => {
  const [messageApi, contextHolder] = message.useMessage();
  const [unitOptions, setUnitOptions] = useState<MallProductUnitTypes.MallProductUnitVo[]>([]);
  const [loadingUnits, setLoadingUnits] = useState(false);
  const [creatingUnit, setCreatingUnit] = useState(false);
  const [searchValue, setSearchValue] = useState('');

  /**
   * 构建下拉选项列表。
   *
   * @returns 当前应展示的下拉选项
   */
  const buildSelectOptions = (): ProductUnitSelectOption[] => {
    const normalizedCurrentValue = normalizeUnitName(value);
    const hasCurrentValueInActiveOptions = unitOptions.some(
      (unitOption) =>
        buildUnitNameKey(unitOption.name) === buildUnitNameKey(normalizedCurrentValue),
    );
    const mergedOptions = [...unitOptions];

    if (normalizedCurrentValue && !hasCurrentValueInActiveOptions) {
      mergedOptions.unshift({
        name: normalizedCurrentValue,
      });
    }

    return mergedOptions.map((unitOption) => {
      const unitName = normalizeUnitName(unitOption.name);
      const unitId = unitOption.id;
      return {
        value: unitName,
        unitName,
        unitId,
        label: unitId ? (
          <Dropdown
            trigger={['contextMenu']}
            menu={{
              items: [
                {
                  key: 'delete',
                  danger: true,
                  label: '删除此项',
                  icon: <DeleteOutlined />,
                  onClick: ({ domEvent }) => {
                    domEvent.stopPropagation();
                    Modal.confirm({
                      title: '确认删除',
                      content: (
                        <div style={{ marginTop: 4 }}>
                          确定要删除单位 <strong>{unitName}</strong> 吗？
                        </div>
                      ),
                      okText: '删除',
                      okButtonProps: { danger: true },
                      cancelText: '取消',
                      onOk: () => handleDeleteUnit(unitId, unitName),
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
              {unitName}
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
            {unitName}
          </div>
        ),
      };
    });
  };

  /**
   * 处理商品单位变更。
   *
   * @param nextValue 下一个单位值
   */
  const handleChange = (nextValue: string) => {
    setSearchValue('');
    onChange?.(nextValue);
  };

  /**
   * 处理商品单位新增。
   */
  const handleCreateUnit = async () => {
    const normalizedSearchValue = normalizeUnitName(searchValue);
    if (!normalizedSearchValue) {
      return;
    }
    if (normalizedSearchValue.length > MAX_UNIT_NAME_LENGTH) {
      messageApi.error(`商品单位不能超过${MAX_UNIT_NAME_LENGTH}个字符`);
      return;
    }

    const existingUnit = unitOptions.find(
      (unitOption) => buildUnitNameKey(unitOption.name) === buildUnitNameKey(normalizedSearchValue),
    );
    if (existingUnit?.name) {
      handleChange(normalizeUnitName(existingUnit.name));
      return;
    }

    try {
      setCreatingUnit(true);
      const createdUnit = await addProductUnit({
        name: normalizedSearchValue,
      });
      if (!createdUnit?.name) {
        throw new Error('created unit name missing');
      }
      setUnitOptions((previousOptions) => [...previousOptions, createdUnit]);
      handleChange(normalizeUnitName(createdUnit.name));
      messageApi.success('新增商品单位成功');
    } catch (error) {
      console.error('handleCreateUnit error:', error);
      messageApi.error('新增商品单位失败');
    } finally {
      setCreatingUnit(false);
    }
  };

  /**
   * 处理商品单位删除。
   *
   * @param unitId 商品单位 ID
   * @param unitName 商品单位名称
   */
  const handleDeleteUnit = async (unitId: string, unitName: string) => {
    try {
      await deleteProductUnit(unitId);
      setUnitOptions((previousOptions) =>
        previousOptions.filter((unitOption) => unitOption.id !== unitId),
      );
      messageApi.success(`已删除单位“${unitName}”`);
    } catch (error) {
      console.error('handleDeleteUnit error:', error);
      messageApi.error('删除商品单位失败');
    }
  };

  /**
   * 处理输入框回车事件。
   *
   * @param event 键盘事件对象
   */
  const handleInputKeyDown = (
    event: React.KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    if (event.key !== 'Enter' || event.nativeEvent.isComposing || creatingUnit) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    void handleCreateUnit();
  };

  useEffect(() => {
    /**
     * 首次挂载时加载商品单位列表。
     */
    const initializeUnitOptions = async () => {
      try {
        setLoadingUnits(true);
        const response = await listProductUnitOptions();
        setUnitOptions(response || []);
      } catch (error) {
        console.error('initializeUnitOptions error:', error);
        messageApi.error('加载商品单位失败');
      } finally {
        setLoadingUnits(false);
      }
    };

    void initializeUnitOptions();
  }, [messageApi]);

  const selectOptions = buildSelectOptions();
  const normalizedSearchValue = normalizeUnitName(searchValue);

  return (
    <>
      {contextHolder}
      <Select<string, ProductUnitSelectOption>
        value={value}
        disabled={disabled}
        loading={loadingUnits}
        showSearch
        placeholder={placeholder}
        searchValue={searchValue}
        optionLabelProp="unitName"
        filterOption={(inputValue, option) =>
          buildUnitNameKey(option?.unitName).includes(buildUnitNameKey(inputValue))
        }
        onSearch={setSearchValue}
        onChange={handleChange}
        onInputKeyDown={handleInputKeyDown}
        notFoundContent={
          normalizedSearchValue ? (
            <span
              style={{ color: '#1677ff', cursor: 'pointer', padding: '4px 0', display: 'block' }}
              onMouseDown={(e) => {
                e.preventDefault();
                void handleCreateUnit();
              }}
            >
              + 点击或回车新增 <strong>{normalizedSearchValue}</strong>
            </span>
          ) : (
            <span style={{ color: 'rgba(0, 0, 0, 0.45)' }}>暂无单位选项</span>
          )
        }
        options={selectOptions}
      />
    </>
  );
};

export default ProductUnitSelect;
