import React from 'react';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { LoginForm, ProFormText } from '@ant-design/pro-components';
import { history, useModel, useSearchParams } from '@umijs/max';
import { Alert, message } from 'antd';
import { login } from '@/services/auth';
import { useAuthStore } from '@/stores/auth';

const LoginPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const { initialState, setInitialState } = useModel('@@initialState');
  const [messageApi, contextHolder] = message.useMessage();

  return (
    <div style={{ minHeight: '100vh', background: '#f5f7fa', paddingTop: 80 }}>
      {contextHolder}
      <LoginForm
        title="二手商城后台"
        subTitle="React + Umi Max + Ant Design Pro 精简后台"
        submitter={{ searchConfig: { submitText: '登录' } }}
        onFinish={async (values) => {
          try {
            const result = await login({ username: values.username, password: values.password });
            if (result.currentUser.role !== 'ADMIN') {
              useAuthStore.getState().clearAuth();
              messageApi.error('当前账号不是管理员，无法进入后台');
              return false;
            }
            useAuthStore.getState().setAuth({ token: result.accessToken, currentUser: result.currentUser });
            await setInitialState({
              ...(initialState || { fetchCurrentUser: async () => result.currentUser }),
              currentUser: result.currentUser,
            });
            messageApi.success('登录成功');
            history.push(searchParams.get('redirect') || '/dashboard');
            return true;
          } catch (error: any) {
            messageApi.error(error?.data?.message || error?.message || '登录失败');
            return false;
          }
        }}
      >
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 24 }}
          message="请使用管理员账号登录；示例账号见 frontend/README.md。"
        />
        <ProFormText
          name="username"
          fieldProps={{ size: 'large', prefix: <UserOutlined /> }}
          placeholder="用户名"
          rules={[{ required: true, message: '请输入用户名' }]}
        />
        <ProFormText.Password
          name="password"
          fieldProps={{ size: 'large', prefix: <LockOutlined /> }}
          placeholder="密码"
          rules={[{ required: true, message: '请输入密码' }]}
        />
      </LoginForm>
    </div>
  );
};

export default LoginPage;
