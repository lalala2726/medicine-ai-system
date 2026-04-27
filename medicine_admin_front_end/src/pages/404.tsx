import { useNavigate } from 'react-router-dom';
import { Button, Card, Result } from 'antd';
import React from 'react';

const NoFoundPage: React.FC = () => {
  const navigate = useNavigate();
  return (
    <Card variant="borderless">
      <Result
        status="404"
        title="404"
        subTitle={'抱歉，您访问的页面不存在。'}
        extra={
          <Button type="primary" onClick={() => navigate('/')}>
            {'返回首页'}
          </Button>
        }
      />
    </Card>
  );
};
export default NoFoundPage;
