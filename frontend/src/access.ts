import type { InitialState } from './app';

export default function access(initialState: InitialState) {
  const currentUser = initialState?.currentUser;
  return {
    isAdmin: currentUser?.role === 'ADMIN',
  };
}
