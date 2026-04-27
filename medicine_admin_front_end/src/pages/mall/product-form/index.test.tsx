import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import ProductEdit from './index';

const mockNavigate = jest.fn();
const mockGetCategoryTree = jest.fn();
const mockGetMallProductById = jest.fn();
const mockLoadEnabledProductTagSelectGroups = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useParams: () => ({
    id: '101',
  }),
}));

jest.mock('@ant-design/pro-components', () => ({
  PageContainer: ({ children }: any) => <div>{children}</div>,
}));

jest.mock('@/contexts/ThemeContext', () => ({
  useThemeContext: () => ({
    isDark: false,
  }),
}));

jest.mock('@/api/mall/category', () => ({
  tree: (...args: any[]) => mockGetCategoryTree(...args),
}));

jest.mock('@/api/mall/product', () => ({
  addMallProduct: jest.fn(),
  updateMallProduct: jest.fn(),
  getMallProductById: (...args: any[]) => mockGetMallProductById(...args),
}));

jest.mock('@/pages/mall/components/productTagUtils', () => ({
  ...jest.requireActual('@/pages/mall/components/productTagUtils'),
  loadEnabledProductTagSelectGroups: (...args: any[]) =>
    mockLoadEnabledProductTagSelectGroups(...args),
}));

jest.mock('@/pages/mall/product-list/components/BasicInfo', () => {
  const ReactMock = require('react') as typeof import('react');
  const { Form } = require('antd') as typeof import('antd');

  return function MockBasicInfo(props: any) {
    const form = Form.useFormInstance();
    const tagIds = Form.useWatch('tagIds', { form, preserve: true }) || [];

    return (
      <div>
        <div data-testid="tag-group-count">{String(props.tagGroups?.length ?? 0)}</div>
        <div data-testid="product-form-tag-ids">{JSON.stringify(tagIds)}</div>
      </div>
    );
  };
});

jest.mock('@/pages/mall/product-list/components/DrugDetail', () => () => (
  <div data-testid="drug-detail-section">药品详情表单</div>
));

describe('ProductEdit page', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    mockGetCategoryTree.mockResolvedValue([]);
    mockLoadEnabledProductTagSelectGroups.mockResolvedValue([
      {
        typeId: '1',
        typeCode: 'EFFICACY',
        typeName: '功效',
        options: [
          { label: '退烧', value: '11' },
          { label: '止痛', value: '12' },
        ],
      },
    ]);
    mockGetMallProductById.mockResolvedValue({
      id: '101',
      name: '布洛芬缓释胶囊',
      categoryId: '9',
      tags: [
        { id: '11', name: '退烧', typeId: '1', typeCode: 'EFFICACY', typeName: '功效' },
        { id: '12', name: '止痛', typeId: '1', typeCode: 'EFFICACY', typeName: '功效' },
      ],
      drugDetail: {
        warmTips: '饭后服用',
      },
    });
  });

  it('maps product tags into form tagIds when editing', async () => {
    render(<ProductEdit />);

    await waitFor(() =>
      expect(screen.getByTestId('product-form-tag-ids').textContent).toBe('["11","12"]'),
    );
    expect(screen.getByTestId('tag-group-count').textContent).toBe('1');
  });
});
