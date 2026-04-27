import { PageContainer } from '@ant-design/pro-components';
import { Avatar, Card, Col, Empty, Row, Spin, Tabs, Typography, message } from 'antd';
import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getUserDetail, type UserTypes } from '@/api/system/user';
import { routePaths } from '@/router/paths';
import ConsumptionRecordTab from '../components/ConsumptionRecordTab';
import UserActivationLogTab from '../components/UserActivationLogTab';
import UserCouponLogTab from '../components/UserCouponLogTab';
import UserCouponTab from '../components/UserCouponTab';
import WalletFlowTab from '../components/WalletFlowTab';

const { Text, Title } = Typography;

/** 资产页默认激活标签。 */
const DEFAULT_ACTIVE_TAB = 'walletFlow';

/**
 * 格式化金额文本。
 * @param value 原始金额。
 * @returns 格式化后的金额文本。
 */
function formatCurrency(value?: string): string {
  if (!value) {
    return '0.00';
  }
  return Number(value).toFixed(2);
}

/**
 * 构建用户显示名称。
 * @param userDetail 用户详情。
 * @returns 用户显示名称。
 */
function buildDisplayName(userDetail: UserTypes.UserDetailVo | null): string {
  return userDetail?.nickName || userDetail?.basicInfo?.realName || '未命名用户';
}

/**
 * 用户资产明细页面。
 * @returns 用户资产明细页面节点。
 */
const UserAssetsPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const [messageApi, contextHolder] = message.useMessage();
  const [loading, setLoading] = useState(false);
  const [userDetail, setUserDetail] = useState<UserTypes.UserDetailVo | null>(null);
  const [activeTab, setActiveTab] = useState(DEFAULT_ACTIVE_TAB);

  useEffect(() => {
    if (!id) {
      return;
    }

    /**
     * 加载用户详情数据。
     * @returns 无返回值。
     */
    const fetchUserDetail = async () => {
      try {
        setLoading(true);
        const result = await getUserDetail(id);
        setUserDetail(result);
      } catch (error) {
        console.error('获取用户资产信息失败:', error);
        messageApi.error('获取用户资产信息失败');
      } finally {
        setLoading(false);
      }
    };

    void fetchUserDetail();
  }, [id, messageApi]);

  return (
    <PageContainer title="用户资产明细" onBack={() => navigate(routePaths.systemUser)}>
      {contextHolder}
      <Spin spinning={loading}>
        {userDetail && id ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Card>
              <Row gutter={[24, 24]} align="middle">
                <Col xs={24} xl={8}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                    <Avatar size={88} src={userDetail.avatar}>
                      {buildDisplayName(userDetail).slice(0, 1)}
                    </Avatar>
                    <div>
                      <Title level={4} style={{ margin: 0, marginBottom: 8 }}>
                        {buildDisplayName(userDetail)}
                      </Title>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                        <Text type="secondary">用户ID：{userDetail.basicInfo?.userId || '-'}</Text>
                        <Text type="secondary">
                          手机号：{userDetail.basicInfo?.phoneNumber || '-'}
                        </Text>
                        <Text type="secondary">邮箱：{userDetail.basicInfo?.email || '-'}</Text>
                      </div>
                    </div>
                  </div>
                </Col>
                <Col xs={24} xl={16}>
                  <Row gutter={[16, 16]}>
                    <Col xs={24} md={8}>
                      <Card size="small">
                        <Text type="secondary">钱包余额</Text>
                        <Title level={3} style={{ margin: '12px 0 0' }}>
                          {formatCurrency(userDetail.walletBalance)}
                        </Title>
                      </Card>
                    </Col>
                    <Col xs={24} md={8}>
                      <Card size="small">
                        <Text type="secondary">总计订单</Text>
                        <Title level={3} style={{ margin: '12px 0 0' }}>
                          {userDetail.totalOrders || 0}
                        </Title>
                      </Card>
                    </Col>
                    <Col xs={24} md={8}>
                      <Card size="small">
                        <Text type="secondary">总消费金额</Text>
                        <Title level={3} style={{ margin: '12px 0 0' }}>
                          {formatCurrency(userDetail.totalConsume)}
                        </Title>
                      </Card>
                    </Col>
                  </Row>
                </Col>
              </Row>
            </Card>

            <Card>
              <Tabs
                activeKey={activeTab}
                onChange={setActiveTab}
                items={[
                  {
                    key: 'walletFlow',
                    label: '钱包流水',
                    children: (
                      <WalletFlowTab
                        userId={Number(id)}
                        visible={activeTab === 'walletFlow'}
                        tableSize="middle"
                      />
                    ),
                  },
                  {
                    key: 'consume',
                    label: '消费记录',
                    children: (
                      <ConsumptionRecordTab
                        userId={Number(id)}
                        visible={activeTab === 'consume'}
                        tableSize="middle"
                      />
                    ),
                  },
                  {
                    key: 'coupon',
                    label: '优惠券',
                    children: (
                      <UserCouponTab
                        userId={Number(id)}
                        visible={activeTab === 'coupon'}
                        tableSize="middle"
                      />
                    ),
                  },
                  {
                    key: 'couponLog',
                    label: '优惠券记录',
                    children: (
                      <UserCouponLogTab
                        userId={Number(id)}
                        visible={activeTab === 'couponLog'}
                        tableSize="middle"
                      />
                    ),
                  },
                  {
                    key: 'activationLog',
                    label: '激活码记录',
                    children: (
                      <UserActivationLogTab
                        userId={Number(id)}
                        visible={activeTab === 'activationLog'}
                        tableSize="middle"
                      />
                    ),
                  },
                ]}
              />
            </Card>
          </div>
        ) : (
          <Empty description="暂无用户资产信息" />
        )}
      </Spin>
    </PageContainer>
  );
};

export default UserAssetsPage;
