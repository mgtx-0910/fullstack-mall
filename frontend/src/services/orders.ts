import { request } from '@umijs/max';
import { unwrap } from './common';
import type { ApiResponse, OrderDetail, OrderStatus, OrderSummary, PageResponse } from './types';

export function queryOrders(params: {
  current?: number;
  pageSize?: number;
  orderNo?: string;
  userId?: number;
  status?: OrderStatus;
}) {
  return unwrap(
    request<ApiResponse<PageResponse<OrderSummary>>>('/api/admin/orders', {
      method: 'GET',
      params: {
        pageNum: params.current,
        pageSize: params.pageSize,
        orderNo: params.orderNo,
        userId: params.userId,
        status: params.status,
      },
    }),
  );
}

export function getOrder(id: number) {
  return unwrap(request<ApiResponse<OrderDetail>>(`/api/admin/orders/${id}`));
}

export function cancelOrder(id: number) {
  return unwrap(
    request<ApiResponse<OrderDetail>>(`/api/admin/orders/${id}/cancel`, {
      method: 'PATCH',
    }),
  );
}
