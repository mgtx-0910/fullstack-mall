import React from 'react';
import type { RequestConfig, RunTimeLayoutConfig } from '@umijs/max';
import { history } from '@umijs/max';
import { Dropdown, message } from 'antd';
import type { MenuProps } from 'antd';
import defaultSettings from '../config/defaultSettings';
import { getMe } from '@/services/auth';
import type { ApiResponse, CurrentUser } from '@/services/types';
import { useAuthStore } from '@/stores/auth';

const loginPath = '/user/login';

export type InitialState = {
  currentUser?: CurrentUser;
  fetchCurrentUser: () => Promise<CurrentUser | undefined>;
};

const fallbackFetchCurrentUser = async () => undefined;

function createFallbackState(): InitialState {
  return {
    currentUser: undefined,
    fetchCurrentUser: fallbackFetchCurrentUser,
  };
}

export async function getInitialState(): Promise<InitialState> {
  const fetchCurrentUser = async () => {
    const token = useAuthStore.getState().token;
    if (!token) {
      return undefined;
    }
    try {
      const currentUser = await getMe();
      if (currentUser.role !== 'ADMIN') {
        useAuthStore.getState().clearAuth();
        message.error('当前账号不是管理员，无法进入后台');
        return undefined;
      }
      useAuthStore.getState().setCurrentUser(currentUser);
      return currentUser;
    } catch {
      useAuthStore.getState().clearAuth();
      return undefined;
    }
  };

  const currentUser = await fetchCurrentUser();
  return { currentUser, fetchCurrentUser };
}

export const layout: RunTimeLayoutConfig = ({ initialState, setInitialState }) => {
  const logout = () => {
    useAuthStore.getState().clearAuth();
    setInitialState((prev) => ({ ...(prev || createFallbackState()), currentUser: undefined }));
    message.success('已退出登录');
    history.push(loginPath);
  };

  const menuItems: MenuProps['items'] = [
    {
      key: 'logout',
      label: '退出登录',
      onClick: logout,
    },
  ];

  return {
    ...defaultSettings,
    menuHeaderRender: undefined,
    avatarProps: {
      title: initialState?.currentUser?.nickname || initialState?.currentUser?.username || '管理员',
      render: (_, dom) => <Dropdown menu={{ items: menuItems }}>{dom}</Dropdown>,
    },
    onPageChange: () => {
      const { location } = history;
      const token = useAuthStore.getState().token;
      if (location.pathname === loginPath) {
        return;
      }
      if (!token) {
        history.push(loginPath);
        return;
      }
      if (initialState?.currentUser && initialState.currentUser.role !== 'ADMIN') {
        useAuthStore.getState().clearAuth();
        history.push(loginPath);
      }
    },
  };
};

export const request: RequestConfig = {
  timeout: 10000,
  errorConfig: {
    errorHandler(error: any, opts: any) {
      if (opts?.skipErrorHandler) {
        throw error;
      }
      const status = error?.response?.status;
      if (status === 401) {
        useAuthStore.getState().clearAuth();
        message.error('登录已失效，请重新登录');
        history.push(loginPath);
        return;
      }
      if (status === 403) {
        message.error('无权限访问该资源');
        return;
      }
      const data = error?.data;
      const traceText = data?.traceId ? `（traceId：${data.traceId}）` : '';
      message.error(data?.message ? `${data.message}${traceText}` : error?.message || '请求失败');
    },
  },
  requestInterceptors: [
    (url, options: any) => {
      const token = useAuthStore.getState().token;
      const headers = {
        ...(options?.headers || {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      };
      return { url, options: { ...options, headers } };
    },
  ],
  responseInterceptors: [
    async (response: any) => {
      const payload = response?.data as ApiResponse<unknown> | undefined;
      if (payload?.code && payload.code !== 'SUCCESS') {
        const error = new Error(payload.message || '请求失败') as Error & { data?: any; response?: any };
        error.data = payload;
        error.response = response;
        throw error;
      }
      return response;
    },
  ],
};
