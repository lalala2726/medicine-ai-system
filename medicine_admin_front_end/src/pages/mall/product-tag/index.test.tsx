import React from 'react';
import { render, screen } from '@testing-library/react';
import MallProductTagPage from './index';

jest.mock('@ant-design/pro-components', () => ({
  PageContainer: ({ title, children }: any) => (
    <div>
      <div>{title}</div>
      {children}
    </div>
  ),
}));

jest.mock('./components/TagTypeTable', () => () => (
  <div data-testid="tag-type-table">标签类型表格</div>
));

jest.mock('./components/TagTable', () => () => <div data-testid="tag-table">商品标签表格</div>);

describe('MallProductTagPage', () => {
  it('renders tag management page on first load', () => {
    render(<MallProductTagPage />);

    expect(screen.getByText('商品标签')).toBeTruthy();
    expect(screen.getByText('标签类型管理')).toBeTruthy();
    expect(screen.getByText('商品标签管理')).toBeTruthy();
    expect(screen.getByTestId('tag-type-table')).toBeTruthy();
  });
});
