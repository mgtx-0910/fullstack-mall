import React, { useEffect, useState } from 'react';
import { PageContainer } from '@ant-design/pro-components';
import { history, useParams } from '@umijs/max';
import { Button, Card, Descriptions, Empty, Modal, Space, Spin, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { cancelOrder, getOrder } from '@/services/orders';
import type { OrderDetail, OrderItem } from '@/services/types';
import { formatDateTime, formatMoney, orderStatusText, statusTagColor } from '@/utils/format';

const OrderDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const orderId = Number(id);
  const [order, setOrder] = useState<OrderDetail>();
  const [loading, setLoading] = useState(true);

  const reload = async () => {
    setLoading(true);
    try {
      setOrder(await getOrder(orderId));
    } catch (error: any) {
      message.error(error?.message || '订单加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!orderId) {
      message.error('订单 ID 不合法');
      history.push('/orders');
      return;
    }
    reload();
  }, [orderId]);

  const handleCancel = () => {
    if (!order) return;
    Modal.confirm({
      title: '取消订单',
      content: `确定要取消订单「${order.orderNo}」吗？`,
      okText: '确定取消',
      okButtonProps: { danger: true },
      cancelText: '返回',
      onOk: async () => {
        await cancelOrder(order.id);
        message.success('订单已取消');
        await reload();
      },
    });
  };

  const itemColumns: ColumnsType<OrderItem> = [
    { title: '商品', dataIndex: 'productTitle' },
    { title: '商品 ID', dataIndex: 'productId', width: 100 },
    { title: 'SKU ID', dataIndex: 'skuId', width: 100 },
    { title: 'SKU 编码', dataIndex: 'skuCode' },
    { title: '规格', dataIndex: 'specText' },
    { title: '单价', dataIndex: 'unitPrice', render: (value) => formatMoney(value) },
    { title: '数量', dataIndex: 'quantity', width: 80 },
    { title: '小计', dataIndex: 'lineAmount', render: (value) => formatMoney(value) },
  ];

  return (
    <PageContainer
      title="订单详情"
      onBack={() => history.back()}
      extra={
        order?.status === 'PENDING_PAYMENT'
          ? [
              <Button key="cancel" danger onClick={handleCancel}>
                取消订单
              </Button>,
            ]
          : []
      }
    >
      <Spin spinning={loading}>
        {order ? (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Card>
              <Descriptions
                bordered
                column={2}
                items={[
                  { key: 'orderNo', label: '订单号', children: order.orderNo },
                  { key: 'id', label: '订单 ID', children: order.id },
                  {
                    key: 'status',
                    label: '状态',
                    children: <Tag color={statusTagColor(order.status)}>{orderStatusText[order.status]}</Tag>,
                  },
                  { key: 'userId', label: '用户 ID', children: order.userId || '-' },
                  { key: 'totalAmount', label: '订单金额', children: formatMoney(order.totalAmount) },
                  { key: 'createdAt', label: '创建时间', children: formatDateTime(order.createdAt) },
                  { key: 'updatedAt', label: '更新时间', children: formatDateTime(order.updatedAt) },
                  { key: 'expiresAt', label: '支付截止', children: formatDateTime(order.expiresAt) },
                  { key: 'paidAt', label: '支付时间', children: formatDateTime(order.paidAt) },
                  { key: 'cancelledAt', label: '取消时间', children: formatDateTime(order.cancelledAt) },
                  { key: 'closedAt', label: '关闭时间', children: formatDateTime(order.closedAt) },
                ]}
              />
            </Card>
            <Card title="订单项">
              <Table<OrderItem> rowKey="id" columns={itemColumns} dataSource={order.items || []} pagination={false} />
            </Card>
          </Space>
        ) : (
          <Empty description="暂无订单数据" />
        )}
      </Spin>
    </PageContainer>
  );
};

export default OrderDetailPage;
