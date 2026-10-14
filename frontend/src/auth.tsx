import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { api, clearToken, getToken, setToken, type User } from './api';

interface AuthState {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState<boolean>(!!getToken());

  useEffect(() => {
    if (!getToken()) return;
    api.me().then(u => setUser(u)).catch(() => clearToken()).finally(() => setLoading(false));
  }, []);

  const finish = (token: string, u: User) => {
    setToken(token);
    setUser(u);
  };

  const value: AuthState = {
    user,
    loading,
    login: async (email, password) => {
      const res = await api.login(email, password);
      finish(res.token, res.user);
    },
    register: async (email, password) => {
      const res = await api.register(email, password);
      finish(res.token, res.user);
    },
    logout: () => { clearToken(); setUser(null); },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth outside AuthProvider');
  return ctx;
}
