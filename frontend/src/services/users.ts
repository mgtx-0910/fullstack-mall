import { request } from '@umijs/max';
import { unwrap } from './common';
import type { ApiResponse, PageResponse, UserSummary } from './types';

export function queryUsers(params: { current?: number; pageSize?: number; keyword?: string; status?: number }) {
  return unwrap(
    request<ApiResponse<PageResponse<UserSummary>>>('/api/users', {
      method: 'GET',
      params: {
        pageNum: params.current,
        pageSize: params.pageSize,
        keyword: params.keyword,
        status: params.status,
      },
    }),
  );
}
