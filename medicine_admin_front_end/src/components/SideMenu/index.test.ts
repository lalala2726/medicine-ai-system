import type { MenuRoute } from '@/router';
import { getMenuMatchState } from './index';

const routes: MenuRoute[] = [
  { path: '/welcome', name: '欢迎', icon: 'smile' },
  {
    path: '/mall',
    name: '商城管理',
    children: [
      { path: '/mall/product-list', name: '商品列表', icon: 'shopping' },
      { path: '/mall/product-edit/:id', name: '编辑商品', hideInMenu: true },
    ],
  },
  {
    path: '/system',
    name: '系统管理',
    children: [
      { path: '/system/user', name: '用户管理', icon: 'user' },
      { path: '/system/message-edit/:id', name: '编辑消息', hideInMenu: true },
    ],
  },
];

describe('getMenuMatchState', () => {
  it('matches visible menu items and opens their parent group', () => {
    expect(getMenuMatchState('/mall/product-list', routes)).toEqual({
      openKeys: ['/mall'],
      selectedKeys: ['/mall/product-list'],
    });
  });

  it('keeps the parent group expanded for hidden detail routes', () => {
    expect(getMenuMatchState('/mall/product-edit/123', routes)).toEqual({
      openKeys: ['/mall'],
      selectedKeys: [],
    });
  });

  it('supports hidden routes under other一级分组', () => {
    expect(getMenuMatchState('/system/message-edit/88', routes)).toEqual({
      openKeys: ['/system'],
      selectedKeys: [],
    });
  });

  it('handles top-level leaf routes without opening extra groups', () => {
    expect(getMenuMatchState('/welcome', routes)).toEqual({
      openKeys: [],
      selectedKeys: ['/welcome'],
    });
  });
});
