import { PageContainer } from '@ant-design/pro-components';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Card, Checkbox, Empty, message, Spin } from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { getRole, getRolePermission, updateRolePermission } from '@/api/system/role';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { routePaths } from '@/router/paths';

import type { Option } from '@/types';
import styles from './permission.module.less';

/** 最大支持层级 */
const MAX_DEPTH = 5;

const RolePermissionPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [permissionData, setPermissionData] = useState<Option<string | number>[]>([]);
  const [checkedKeys, setCheckedKeys] = useState<Set<string | number>>(new Set());
  const [loading, setLoading] = useState<boolean>(true);
  const [saving, setSaving] = useState<boolean>(false);
  const [roleName, setRoleName] = useState<string>('');

  useEffect(() => {
    if (id) {
      fetchData();
    }
  }, [id]);

  const fetchData = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const roleData = await getRole(id);
      setRoleName(roleData.roleName || '');

      const permissionData = await getRolePermission(id);
      setPermissionData(permissionData.permissionOption || []);
      setCheckedKeys(new Set(permissionData.rolePermission || []));
    } catch (error) {
      console.error(error);
      message.error('获取权限失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  /** 递归收集节点自身及所有后代的 value */
  const collectAllValues = useCallback((node: Option<string | number>): (string | number)[] => {
    const values: (string | number)[] = [];
    if (node.value !== undefined) values.push(node.value);
    if (node.children) {
      for (const child of node.children) {
        values.push(...collectAllValues(child));
      }
    }
    return values;
  }, []);

  /** 递归收集所有叶子节点的 value */
  const collectLeafValues = useCallback((node: Option<string | number>): (string | number)[] => {
    if (!node.children || node.children.length === 0) {
      return node.value !== undefined ? [node.value] : [];
    }
    const values: (string | number)[] = [];
    for (const child of node.children) {
      values.push(...collectLeafValues(child));
    }
    return values;
  }, []);

  const isChecked = (value: string | number) => checkedKeys.has(value);

  /** 切换单个叶子节点，同时向上维护祖先的选中状态 */
  const togglePermission = useCallback(
    (value: string | number) => {
      const newChecked = new Set(checkedKeys);
      if (newChecked.has(value)) {
        newChecked.delete(value);
      } else {
        newChecked.add(value);
      }
      // 向上同步父节点状态
      syncAncestors(permissionData, newChecked);
      setCheckedKeys(newChecked);
    },
    [checkedKeys, permissionData],
  );

  /** 切换某个节点及其所有后代 */
  const toggleNode = useCallback(
    (node: Option<string | number>) => {
      if (node.value === undefined) return;
      const newChecked = new Set(checkedKeys);
      const allValues = collectAllValues(node);
      const shouldCheck = !newChecked.has(node.value);

      for (const v of allValues) {
        if (shouldCheck) {
          newChecked.add(v);
        } else {
          newChecked.delete(v);
        }
      }
      // 向上同步父节点状态
      syncAncestors(permissionData, newChecked);
      setCheckedKeys(newChecked);
    },
    [checkedKeys, permissionData, collectAllValues],
  );

  /**
   * 根据子节点选中情况，自底向上同步祖先节点的选中/取消状态。
   * 当某节点的所有后代叶子都被选中时，该节点也被选中；否则取消选中。
   */
  const syncAncestors = (nodes: Option<string | number>[], checked: Set<string | number>) => {
    const sync = (node: Option<string | number>): boolean => {
      if (!node.children || node.children.length === 0) {
        return node.value !== undefined && checked.has(node.value);
      }
      const allChildrenChecked = node.children.every((child) => sync(child));
      if (node.value !== undefined) {
        if (allChildrenChecked) {
          checked.add(node.value);
        } else {
          checked.delete(node.value);
        }
      }
      return allChildrenChecked;
    };
    for (const node of nodes) {
      sync(node);
    }
  };

  /** 递归判断节点是否处于半选状态 */
  const isIndeterminate = useCallback(
    (node: Option<string | number>): boolean => {
      const leaves = collectLeafValues(node);
      if (leaves.length === 0) return false;
      const checkedCount = leaves.filter((v) => checkedKeys.has(v)).length;
      return checkedCount > 0 && checkedCount < leaves.length;
    },
    [checkedKeys, collectLeafValues],
  );

  const handleSave = async () => {
    if (!id) return;
    setSaving(true);
    try {
      await updateRolePermission({
        roleId: id,
        permissionIds: Array.from(checkedKeys).map(String),
      });

      message.success('权限分配成功');
      navigate(routePaths.systemRole);
    } catch (error) {
      console.error(error);
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    navigate(routePaths.systemRole);
  };

  /** 递归渲染权限节点，depth 从 1 开始，最多渲染 MAX_DEPTH 级 */
  const renderPermissionNode = (node: Option<string | number>, depth: number) => {
    if (node.value === undefined || depth > MAX_DEPTH) return null;
    const hasChildren = node.children && node.children.length > 0;
    const isLeaf = !hasChildren;

    // 第 1 级：使用行布局（parentItem 左侧 + 右侧子节点）
    if (depth === 1) {
      return (
        <div key={node.value} className={styles.permissionRow}>
          <div className={styles.parentItem}>
            <Checkbox
              checked={isChecked(node.value)}
              indeterminate={isIndeterminate(node)}
              onChange={() => (isLeaf ? togglePermission(node.value!) : toggleNode(node))}
            >
              {node.label}
            </Checkbox>
          </div>
          {hasChildren && (
            <div className={styles.childrenContainer}>
              {node.children!.map((child) => renderPermissionNode(child, depth + 1))}
            </div>
          )}
        </div>
      );
    }

    // 第 2 级及以下：如果是叶子节点，直接显示为 inline checkbox
    if (isLeaf) {
      return (
        <span key={node.value} className={styles.leafItem}>
          <Checkbox checked={isChecked(node.value)} onChange={() => togglePermission(node.value!)}>
            {node.label}
          </Checkbox>
        </span>
      );
    }

    // 第 2 级及以下：有子节点，显示为嵌套分组
    return (
      <div key={node.value} className={styles.nestedGroup}>
        <div className={styles.nestedParent}>
          <Checkbox
            checked={isChecked(node.value)}
            indeterminate={isIndeterminate(node)}
            onChange={() => toggleNode(node)}
          >
            {node.label}
          </Checkbox>
        </div>
        <div className={styles.nestedChildren}>
          {node.children!.map((child) => renderPermissionNode(child, depth + 1))}
        </div>
      </div>
    );
  };

  return (
    <PageContainer
      title={roleName ? `分配权限 - ${roleName}` : '分配权限'}
      onBack={() => navigate(routePaths.systemRole)}
      breadcrumb={{
        items: [{ title: '系统管理' }, { title: '分配权限' }],
      }}
    >
      <div className={styles.pageContent}>
        <Card>
          <Spin spinning={loading}>
            {permissionData.length > 0 ? (
              <div className={styles.permissionContainer}>
                {permissionData.map((node) => renderPermissionNode(node, 1))}
              </div>
            ) : (
              <Empty description="暂无可分配的权限数据" />
            )}
          </Spin>
        </Card>
      </div>

      {permissionData.length > 0 && (
        <div className={styles.footer}>
          <div className={styles.footerContent}>
            <Button onClick={handleCancel}>取消</Button>
            <PermissionButton
              type="primary"
              loading={saving}
              access={ADMIN_PERMISSIONS.systemRole.update}
              onClick={handleSave}
            >
              保存
            </PermissionButton>
          </div>
        </div>
      )}
    </PageContainer>
  );
};

export default RolePermissionPage;
