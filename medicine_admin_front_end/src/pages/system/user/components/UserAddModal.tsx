import { PASSWORD_INPUT_AUTOCOMPLETE, TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { Col, Form, Input, Modal, message, Row, Select } from 'antd';
import React, { useEffect } from 'react';
import { roleOption } from '@/api/system/role';
import { addUser, type UserTypes } from '@/api/system/user';
import AvatarUpload from '@/components/Upload/Avatar';
import type { Option } from '@/types';

/**
 * 新增用户输入框禁用常见密码管理器识别的属性。
 */
const USER_ADD_DISABLE_AUTOFILL_PROPS = {
  'data-lpignore': 'true',
  'data-1p-ignore': 'true',
  'data-form-type': 'other',
} as const;

/**
 * 新增用户密码输入框禁用常见密码管理器识别的属性。
 */
const USER_ADD_DISABLE_PASSWORD_AUTOFILL_PROPS = {
  ...USER_ADD_DISABLE_AUTOFILL_PROPS,
} as const;

interface UserAddModalProps {
  /** 是否展示新增用户弹窗。 */
  visible: boolean;
  /** 关闭新增用户弹窗回调。 */
  onCancel: () => void;
  /** 新增成功回调。 */
  onSuccess: () => void;
}

/**
 * 新增用户弹窗。
 * @param props 新增用户弹窗属性。
 * @returns 新增用户弹窗节点。
 */
const UserAddModal: React.FC<UserAddModalProps> = ({ visible, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = React.useState(false);
  const [messageApi, contextHolder] = message.useMessage();
  const [avatarUrl, setAvatarUrl] = React.useState<string>('');
  const [roleOptions, setRoleOptions] = React.useState<Option<number>[]>([]);
  const [roleOptionsLoading, setRoleOptionsLoading] = React.useState(false);

  /**
   * 加载后端角色选项。
   * @returns 无返回值。
   */
  const fetchRoleOptions = React.useCallback(async () => {
    try {
      setRoleOptionsLoading(true);
      const result = await roleOption();
      setRoleOptions(result);
    } catch (error) {
      console.error('获取角色选项失败:', error);
      messageApi.error('获取角色选项失败');
    } finally {
      setRoleOptionsLoading(false);
    }
  }, [messageApi]);

  useEffect(() => {
    if (visible) {
      form.resetFields();
      setAvatarUrl('');
      void fetchRoleOptions();
    }
  }, [fetchRoleOptions, form, visible]);

  /**
   * 提交新增用户表单。
   * @returns 无返回值。
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const addData: UserTypes.UserAddRequest = {
        username: values.username,
        nickname: values.nickname,
        password: values.password,
        realName: values.realName,
        phoneNumber: values.phoneNumber,
        email: values.email,
        idCard: values.idCard,
        avatar: avatarUrl || values.avatar,
        roles: values.roles,
        status: values.status,
      };

      await addUser(addData);
      messageApi.success('新增用户成功');
      form.resetFields();
      onSuccess();
    } catch (error: any) {
      if (error.errorFields) {
        // 表单验证错误
        return;
      }
      console.error('新增用户失败:', error);
      messageApi.error(error.message || '新增用户失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {contextHolder}
      <Modal
        title="新增用户"
        open={visible}
        onCancel={onCancel}
        onOk={handleSubmit}
        confirmLoading={loading}
        width={800}
        destroyOnHidden
      >
        {/* 用户头像 */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'center',
            marginTop: 24,
            marginBottom: 24,
          }}
        >
          <AvatarUpload
            value={avatarUrl}
            onChange={setAvatarUrl}
            shapes={['circle']}
            uploadText="上传头像"
          />
        </div>

        <Form form={form} layout="vertical" autoComplete={TEXT_INPUT_AUTOCOMPLETE}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="用户名"
                name="username"
                rules={[
                  { required: true, message: '请输入用户名' },
                  { max: 50, message: '用户名长度不能超过50个字符' },
                ]}
              >
                <Input
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  {...USER_ADD_DISABLE_AUTOFILL_PROPS}
                  id="medicine-user-add-field-1"
                  name="medicine_user_add_field_1"
                  placeholder="请输入用户名"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="昵称"
                name="nickname"
                rules={[{ max: 50, message: '昵称长度不能超过50个字符' }]}
              >
                <Input
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  {...USER_ADD_DISABLE_AUTOFILL_PROPS}
                  id="medicine-user-add-field-2"
                  name="medicine_user_add_field_2"
                  placeholder="请输入昵称"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="密码"
                name="password"
                rules={[
                  { required: true, message: '请输入密码' },
                  { min: 6, max: 20, message: '密码长度为6-20个字符' },
                ]}
              >
                <Input.Password
                  autoComplete={PASSWORD_INPUT_AUTOCOMPLETE}
                  {...USER_ADD_DISABLE_PASSWORD_AUTOFILL_PROPS}
                  id="medicine-user-add-field-3"
                  name="medicine_user_add_field_3"
                  placeholder="请输入密码"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="确认密码"
                name="confirmPassword"
                dependencies={['password']}
                rules={[
                  { required: true, message: '请再次输入密码' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue('password') === value) {
                        return Promise.resolve();
                      }
                      return Promise.reject(new Error('两次输入的密码不一致'));
                    },
                  }),
                ]}
              >
                <Input.Password
                  autoComplete={PASSWORD_INPUT_AUTOCOMPLETE}
                  {...USER_ADD_DISABLE_PASSWORD_AUTOFILL_PROPS}
                  id="medicine-user-add-field-4"
                  name="medicine_user_add_field_4"
                  placeholder="请再次输入密码"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="真实姓名"
                name="realName"
                rules={[{ max: 50, message: '真实姓名长度不能超过50个字符' }]}
              >
                <Input
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  {...USER_ADD_DISABLE_AUTOFILL_PROPS}
                  id="medicine-user-add-field-5"
                  name="medicine_user_add_field_5"
                  placeholder="请输入真实姓名"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="手机号"
                name="phoneNumber"
                rules={[
                  {
                    pattern: /^1[3-9]\d{9}$/,
                    message: '请输入正确的手机号',
                  },
                ]}
              >
                <Input
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  {...USER_ADD_DISABLE_AUTOFILL_PROPS}
                  id="medicine-user-add-field-6"
                  name="medicine_user_add_field_6"
                  placeholder="请输入手机号"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="邮箱"
                name="email"
                rules={[
                  { type: 'email', message: '请输入正确的邮箱地址' },
                  { max: 100, message: '邮箱长度不能超过100个字符' },
                ]}
              >
                <Input
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  {...USER_ADD_DISABLE_AUTOFILL_PROPS}
                  id="medicine-user-add-field-7"
                  name="medicine_user_add_field_7"
                  placeholder="请输入邮箱"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="身份证号"
                name="idCard"
                rules={[
                  {
                    pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/,
                    message: '请输入正确的身份证号',
                  },
                ]}
              >
                <Input
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  {...USER_ADD_DISABLE_AUTOFILL_PROPS}
                  id="medicine-user-add-field-8"
                  name="medicine_user_add_field_8"
                  placeholder="请输入身份证号"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="角色"
                name="roles"
                rules={[{ required: true, message: '请选择角色' }]}
              >
                <Select
                  mode="multiple"
                  options={roleOptions}
                  loading={roleOptionsLoading}
                  placeholder="请选择角色"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="状态" name="status" initialValue={0}>
                <Select placeholder="请选择状态">
                  <Select.Option value={0}>启用</Select.Option>
                  <Select.Option value={1}>禁用</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </>
  );
};

export default UserAddModal;
