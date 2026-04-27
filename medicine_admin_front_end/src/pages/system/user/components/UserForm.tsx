import { PASSWORD_INPUT_AUTOCOMPLETE, TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { Form, Input, Modal, Select, Switch, message } from 'antd';
import React, { useEffect } from 'react';
import { roleOption } from '@/api/system/role';
import type { UserTypes } from '@/api/system/user';
import type { Option } from '@/types';

interface UserFormProps {
  visible: boolean;
  title: string;
  record?: UserTypes.UserListVo;
  onSubmit: (values: UserTypes.UserAddRequest | UserTypes.UserUpdateRequest) => Promise<void>;
  onCancel: () => void;
  loading: boolean;
}

const UserForm: React.FC<UserFormProps> = ({
  visible,
  title,
  record,
  onSubmit,
  onCancel,
  loading,
}) => {
  const [form] = Form.useForm();
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
      message.error('获取角色选项失败');
    } finally {
      setRoleOptionsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (visible && record) {
      form.setFieldsValue({
        username: record.username,
        nickname: record.nickname,
        avatar: record.avatar,
        status: record.status === 0,
      });
    } else if (visible && !record) {
      form.resetFields();
    }
    if (visible) {
      void fetchRoleOptions();
    }
  }, [fetchRoleOptions, visible, record, form]);

  /**
   * 提交用户表单。
   * @returns 无返回值。
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const submitValues = {
        ...values,
        status: values.status ? 0 : 1,
      };
      await onSubmit(submitValues);
    } catch (error) {
      console.error('Form validation failed:', error);
    }
  };

  return (
    <Modal
      title={title}
      open={visible}
      onOk={handleSubmit}
      onCancel={onCancel}
      confirmLoading={loading}
      width={600}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" requiredMark={false}>
        <Form.Item
          name="username"
          label="用户名"
          rules={[
            { required: true, message: '请输入用户名' },
            { min: 3, max: 20, message: '用户名长度为3-20个字符' },
          ]}
        >
          <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入用户名" />
        </Form.Item>

        <Form.Item
          name="nickname"
          label="昵称"
          rules={[
            { required: true, message: '请输入昵称' },
            { max: 50, message: '昵称长度不能超过50个字符' },
          ]}
        >
          <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入昵称" />
        </Form.Item>

        {!record && (
          <Form.Item
            name="password"
            label="密码"
            rules={[
              { required: true, message: '请输入密码' },
              { min: 6, max: 20, message: '密码长度为6-20个字符' },
            ]}
          >
            <Input.Password autoComplete={PASSWORD_INPUT_AUTOCOMPLETE} placeholder="请输入密码" />
          </Form.Item>
        )}

        <Form.Item
          name="avatar"
          label="头像URL"
          rules={[{ type: 'url', message: '请输入有效的URL' }]}
        >
          <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入头像URL" />
        </Form.Item>

        <Form.Item name="roles" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
          <Select
            mode="multiple"
            options={roleOptions}
            loading={roleOptionsLoading}
            placeholder="请选择角色"
          />
        </Form.Item>

        <Form.Item name="status" label="状态" valuePropName="checked" initialValue={true}>
          <Switch checkedChildren="启用" unCheckedChildren="禁用" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default UserForm;
