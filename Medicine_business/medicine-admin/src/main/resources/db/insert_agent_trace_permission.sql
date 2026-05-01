-- 智能体观测权限种子数据。
-- 使用方式：在管理端数据库执行本脚本，将智能体观测以树形关系挂到“大模型管理”权限节点下。

INSERT INTO permission
(parent_id, permission_code, permission_name, sort_order, status, remark, create_time, update_time, create_by, update_by)
SELECT p.id,
       'system:agent_trace',
       '智能体观测',
       70,
       0,
       '智能体观测菜单权限',
       NOW(),
       NOW(),
       'system',
       'system'
FROM permission p
WHERE p.permission_code = 'system:llm'
  AND NOT EXISTS (SELECT 1 FROM permission x WHERE x.permission_code = 'system:agent_trace');

UPDATE permission child
    JOIN permission parent ON parent.permission_code = 'system:llm'
SET child.parent_id = parent.id,
    child.permission_name = '智能体观测',
    child.sort_order = 70,
    child.status = 0,
    child.remark = '智能体观测菜单权限',
    child.update_time = NOW(),
    child.update_by = 'system'
WHERE child.permission_code = 'system:agent_trace';

INSERT INTO permission
(parent_id, permission_code, permission_name, sort_order, status, remark, create_time, update_time, create_by, update_by)
SELECT p.id,
       'system:agent_trace:monitor',
       '监控面板',
       10,
       0,
       '智能体观测监控面板权限',
       NOW(),
       NOW(),
       'system',
       'system'
FROM permission p
WHERE p.permission_code = 'system:agent_trace'
  AND NOT EXISTS (SELECT 1 FROM permission x WHERE x.permission_code = 'system:agent_trace:monitor');

UPDATE permission child
    JOIN permission parent ON parent.permission_code = 'system:agent_trace'
SET child.parent_id = parent.id,
    child.permission_name = '监控面板',
    child.sort_order = 10,
    child.status = 0,
    child.remark = '智能体观测监控面板权限',
    child.update_time = NOW(),
    child.update_by = 'system'
WHERE child.permission_code = 'system:agent_trace:monitor';

INSERT INTO permission
(parent_id, permission_code, permission_name, sort_order, status, remark, create_time, update_time, create_by, update_by)
SELECT p.id,
       'system:agent_trace:list',
       '智能体跟踪',
       20,
       0,
       '智能体跟踪列表权限',
       NOW(),
       NOW(),
       'system',
       'system'
FROM permission p
WHERE p.permission_code = 'system:agent_trace'
  AND NOT EXISTS (SELECT 1 FROM permission x WHERE x.permission_code = 'system:agent_trace:list');

UPDATE permission child
    JOIN permission parent ON parent.permission_code = 'system:agent_trace'
SET child.parent_id = parent.id,
    child.permission_name = '智能体跟踪',
    child.sort_order = 20,
    child.status = 0,
    child.remark = '智能体跟踪列表权限',
    child.update_time = NOW(),
    child.update_by = 'system'
WHERE child.permission_code = 'system:agent_trace:list';

INSERT INTO permission
(parent_id, permission_code, permission_name, sort_order, status, remark, create_time, update_time, create_by, update_by)
SELECT p.id,
       'system:agent_trace:query',
       '查看跟踪详情',
       10,
       0,
       '智能体跟踪详情权限',
       NOW(),
       NOW(),
       'system',
       'system'
FROM permission p
WHERE p.permission_code = 'system:agent_trace:list'
  AND NOT EXISTS (SELECT 1 FROM permission x WHERE x.permission_code = 'system:agent_trace:query');

UPDATE permission child
    JOIN permission parent ON parent.permission_code = 'system:agent_trace:list'
SET child.parent_id = parent.id,
    child.permission_name = '查看跟踪详情',
    child.sort_order = 10,
    child.status = 0,
    child.remark = '智能体跟踪详情权限',
    child.update_time = NOW(),
    child.update_by = 'system'
WHERE child.permission_code = 'system:agent_trace:query';

INSERT INTO permission
(parent_id, permission_code, permission_name, sort_order, status, remark, create_time, update_time, create_by, update_by)
SELECT p.id,
       'system:agent_trace:delete',
       '删除跟踪记录',
       20,
       0,
       '智能体跟踪删除权限',
       NOW(),
       NOW(),
       'system',
       'system'
FROM permission p
WHERE p.permission_code = 'system:agent_trace:list'
  AND NOT EXISTS (SELECT 1 FROM permission x WHERE x.permission_code = 'system:agent_trace:delete');

UPDATE permission child
    JOIN permission parent ON parent.permission_code = 'system:agent_trace:list'
SET child.parent_id = parent.id,
    child.permission_name = '删除跟踪记录',
    child.sort_order = 20,
    child.status = 0,
    child.remark = '智能体跟踪删除权限',
    child.update_time = NOW(),
    child.update_by = 'system'
WHERE child.permission_code = 'system:agent_trace:delete';
