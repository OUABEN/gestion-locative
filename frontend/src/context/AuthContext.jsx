import React, { createContext, useContext, useEffect, useState } from 'react';
import { decodeToken } from '../utils/jwt';

const AuthContext = createContext(null);
const SESSION_REJECTED_EVENT = 'auth:session-rejected';

const removeStoredToken = () => {
  sessionStorage.removeItem('token');
  localStorage.removeItem('token');
};

const getInitialSession = () => {
  const stored = sessionStorage.getItem('token') || localStorage.getItem('token');
  if (!stored) return { token: null, user: null, sessionNotice: '' };

  const decoded = decodeToken(stored);
  if (decoded && !decoded.isExpired) {
    sessionStorage.setItem('token', stored);
    localStorage.setItem('token', stored);
    return { token: stored, user: decoded, sessionNotice: '' };
  }

  removeStoredToken();
  return {
    token: null,
    user: null,
    sessionNotice: decoded?.isExpired
      ? 'Votre session a expiré. Veuillez vous reconnecter.'
      : 'Votre session est illisible ou incomplète. Veuillez vous reconnecter.',
  };
};

export const AuthProvider = ({ children }) => {
  const [initial] = useState(getInitialSession);
  const [token, setToken] = useState(initial.token);
  const [user, setUser] = useState(initial.user);
  const [sessionNotice, setSessionNotice] = useState(initial.sessionNotice);
  const [loading] = useState(false);

  useEffect(() => {
    const handleRejectedSession = () => {
      removeStoredToken();
      setToken(null);
      setUser(null);
      setSessionNotice('Votre session a été refusée. Veuillez vous reconnecter.');
    };

    window.addEventListener(SESSION_REJECTED_EVENT, handleRejectedSession);
    return () => {
      window.removeEventListener(SESSION_REJECTED_EVENT, handleRejectedSession);
    };
  }, []);

  const loginWithToken = (rawToken) => {
    const decoded = decodeToken(rawToken);
    if (!decoded) {
      throw new Error('Le jeton reçu du serveur est illisible ou incomplet.');
    }
    if (decoded.isExpired) {
      throw new Error('Le jeton reçu du serveur a expiré.');
    }

    sessionStorage.setItem('token', rawToken);
    localStorage.setItem('token', rawToken);
    setToken(rawToken);
    setUser(decoded);
    setSessionNotice('');
    return decoded;
  };

  const logout = () => {
    removeStoredToken();
    setToken(null);
    setUser(null);
    setSessionNotice('');
  };

  const canWrite = user?.role === 'BAILLEUR' || user?.role === 'GESTIONNAIRE';

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        loading,
        canWrite,
        sessionNotice,
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
