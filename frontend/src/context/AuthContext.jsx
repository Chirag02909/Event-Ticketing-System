import React, { createContext, useState, useEffect, useContext } from 'react';
import api from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  // Helper to decode JWT token without external libraries
  const decodeToken = (jwtToken) => {
    try {
      const base64Url = jwtToken.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      const decoded = JSON.parse(jsonPayload);
      return {
        email: decoded.sub,
        role: decoded.role, // e.g., 'USER', 'ORGANISER', 'ADMIN'
        exp: decoded.exp,
      };
    } catch (e) {
      console.error('Failed to decode token:', e);
      return null;
    }
  };

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    if (storedToken) {
      const decoded = decodeToken(storedToken);
      if (decoded && decoded.exp * 1000 > Date.now()) {
        setToken(storedToken);
        setUser({ email: decoded.email, role: decoded.role });
      } else {
        // Token expired or invalid
        localStorage.removeItem('token');
      }
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    try {
      const response = await api.post('/auth/login', { email, password });
      // The login response contains { status, message, token, role }
      if (response.data && response.data.token) {
        const jwtToken = response.data.token;
        localStorage.setItem('token', jwtToken);
        const decoded = decodeToken(jwtToken);
        const role = response.data.role || (decoded ? decoded.role : 'USER');
        const emailVal = decoded ? decoded.email : email;
        
        setToken(jwtToken);
        setUser({ email: emailVal, role });
        return { success: true };
      }
      return { success: false, message: response.data?.message || 'Login failed' };
    } catch (error) {
      console.error('Login error:', error);
      const msg = error.response?.data?.message || 'Invalid email or password';
      return { success: false, message: msg };
    }
  };

  const signup = async (username, email, password, otp, role) => {
    try {
      const response = await api.post('/auth/signup', {
        username,
        email,
        password,
        otp,
        role,
      });
      if (response.data && response.data.status) {
        return { success: true, message: response.data.message };
      }
      return { success: false, message: response.data?.message || 'Signup failed' };
    } catch (error) {
      console.error('Signup error:', error);
      const msg = error.response?.data?.message || 'Signup failed. Please try again.';
      return { success: false, message: msg };
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!user,
        loading,
        login,
        signup,
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
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
