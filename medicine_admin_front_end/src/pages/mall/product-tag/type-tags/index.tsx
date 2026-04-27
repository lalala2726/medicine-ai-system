import { PageContainer } from '@ant-design/pro-components';
import { Button, Empty, Spin, message } from 'antd';
import React from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { getProductTagTypeById, type MallProductTagTypeTypes } from '@/api/mall/productTagType';
import { routePaths } from '@/router/paths';
import TagTable from '../components/TagTable';

/**
 * 标签类型标签列表页面。
 * @returns 页面节点。
 */
const MallProductTagByTypePage: React.FC = () => {
  const [messageApi, contextHolder] = message.useMessage();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const [loading, setLoading] = React.useState(false);
  const [typeDetail, setTypeDetail] =
    React.useState<MallProductTagTypeTypes.MallProductTagTypeAdminVo>();

  /**
   * 加载标签类型详情。
   * @returns 无返回值。
   */
  const loadTypeDetail = React.useCallback(async () => {
    if (!id) {
      setTypeDetail(undefined);
      return;
    }
    setLoading(true);
    try {
      const detail = await getProductTagTypeById(id);
      setTypeDetail(detail);
    } catch (error) {
      console.error('加载标签类型详情失败:', error);
      messageApi.error('加载标签类型详情失败');
      setTypeDetail(undefined);
    } finally {
      setLoading(false);
    }
  }, [id, messageApi]);

  React.useEffect(() => {
    void loadTypeDetail();
  }, [loadTypeDetail]);

  return (
    <PageContainer
      title={typeDetail?.name || searchParams.get('typeName') || '标签列表'}
      extra={[
        <Button
          key="back"
          onClick={() => {
            navigate(routePaths.mallProductTag);
          }}
        >
          返回标签类型管理
        </Button>,
      ]}
    >
      {contextHolder}
      <Spin spinning={loading}>
        {typeDetail && id ? (
          <div style={{ display: 'grid', gap: 16 }}>
            <TagTable
              fixedTypeId={id}
              fixedTypeName={typeDetail.name || searchParams.get('typeName') || undefined}
            />
          </div>
        ) : (
          <Empty description="标签类型不存在或已删除" />
        )}
      </Spin>
    </PageContainer>
  );
};

export default MallProductTagByTypePage;
