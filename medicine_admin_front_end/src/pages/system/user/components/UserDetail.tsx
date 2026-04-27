import { Button, Col, Drawer, Empty, Form, Row, Space, Spin, Typography, message } from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { roleOption } from '@/api/system/role';
import { getUserDetail, type UserTypes, updateUser } from '@/api/system/user';
import PermissionButton from '@/components/PermissionButton';
import AvatarUpload from '@/components/Upload/Avatar';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import type { Option } from '@/types';
import UserInfoTab from './UserInfoTab';

const { Text } = Typography;

/**
 * 用户详情抽屉属性。
 */
interface UserDetailProps {
  /** 是否打开抽屉。 */
  open: boolean;
  /** 关闭抽屉回调。 */
  onClose: () => void;
  /** 用户ID。 */
  userId?: number | null;
  /** 用户名。 */
  username?: string;
  /** 是否默认进入编辑态。 */
  autoEdit?: boolean;
  /** 更新成功回调。 */
  onUpdateSuccess?: () => void;
}

/**
 * 用户详情与编辑共用抽屉。
 * @param props 组件属性。
 * @returns 用户详情抽屉节点。
 */
const UserDetail: React.FC<UserDetailProps> = ({
  open,
  onClose,
  userId,
  username,
  autoEdit = false,
  onUpdateSuccess,
}) => {
  const [loading, setLoading] = useState(false);
  const [userDetail, setUserDetail] = useState<UserTypes.UserDetailVo | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [form] = Form.useForm();
  const [messageApi, contextHolder] = message.useMessage();
  const [avatarUrl, setAvatarUrl] = useState<string | undefined>(undefined);
  const [tempAvatarUrl, setTempAvatarUrl] = useState<string | undefined>(undefined);
  const [hasAvatarChanges, setHasAvatarChanges] = useState(false);
  const [roleOptions, setRoleOptions] = useState<Option<number>[]>([]);
  const [roleOptionsLoading, setRoleOptionsLoading] = useState(false);

  /**
   * 加载后端角色选项。
   * @returns 无返回值。
   */
  const fetchRoleOptions = useCallback(async () => {
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

  /**
   * 加载用户详情并初始化表单。
   * @param currentUserId 当前用户ID。
   * @returns 无返回值。
   */
  const fetchUserDetail = useCallback(
    async (currentUserId: number) => {
      try {
        setLoading(true);
        const result = await getUserDetail(currentUserId);
        setUserDetail(result);
        setAvatarUrl(result?.avatar);
        setTempAvatarUrl(result?.avatar);
        setHasAvatarChanges(false);
        form.setFieldsValue({
          userId: result?.basicInfo?.userId,
          realName: result?.basicInfo?.realName,
          nickName: result?.nickName,
          phoneNumber: result?.basicInfo?.phoneNumber,
          email: result?.basicInfo?.email,
          gender: result?.basicInfo?.gender,
          idCard: result?.basicInfo?.idCard,
          roles: result?.roles,
          status: result?.securityInfo?.status,
          password: undefined,
          confirmPassword: undefined,
        });
      } catch (error) {
        console.error('获取用户详情失败:', error);
        messageApi.error('获取用户详情失败');
      } finally {
        setLoading(false);
      }
    },
    [form, messageApi],
  );

  useEffect(() => {
    if (!open || !userId) {
      return;
    }

    setIsEditing(autoEdit);
    void fetchRoleOptions();
    void fetchUserDetail(userId);
  }, [autoEdit, fetchRoleOptions, fetchUserDetail, open, userId]);

  /**
   * 处理头像变更。
   * @param url 新头像地址。
   * @returns 无返回值。
   */
  const handleAvatarChange = async (url: string) => {
    try {
      setTempAvatarUrl(url);
      setHasAvatarChanges(true);
      messageApi.success('头像已修改，请点击"完成"按钮保存');
    } catch (error) {
      console.error('头像上传失败:', error);
      messageApi.error('头像上传失败');
    }
  };

  /**
   * 进入编辑态。
   * @returns 无返回值。
   */
  const handleEditClick = () => {
    setIsEditing(true);
  };

  /**
   * 取消编辑并恢复只读态。
   * @returns 无返回值。
   */
  const handleCancelEdit = () => {
    setIsEditing(false);
    form.setFieldsValue({
      userId: userDetail?.basicInfo?.userId,
      realName: userDetail?.basicInfo?.realName,
      nickName: userDetail?.nickName,
      phoneNumber: userDetail?.basicInfo?.phoneNumber,
      email: userDetail?.basicInfo?.email,
      gender: userDetail?.basicInfo?.gender,
      idCard: userDetail?.basicInfo?.idCard,
      roles: userDetail?.roles,
      status: userDetail?.securityInfo?.status,
      password: undefined,
      confirmPassword: undefined,
    });
    setTempAvatarUrl(avatarUrl);
    setHasAvatarChanges(false);
  };

  /**
   * 提交编辑结果。
   * @returns 无返回值。
   */
  const handleFinishEdit = async () => {
    try {
      const values = await form.validateFields();

      if (values.password && values.password !== values.confirmPassword) {
        messageApi.error('两次输入的密码不一致');
        return;
      }

      setLoading(true);

      const updateData: UserTypes.UserUpdateRequest = {
        id: userDetail?.basicInfo?.userId,
        realName: values.realName,
        nickname: values.nickName,
        phoneNumber: values.phoneNumber,
        email: values.email,
        gender: values.gender,
        idCard: values.idCard,
        roles: values.roles,
        status: values.status,
      };

      if (values.password) {
        updateData.password = values.password;
      }

      if (hasAvatarChanges && tempAvatarUrl !== avatarUrl) {
        updateData.avatar = tempAvatarUrl;
      }

      await updateUser(updateData);
      messageApi.success('更新成功');
      setIsEditing(false);
      if (userId) {
        await fetchUserDetail(userId);
      }
      onUpdateSuccess?.();
    } catch (error: any) {
      console.error('更新用户信息失败:', error);
      if (error?.errorFields) {
        messageApi.error('请检查表单输入');
        return;
      }
      messageApi.error('更新失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {contextHolder}
      <Drawer
        title="用户详情"
        placement="right"
        width={800}
        open={open}
        onClose={onClose}
        destroyOnHidden
        extra={
          isEditing ? (
            <Space>
              <PermissionButton
                type="primary"
                access={ADMIN_PERMISSIONS.systemUser.update}
                onClick={handleFinishEdit}
                loading={loading}
              >
                完成
              </PermissionButton>
              <Button onClick={handleCancelEdit}>取消</Button>
            </Space>
          ) : (
            <Space>
              <PermissionButton
                type="primary"
                access={ADMIN_PERMISSIONS.systemUser.update}
                onClick={handleEditClick}
              >
                编辑
              </PermissionButton>
            </Space>
          )
        }
      >
        <Spin spinning={loading}>
          {userDetail ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
              <div
                style={{
                  padding: '4px 0 20px',
                  borderBottom: '1px solid #f0f0f0',
                }}
              >
                <Row gutter={24} align="top">
                  <Col>
                    <AvatarUpload
                      value={tempAvatarUrl}
                      onChange={handleAvatarChange}
                      shapes={['circle']}
                      uploadText={hasAvatarChanges ? '头像已修改' : '修改头像'}
                      disabled={!isEditing}
                    />
                    {isEditing && hasAvatarChanges && (
                      <div
                        style={{
                          marginTop: 8,
                          textAlign: 'center',
                          fontSize: 12,
                          color: '#faad14',
                        }}
                      >
                        请点击"完成"保存头像
                      </div>
                    )}
                    {!isEditing && (
                      <div
                        style={{
                          marginTop: 8,
                          textAlign: 'center',
                          fontSize: 12,
                          color: '#8c8c8c',
                        }}
                      >
                        点击右上角"编辑"可修改资料
                      </div>
                    )}
                  </Col>
                  <Col flex="1">
                    <div style={{ marginBottom: 16 }}>
                      <Text
                        style={{
                          color: '#262626',
                          fontSize: 16,
                          fontWeight: 600,
                        }}
                      >
                        {userDetail.nickName || userDetail.basicInfo?.realName || '未命名用户'}
                      </Text>
                    </div>
                    <Row gutter={[24, 12]}>
                      <Col span={12}>
                        <Text type="secondary">用户ID：{userDetail.basicInfo?.userId || '-'}</Text>
                      </Col>
                      <Col span={12}>
                        <Text type="secondary">用户名：{username || '-'}</Text>
                      </Col>
                      <Col span={12}>
                        <Text type="secondary">
                          当前状态：{userDetail.securityInfo?.status === 0 ? '启用' : '禁用'}
                        </Text>
                      </Col>
                    </Row>
                  </Col>
                </Row>
              </div>

              <UserInfoTab
                userDetail={userDetail}
                isEditing={isEditing}
                form={form}
                roleOptions={roleOptions}
                roleOptionsLoading={roleOptionsLoading}
              />
            </div>
          ) : (
            <Empty description="暂无用户详情信息" />
          )}
        </Spin>
      </Drawer>
    </>
  );
};

export default UserDetail;
