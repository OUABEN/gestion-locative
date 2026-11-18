import React, { createContext, useContext, useState } from 'react';
import { decodeToken } from '../utils/jwt';

const AuthContext = createContext(null);

const getInitialSession = () => {
  const stored = sessionStorage.getItem('token') || localStorage.getItem('token');
  if (!stored) return { token: null, user: null };
  const decoded = decodeToken(stored);
  if (decoded && !decoded.isExpired) {
    sessionStorage.setItem('token', stored);
    localStorage.setItem('token', stored);
    return { token: stored, user: decoded };
  }
  sessionStorage.removeItem('token');
  localStorage.removeItem('token');
  return { token: null, user: null };
};

export const AuthProvider = ({ children }) => {
  const initial = getInitialSession();
  const [token, setToken] = useState(initial.token);
  const [user, setUser] = useState(initial.user);
  const [loading] = useState(false);

  const loginWithToken = (rawToken) => {
    const cleanToken = (rawToken || '').trim();
    const decoded = decodeToken(cleanToken);
    if (!decoded) {
      throw new Error('Jeton JWT invalide ou illisible.');
    }
    if (decoded.isExpired) {
      throw new Error('Ce jeton JWT a expiré.');
    }
    // Sauvegarde synchrone immédiate avant toute navigation React Router
    sessionStorage.setItem('token', cleanToken);
    localStorage.setItem('token', cleanToken);
    setToken(cleanToken);
    setUser(decoded);
    return decoded;
  };

  const logout = () => {
    sessionStorage.removeItem('token');
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  const canWrite = user?.role === 'BAILLEUR' || user?.role === 'GESTIONNAIRE';

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        loading,
        canWrite,
        loginWithToken,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth doit être utilisé dans un AuthProvider');
  }
  return context;
};
