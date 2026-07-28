import { request } from '@umijs/max';
import { unwrap } from './common';
import type { ApiResponse, PageResponse, ProductDetail, ProductStatus, ProductSummary } from './types';

export type ProductQuery = {
  current?: number;
  pageSize?: number;
  keyword?: string;
  categoryId?: number;
  status?: ProductStatus;
};

export type ProductFormValues = {
  categoryId: number;
  title: string;
  description?: string;
};

export function queryProducts(params: ProductQuery) {
  return unwrap(
    request<ApiResponse<PageResponse<ProductSummary>>>('/api/admin/products', {
      method: 'GET',
      params: {
        pageNum: params.current,
        pageSize: params.pageSize,
        keyword: params.keyword,
        categoryId: params.categoryId,
        status: params.status,
      },
    }),
  );
}

export function getProduct(id: number) {
  return unwrap(request<ApiResponse<ProductDetail>>(`/api/admin/products/${id}`));
}

export function createProduct(data: ProductFormValues) {
  return unwrap(
    request<ApiResponse<ProductDetail>>('/api/admin/products', {
      method: 'POST',
      data,
    }),
  );
}

export function updateProduct(id: number, data: ProductFormValues) {
  return unwrap(
    request<ApiResponse<ProductDetail>>(`/api/admin/products/${id}`, {
      method: 'PATCH',
      data,
    }),
  );
}

export function updateProductStatus(id: number, targetStatus: ProductStatus) {
  return unwrap(
    request<ApiResponse<ProductDetail>>(`/api/admin/products/${id}/status`, {
      method: 'PATCH',
      data: { targetStatus },
    }),
  );
}

export function updateProductSubtitle(id: number, subtitle: string) {
  return unwrap(
    request<ApiResponse<ProductDetail>>(`/api/admin/products/${id}/subtitle`, {
      method: 'PATCH',
      data: { subtitle },
    }),
  );
}
