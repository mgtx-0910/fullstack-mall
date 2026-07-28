import type { ApiResponse, PageResponse } from './types';

export async function unwrap<T>(promise: Promise<ApiResponse<T>>): Promise<T> {
  const response = await promise;
  if (response.code !== 'SUCCESS') {
    const error = new Error(response.message || '请求失败');
    (error as Error & { data?: ApiResponse<T> }).data = response;
    throw error;
  }
  return response.data;
}

export function toTableResult<T>(page: PageResponse<T>) {
  return {
    data: page.records,
    success: true,
    total: page.total,
  };
}
