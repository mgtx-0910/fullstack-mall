import type { OrderStatus, ProductStatus } from '@/services/types';

export const productStatusText: Record<ProductStatus, string> = {
  DRAFT: '草稿',
  ON_SALE: '已上架',
  OFF_SHELF: '已下架',
};

export const productStatusValueEnum = {
  DRAFT: { text: '草稿', status: 'Default' },
  ON_SALE: { text: '已上架', status: 'Success' },
  OFF_SHELF: { text: '已下架', status: 'Warning' },
};

export const orderStatusText: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  CANCELLED: '已取消',
  CLOSED: '已关闭',
};

export const orderStatusValueEnum = {
  PENDING_PAYMENT: { text: '待支付', status: 'Processing' },
  PAID: { text: '已支付', status: 'Success' },
  CANCELLED: { text: '已取消', status: 'Default' },
  CLOSED: { text: '已关闭', status: 'Warning' },
};

export function formatMoney(value?: number | string | null) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  const numberValue = Number(value);
  if (Number.isNaN(numberValue)) {
    return String(value);
  }
  return `¥${numberValue.toFixed(2)}`;
}

export function formatDateTime(value?: string | null) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 19);
}

export function statusTagColor(status?: ProductStatus | OrderStatus) {
  switch (status) {
    case 'ON_SALE':
    case 'PAID':
      return 'green';
    case 'PENDING_PAYMENT':
      return 'blue';
    case 'OFF_SHELF':
    case 'CLOSED':
      return 'orange';
    case 'CANCELLED':
      return 'default';
    default:
      return 'default';
  }
}
