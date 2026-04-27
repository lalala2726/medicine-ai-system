import { PageContainer } from '@ant-design/pro-components';
import React from 'react';
import TagTypeTable from './components/TagTypeTable';

/**
 * 商品标签管理页面。
 */
const MallProductTagPage: React.FC = () => {
  return (
    <PageContainer title="商品标签">
      <TagTypeTable />
    </PageContainer>
  );
};

export default MallProductTagPage;
