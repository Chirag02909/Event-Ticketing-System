import React, { useState } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import Button from '../components/Button';
import { Mail, Lock, LogIn, AlertCircle, Key, ArrowLeft, CheckCircle } from 'lucide-react';

export const Login = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  
  // Forgot Password States
  const [showForgotPassword, setShowForgotPassword] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotOtp, setForgotOtp] = useState('');
  const [forgotNewPassword, setForgotNewPassword] = useState('');
  const [forgotStep, setForgotStep] = useState(1); // 1: Send OTP, 2: Reset Password
  const [forgotMsg, setForgotMsg] = useState('');

  // Status States
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  const isExpired = searchParams.get('expired') === 'true';

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please fill in all fields.');
      return;
    }

    setError('');
    setLoading(true);
    
    const result = await login(email, password);
    setLoading(false);
    
    if (result.success) {
      const token = localStorage.getItem('token');
      const storedRole = token ? JSON.parse(atob(token.split('.')[1])).role : 'USER';
      
      if (storedRole === 'ADMIN') {
        navigate('/admin');
      } else if (storedRole === 'ORGANISER') {
        navigate('/organiser');
      } else {
        navigate('/');
      }
    } else {
      setError(result.message);
    }
  };

  const handleSendResetOtp = async (e) => {
    e.preventDefault();
    if (!forgotEmail) {
      setError('Email is required.');
      return;
    }

    setError('');
    setLoading(true);
    try {
      const res = await api.post('/auth/forgot-password/send-otp', { email: forgotEmail });
      setLoading(false);
      if (res.data && res.data.status) {
        setForgotMsg(res.data.message);
        setForgotStep(2);
      } else {
        setError(res.data.message || 'Failed to send reset OTP.');
      }
    } catch (err) {
      setLoading(false);
      setError(err.response?.data?.message || 'Error sending reset OTP. Is the email registered?');
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (!forgotOtp || !forgotNewPassword) {
      setError('Please fill in all fields.');
      return;
    }

    setError('');
    setLoading(true);
    try {
      const res = await api.post('/auth/forgot-password/reset', {
        email: forgotEmail,
        otp: forgotOtp,
        password: forgotNewPassword
      });
      setLoading(false);
      if (res.data && res.data.status) {
        alert('Password reset successfully! You can now log in.');
        setShowForgotPassword(false);
        setForgotStep(1);
        setForgotEmail('');
        setForgotOtp('');
        setForgotNewPassword('');
        setForgotMsg('');
      } else {
        setError(res.data.message || 'Failed to reset password.');
      }
    } catch (err) {
      setLoading(false);
      setError(err.response?.data?.message || 'Error resetting password. Check your OTP.');
    }
  };

  if (showForgotPassword) {
    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: 'calc(100vh - 150px)',
        padding: '20px'
      }}>
        <div className="glass-card" style={{ maxWidth: '420px', width: '100%' }}>
          <button
            onClick={() => {
              setShowForgotPassword(false);
              setForgotStep(1);
              setError('');
              setForgotMsg('');
            }}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              background: 'none',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              marginBottom: '20px',
              fontSize: '0.9rem'
            }}
          >
            <ArrowLeft size={16} />
            Back to Login
          </button>

          <div style={{ textAlign: 'center', marginBottom: '28px' }}>
            <Key size={40} className="text-primary" style={{ marginBottom: '12px' }} />
            <h2>Reset Password</h2>
            <p className="text-muted" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
              {forgotStep === 1 
                ? 'Enter your email to receive a password reset OTP'
                : `Enter the OTP sent to ${forgotEmail} and your new password`}
            </p>
          </div>

          {error && (
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              background: 'rgba(239, 68, 68, 0.12)',
              border: '1px solid var(--color-booked)',
              color: 'var(--color-booked)',
              borderRadius: '6px',
              padding: '12px',
              fontSize: '0.85rem',
              marginBottom: '20px'
            }}>
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          {forgotMsg && forgotStep === 2 && (
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              background: 'rgba(16, 185, 129, 0.12)',
              border: '1px solid var(--color-available)',
              color: 'var(--color-available)',
              borderRadius: '6px',
              padding: '12px',
              fontSize: '0.85rem',
              marginBottom: '20px'
            }}>
              <CheckCircle size={16} />
              <span>{forgotMsg}</span>
            </div>
          )}

          {forgotStep === 1 ? (
            <form onSubmit={handleSendResetOtp}>
              <div className="form-group" style={{ marginBottom: '24px' }}>
                <label className="form-label">Email Address</label>
                <div style={{ position: 'relative' }}>
                  <Mail size={18} style={{
                    position: 'absolute',
                    left: '14px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    color: 'var(--text-dark)'
                  }} />
                  <input
                    type="email"
                    className="form-input"
                    style={{ paddingLeft: '45px' }}
                    placeholder="you@example.com"
                    value={forgotEmail}
                    onChange={(e) => setForgotEmail(e.target.value)}
                    disabled={loading}
                    required
                  />
                </div>
              </div>

              <Button
                type="submit"
                className="w-full"
                loading={loading}
                style={{ width: '100%', padding: '14px' }}
              >
                Send Reset OTP
              </Button>
            </form>
          ) : (
            <form onSubmit={handleResetPassword}>
              <div className="form-group">
                <label className="form-label">Reset OTP</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="6-digit code"
                  maxLength={6}
                  value={forgotOtp}
                  onChange={(e) => setForgotOtp(e.target.value.replace(/\D/g, ''))}
                  required
                />
              </div>

              <div className="form-group" style={{ marginBottom: '28px' }}>
                <label className="form-label">New Password</label>
                <input
                  type="password"
                  className="form-input"
                  placeholder="6 to 16 characters"
                  value={forgotNewPassword}
                  onChange={(e) => setForgotNewPassword(e.target.value)}
                  required
                />
              </div>

              <Button
                type="submit"
                className="w-full"
                loading={loading}
                style={{ width: '100%', padding: '14px' }}
              >
                Reset Password
              </Button>
            </form>
          )}
        </div>
      </div>
    );
  }

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: 'calc(100vh - 150px)',
      padding: '20px'
    }}>
      <div className="glass-card" style={{ maxWidth: '420px', width: '100%' }}>
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <LogIn size={40} className="text-primary" style={{ marginBottom: '12px' }} />
          <h2>Welcome Back</h2>
          <p className="text-muted" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
            Sign in to book tickets and manage events
          </p>
        </div>

        {isExpired && (
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            background: 'rgba(245, 158, 11, 0.12)',
            border: '1px solid var(--color-held)',
            color: 'var(--color-held)',
            borderRadius: '6px',
            padding: '12px',
            fontSize: '0.85rem',
            marginBottom: '20px'
          }}>
            <AlertCircle size={16} />
            <span>Your session has expired. Please sign in again.</span>
          </div>
        )}

        {error && (
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            background: 'rgba(239, 68, 68, 0.12)',
            border: '1px solid var(--color-booked)',
            color: 'var(--color-booked)',
            borderRadius: '6px',
            padding: '12px',
            fontSize: '0.85rem',
            marginBottom: '20px'
          }}>
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Email Address</label>
            <div style={{ position: 'relative' }}>
              <Mail size={18} style={{
                position: 'absolute',
                left: '14px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--text-dark)'
              }} />
              <input
                type="email"
                className="form-input"
                style={{ paddingLeft: '45px' }}
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={loading}
                required
              />
            </div>
          </div>

          <div className="form-group" style={{ marginBottom: '12px' }}>
            <label className="form-label">Password</label>
            <div style={{ position: 'relative' }}>
              <Lock size={18} style={{
                position: 'absolute',
                left: '14px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--text-dark)'
              }} />
              <input
                type="password"
                className="form-input"
                style={{ paddingLeft: '45px' }}
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
                required
              />
            </div>
          </div>

          <div style={{ textAlign: 'right', marginBottom: '24px' }}>
            <button
              type="button"
              onClick={() => {
                setShowForgotPassword(true);
                setError('');
              }}
              style={{
                background: 'none',
                border: 'none',
                color: 'var(--primary)',
                fontSize: '0.85rem',
                cursor: 'pointer',
                fontWeight: 500
              }}
            >
              Forgot Password?
            </button>
          </div>

          <Button
            type="submit"
            className="w-full"
            loading={loading}
            style={{ width: '100%', padding: '14px' }}
          >
            Sign In
          </Button>
        </form>

        <div style={{
          textAlign: 'center',
          marginTop: '24px',
          fontSize: '0.9rem',
          color: 'var(--text-muted)'
        }}>
          Don't have an account?{' '}
          <Link to="/signup" className="text-primary" style={{ fontWeight: 600 }}>
            Create one
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Login;
