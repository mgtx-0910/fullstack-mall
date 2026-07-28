import { request } from '@umijs/max';
import { unwrap } from './common';
import type { ApiResponse, Sku } from './types';

export type SkuCreateValues = {
  skuCode: string;
  specText: string;
  salePrice: number;
  initialStock: number;
};

export function querySkus(productId: number) {
  return unwrap(request<ApiResponse<Sku[]>>(`/api/admin/products/${productId}/skus`));
}

export function createSku(productId: number, data: SkuCreateValues) {
  return unwrap(
    request<ApiResponse<Sku>>(`/api/admin/products/${productId}/skus`, {
      method: 'POST',
      data,
    }),
  );
}

export function updateSkuPrice(skuId: number, salePrice: number, expectedVersion: number) {
  return unwrap(
    request<ApiResponse<Sku>>(`/api/admin/skus/${skuId}/price`, {
      method: 'PATCH',
      data: { salePrice, expectedVersion },
    }),
  );
}

export function adjustSkuStock(skuId: number, delta: number, expectedVersion: number) {
  return unwrap(
    request<ApiResponse<Sku>>(`/api/admin/skus/${skuId}/stock`, {
      method: 'PATCH',
      data: { delta, expectedVersion },
    }),
  );
}
