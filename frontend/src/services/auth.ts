import { request } from '@umijs/max';
import { unwrap } from './common';
import type { ApiResponse, AuthTokenResponse, CurrentUser } from './types';

export type LoginParams = {
  username: string;
  password: string;
};

export function login(params: LoginParams) {
  return unwrap(
    request<ApiResponse<AuthTokenResponse>>('/api/auth/login', {
      method: 'POST',
      data: params,
      skipErrorHandler: true,
    }),
  );
}

export function getMe() {
  return unwrap(request<ApiResponse<CurrentUser>>('/api/auth/me'));
}
