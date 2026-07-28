const routes = [
  {
    path: '/user/login',
    layout: false,
    component: './Login',
  },
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/dashboard',
    name: '首页',
    icon: 'DashboardOutlined',
    component: './Dashboard',
  },
  {
    path: '/products',
    name: '商品管理',
    icon: 'ShoppingOutlined',
    component: './Products/List',
  },
  {
    path: '/products/create',
    name: '新建商品',
    hideInMenu: true,
    component: './Products/Create',
  },
  {
    path: '/products/:id/edit',
    name: '编辑商品',
    hideInMenu: true,
    component: './Products/Edit',
  },
  {
    path: '/products/:id/skus',
    name: 'SKU 管理',
    hideInMenu: true,
    component: './Products/Sku',
  },
  {
    path: '/users',
    name: '用户管理',
    icon: 'TeamOutlined',
    component: './Users/List',
  },
  {
    path: '/orders',
    name: '订单管理',
    icon: 'UnorderedListOutlined',
    component: './Orders/List',
  },
  {
    path: '/orders/:id',
    name: '订单详情',
    hideInMenu: true,
    component: './Orders/Detail',
  },
  {
    path: '/categories',
    name: '分类管理',
    icon: 'TagsOutlined',
    component: './Categories/List',
  },
  {
    path: '*',
    redirect: '/dashboard',
  },
];

export default routes;
