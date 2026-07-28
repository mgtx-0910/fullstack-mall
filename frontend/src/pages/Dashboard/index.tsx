import React from 'react';
import { PageContainer, ProCard } from '@ant-design/pro-components';
import { history, useModel } from '@umijs/max';
import { Button, Space, Typography } from 'antd';

const Dashboard: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;

  return (
    <PageContainer title="后台首页" subTitle="精简版管理后台入口">
      <ProCard split="vertical" bordered>
        <ProCard title="当前账号" colSpan="35%">
          <Typography.Paragraph>
            用户名：{currentUser?.username || '-'}
            <br />
            昵称：{currentUser?.nickname || '-'}
            <br />
            角色：{currentUser?.role || '-'}
          </Typography.Paragraph>
        </ProCard>
        <ProCard title="常用操作">
          <Space wrap>
            <Button type="primary" onClick={() => history.push('/products/create')}>
              新建商品
            </Button>
            <Button onClick={() => history.push('/products')}>商品管理</Button>
            <Button onClick={() => history.push('/orders')}>订单管理</Button>
            <Button onClick={() => history.push('/categories')}>分类校验</Button>
          </Space>
        </ProCard>
      </ProCard>
    </PageContainer>
  );
};

export default Dashboard;
