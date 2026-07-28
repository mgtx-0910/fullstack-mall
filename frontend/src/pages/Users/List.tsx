import React from 'react';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { queryUsers } from '@/services/users';
import type { UserSummary } from '@/services/types';
import { toTableResult } from '@/services/common';
import { formatDateTime } from '@/utils/format';

const userStatusValueEnum = {
  1: { text: '启用', status: 'Success' },
  0: { text: '停用', status: 'Default' },
};

const UserList: React.FC = () => {
  const columns: ProColumns<UserSummary>[] = [
    { title: 'ID', dataIndex: 'id', width: 90, search: false },
    { title: '关键字', dataIndex: 'keyword', hideInTable: true, fieldProps: { placeholder: '用户名 / 昵称' } },
    { title: '用户名', dataIndex: 'username', search: false },
    { title: '昵称', dataIndex: 'nickname', search: false },
    { title: '状态', dataIndex: 'status', valueType: 'select', valueEnum: userStatusValueEnum },
    { title: '注册时间', dataIndex: 'createdAt', search: false, render: (_, record) => formatDateTime(record.createdAt) },
  ];

  return (
    <PageContainer title="用户管理">
      <ProTable<UserSummary>
        rowKey="id"
        columns={columns}
        request={async (params) => toTableResult(await queryUsers(params))}
        pagination={{ defaultPageSize: 10 }}
      />
    </PageContainer>
  );
};

export default UserList;
