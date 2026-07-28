import React, { useRef } from 'react';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Link } from '@umijs/max';
import { Button, Modal, Space, message } from 'antd';
import { cancelOrder, queryOrders } from '@/services/orders';
import type { OrderSummary } from '@/services/types';
import { toTableResult } from '@/services/common';
import { formatDateTime, formatMoney, orderStatusValueEnum } from '@/utils/format';

const OrderList: React.FC = () => {
  const actionRef = useRef<ActionType>();

  const handleCancel = (record: OrderSummary) => {
    Modal.confirm({
      title: '取消订单',
      content: `确定要取消订单「${record.orderNo}」吗？取消后会释放库存并关闭待支付流水。`,
      okText: '确定取消',
      okButtonProps: { danger: true },
      cancelText: '返回',
      onOk: async () => {
        await cancelOrder(record.id);
        message.success('订单已取消');
        actionRef.current?.reload();
      },
    });
  };

  const columns: ProColumns<OrderSummary>[] = [
    { title: '订单 ID', dataIndex: 'id', width: 90, search: false },
    { title: '订单号', dataIndex: 'orderNo', copyable: true },
    { title: '用户 ID', dataIndex: 'userId', valueType: 'digit' },
    { title: '状态', dataIndex: 'status', valueType: 'select', valueEnum: orderStatusValueEnum },
    { title: '订单金额', dataIndex: 'totalAmount', search: false, render: (_, record) => formatMoney(record.totalAmount) },
    { title: '创建时间', dataIndex: 'createdAt', search: false, render: (_, record) => formatDateTime(record.createdAt) },
    { title: '支付截止', dataIndex: 'expiresAt', search: false, render: (_, record) => formatDateTime(record.expiresAt) },
    {
      title: '操作',
      valueType: 'option',
      width: 160,
      render: (_, record) => (
        <Space>
          <Link to={`/orders/${record.id}`}>详情</Link>
          {record.status === 'PENDING_PAYMENT' && (
            <Button type="link" danger size="small" onClick={() => handleCancel(record)}>
              取消
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title="订单管理">
      <ProTable<OrderSummary>
        rowKey="id"
        actionRef={actionRef}
        columns={columns}
        request={async (params) => toTableResult(await queryOrders(params))}
        pagination={{ defaultPageSize: 10 }}
      />
    </PageContainer>
  );
};

export default OrderList;
