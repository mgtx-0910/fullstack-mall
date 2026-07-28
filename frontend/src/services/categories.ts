import { request } from '@umijs/max';
import { unwrap } from './common';
import type { ApiResponse, CategoryDraftResponse, CategorySummary } from './types';

export function listCategories() {
  return unwrap(request<ApiResponse<CategorySummary[]>>('/api/admin/categories'));
}

export function validateCategoryDraft(data: { name: string; description?: string; sortOrder: number }) {
  return unwrap(
    request<ApiResponse<CategoryDraftResponse>>('/api/categories/validate', {
      method: 'POST',
      data,
    }),
  );
}
