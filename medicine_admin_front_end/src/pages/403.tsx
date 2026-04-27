import { Button, Card, Result } from 'antd';
import React from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * 无权限页面。
 * @returns 无权限结果页。
 */
const ForbiddenPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <Card variant="borderless">
      <Result
        status="403"
        title="403"
        subTitle="抱歉，您没有权限访问该页面。"
        extra={
          <Button type="primary" onClick={() => navigate('/')}>
            返回首页
          </Button>
        }
      />
    </Card>
  );
};

export default ForbiddenPage;
