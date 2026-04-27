import { PASSWORD_INPUT_AUTOCOMPLETE, TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { Col, Divider, Form, Input, Row, Select, Typography } from 'antd';
import type { FormInstance } from 'antd';
import React from 'react';
import type { UserTypes } from '@/api/system/user';
import type { Option } from '@/types';

const { Title, Text } = Typography;

interface UserInfoTabProps {
  /** 用户详情数据。 */
  userDetail: UserTypes.UserDetailVo | null;
  /** 是否处于编辑模式。 */
  isEditing: boolean;
  /** 用户编辑表单实例。 */
  form: FormInstance;
  /** 后端角色选项列表。 */
  roleOptions: Option<number>[];
  /** 角色选项加载状态。 */
  roleOptionsLoading: boolean;
}

/**
 * 信息展示行属性。
 */
interface InfoRowProps {
  /** 信息标签。 */
  label: string;
  /** 信息内容。 */
  value: React.ReactNode;
}

/**
 * 信息展示行。
 * @param props 展示行属性。
 * @returns 信息展示节点。
 */
const InfoRow: React.FC<InfoRowProps> = ({ label, value }) => (
  <Col span={8}>
    <div
      style={{
        marginBottom: 12,
        fontSize: 14,
        display: 'flex',
        alignItems: 'center',
      }}
    >
      <Text style={{ color: '#666', marginRight: 8 }}>{label}:</Text>
      <Text>{value || '-'}</Text>
    </div>
  </Col>
);

/**
 * 根据角色ID集合解析角色名称。
 * @param roleIds 当前用户角色ID集合。
 * @param roleOptions 后端角色选项列表。
 * @returns 角色名称展示文本。
 */
const formatRoleNames = (roleIds: number[] | undefined, roleOptions: Option<number>[]) => {
  if (!roleIds?.length) {
    return '-';
  }

  const roleLabelMap = new Map(roleOptions.map((role) => [role.value, role.label]));
  const roleNames = roleIds
    .map((roleId) => roleLabelMap.get(roleId))
    .filter((roleName): roleName is string => Boolean(roleName));
  return roleNames.length ? roleNames.join('、') : '-';
};

/**
 * 用户详情基础信息区域。
 * @param props 用户详情基础信息属性。
 * @returns 用户详情基础信息节点。
 */
const UserInfoTab: React.FC<UserInfoTabProps> = ({
  userDetail,
  isEditing,
  form,
  roleOptions,
  roleOptionsLoading,
}) => {
  /**
   * 获取性别展示文本。
   * @param gender 性别枚举值。
   * @returns 性别展示文本。
   */
  const getGenderText = (gender?: number | string) => {
    if (gender === undefined || gender === null) return '-';
    const genderNum = typeof gender === 'string' ? Number(gender) : gender;
    return genderNum === 0 ? '男' : genderNum === 1 ? '女' : '-';
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* 基本信息 */}
      <div>
        <Title level={5} style={{ marginBottom: 16 }}>
          基本信息
        </Title>
        {!isEditing ? (
          // 查看模式 - 文本显示
          <>
            <Row gutter={[16, 16]}>
              <InfoRow label="用户ID" value={userDetail?.basicInfo?.userId} />
              <InfoRow label="真实姓名" value={userDetail?.basicInfo?.realName} />
              <InfoRow label="昵称" value={userDetail?.nickName} />
              <InfoRow label="手机号" value={userDetail?.basicInfo?.phoneNumber} />
              <InfoRow label="邮箱" value={userDetail?.basicInfo?.email} />
              <InfoRow label="性别" value={getGenderText(userDetail?.basicInfo?.gender)} />
              <InfoRow label="角色" value={formatRoleNames(userDetail?.roles, roleOptions)} />
              <InfoRow label="身份证号" value={userDetail?.basicInfo?.idCard} />
            </Row>
          </>
        ) : (
          // 编辑模式 - 表单输入
          <Form form={form}>
            <Row gutter={[16, 8]}>
              <Col span={12}>
                <div
                  style={{
                    marginBottom: 16,
                    fontSize: 14,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Text
                    style={{
                      color: '#666',
                      marginRight: 8,
                      width: 90,
                      textAlign: 'right',
                    }}
                  >
                    用户ID:
                  </Text>
                  <Form.Item name="userId" style={{ margin: 0, flex: 1 }}>
                    <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} disabled />
                  </Form.Item>
                </div>
              </Col>
              <Col span={12}>
                <div
                  style={{
                    marginBottom: 16,
                    fontSize: 14,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Text
                    style={{
                      color: '#666',
                      marginRight: 8,
                      width: 90,
                      textAlign: 'right',
                    }}
                  >
                    角色:
                  </Text>
                  <Form.Item
                    name="roles"
                    style={{ margin: 0, flex: 1 }}
                    rules={[{ required: true, message: '请选择角色' }]}
                  >
                    <Select
                      mode="multiple"
                      options={roleOptions}
                      loading={roleOptionsLoading}
                      placeholder="请选择角色"
                    />
                  </Form.Item>
                </div>
              </Col>
              <Col span={12}>
                <div
                  style={{
                    marginBottom: 16,
                    fontSize: 14,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Text
                    style={{
                      color: '#666',
                      marginRight: 8,
                      width: 90,
                      textAlign: 'right',
                    }}
                  >
                    真实姓名:
                  </Text>
                  <Form.Item
                    name="realName"
                    style={{ margin: 0, flex: 1 }}
                    rules={[{ max: 50, message: '真实姓名长度不能超过50个字符' }]}
                  >
                    <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入真实姓名" />
                  </Form.Item>
                </div>
              </Col>
              <Col span={12}>
                <div
                  style={{
                    marginBottom: 16,
                    fontSize: 14,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Text
                    style={{
                      color: '#666',
                      marginRight: 8,
                      width: 90,
                      textAlign: 'right',
                    }}
                  >
                    昵称:
                  </Text>
                  <Form.Item
                    name="nickName"
                    style={{ margin: 0, flex: 1 }}
                    rules={[
                      { required: true, message: '请输入昵称' },
                      { max: 50, message: '昵称长度不能超过50个字符' },
                    ]}
                  >
                    <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入昵称" />
                  </Form.Item>
                </div>
              </Col>
              <Col span={12}>
                <div
                  style={{
                    marginBottom: 16,
                    fontSize: 14,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Text
                    style={{
                      color: '#666',
                      marginRight: 8,
                      width: 90,
                      textAlign: 'right',
                    }}
                  >
                    手机号:
                  </Text>
                  <Form.Item
                    name="phoneNumber"
                    style={{ margin: 0, flex: 1 }}
                    rules={[
                      {
                        pattern: /^1[3-9]\d{9}$/,
                        message: '请输入正确的手机号',
                      },
                    ]}
                  >
                    <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入手机号" />
                  </Form.Item>
                </div>
              </Col>
              <Col span={12}>
                <div
                  style={{
                    marginBottom: 16,
                    fontSize: 14,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Text
                    style={{
                      color: '#666',
                      marginRight: 8,
                      width: 90,
                      textAlign: 'right',
                    }}
                  >
                    邮箱:
                  </Text>
                  <Form.Item
                    name="email"
                    style={{ margin: 0, flex: 1 }}
                    rules={[{ type: 'email', message: '请输入正确的邮箱地址' }]}
                  >
                    <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入邮箱" />
                  </Form.Item>
                </div>
              </Col>
              <Col span={12}>
                <div
                  style={{
                    marginBottom: 16,
                    fontSize: 14,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Text
                    style={{
                      color: '#666',
                      marginRight: 8,
                      width: 90,
                      textAlign: 'right',
                    }}
                  >
                    性别:
                  </Text>
                  <Form.Item name="gender" style={{ margin: 0, flex: 1 }}>
                    <Select placeholder="请选择性别">
                      <Select.Option value={0}>男</Select.Option>
                      <Select.Option value={1}>女</Select.Option>
                    </Select>
                  </Form.Item>
                </div>
              </Col>
              <Col span={12}>
                <div
                  style={{
                    marginBottom: 16,
                    fontSize: 14,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Text
                    style={{
                      color: '#666',
                      marginRight: 8,
                      width: 90,
                      textAlign: 'right',
                    }}
                  >
                    身份证号:
                  </Text>
                  <Form.Item
                    name="idCard"
                    style={{ margin: 0, flex: 1 }}
                    rules={[
                      {
                        pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/,
                        message: '请输入正确的身份证号',
                      },
                    ]}
                  >
                    <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入身份证号" />
                  </Form.Item>
                </div>
              </Col>
              <Col span={12}>
                <div
                  style={{
                    marginBottom: 16,
                    fontSize: 14,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Text
                    style={{
                      color: '#666',
                      marginRight: 8,
                      width: 90,
                      textAlign: 'right',
                    }}
                  >
                    用户状态:
                  </Text>
                  <Form.Item name="status" style={{ margin: 0, flex: 1 }}>
                    <Select placeholder="请选择状态">
                      <Select.Option value={0}>启用</Select.Option>
                      <Select.Option value={1}>禁用</Select.Option>
                    </Select>
                  </Form.Item>
                </div>
              </Col>
              <Col span={12}>
                <div
                  style={{
                    marginBottom: 16,
                    fontSize: 14,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Text
                    style={{
                      color: '#666',
                      marginRight: 8,
                      width: 90,
                      textAlign: 'right',
                    }}
                  >
                    登录密码:
                  </Text>
                  <Form.Item
                    name="password"
                    style={{ margin: 0, flex: 1 }}
                    rules={[{ min: 6, max: 20, message: '密码长度为6-20个字符' }]}
                  >
                    <Input.Password
                      autoComplete={PASSWORD_INPUT_AUTOCOMPLETE}
                      placeholder="如需修改密码请输入"
                    />
                  </Form.Item>
                </div>
              </Col>
              <Col span={12}>
                <div
                  style={{
                    marginBottom: 16,
                    fontSize: 14,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Text
                    style={{
                      color: '#666',
                      marginRight: 8,
                      width: 90,
                      textAlign: 'right',
                    }}
                  >
                    确认密码:
                  </Text>
                  <Form.Item
                    name="confirmPassword"
                    style={{ margin: 0, flex: 1 }}
                    dependencies={['password']}
                    rules={[
                      ({ getFieldValue }) => ({
                        validator(_, value) {
                          const password = getFieldValue('password');
                          if (!password || !value || password === value) {
                            return Promise.resolve();
                          }
                          return Promise.reject(new Error('两次输入的密码不一致'));
                        },
                      }),
                    ]}
                  >
                    <Input.Password
                      autoComplete={PASSWORD_INPUT_AUTOCOMPLETE}
                      placeholder="请再次输入密码"
                    />
                  </Form.Item>
                </div>
              </Col>
            </Row>
          </Form>
        )}
      </div>

      <Divider />

      {/* 安全信息 */}
      <div>
        <Title level={5} style={{ marginBottom: 16 }}>
          安全信息
        </Title>
        <Row gutter={[16, 16]}>
          <InfoRow label="注册时间" value={userDetail?.securityInfo?.registerTime} />
          <InfoRow label="上次登录时间" value={userDetail?.securityInfo?.lastLoginTime} />
          <InfoRow label="上次登录IP" value={userDetail?.securityInfo?.lastLoginIp} />
          <InfoRow
            label="状态"
            value={
              <span
                style={{
                  color: userDetail?.securityInfo?.status === 0 ? '#52c41a' : '#ff4d4f',
                  fontWeight: 500,
                }}
              >
                {userDetail?.securityInfo?.status === 0 ? '启用' : '禁用'}
              </span>
            }
          />
        </Row>
      </div>
    </div>
  );
};

export default UserInfoTab;
