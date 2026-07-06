import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import Button from '../components/Button';
import { Mail, Lock, User, Key, CheckCircle, AlertCircle, ArrowRight, UserPlus } from 'lucide-react';

export const Signup = () => {
  const { signup } = useAuth();
  const navigate = useNavigate();

  const [step, setStep] = useState(1); // 1: Email, 2: OTP, 3: Details
  
  // Form States
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('USER'); // USER or ORGANISER (ADMIN is blocked)

  // Status States
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  // Step 1: Send OTP
  const handleSendOtp = async (e) => {
    e.preventDefault();
    if (!email) {
      setError('Email is required.');
      return;
    }

    setError('');
    setLoading(true);
    try {
      const response = await api.post('/api/auth/sendOtp', { email });
      setLoading(false);
      if (response.data && response.data.status) {
        setMessage(response.data.message);
        setStep(2); // Move to OTP entry
      } else {
        setError(response.data.message || 'Failed to send OTP.');
      }
    } catch (err) {
      setLoading(false);
      setError(err.response?.data?.message || 'Error sending OTP. Is the email already registered?');
    }
  };

  // Step 2: Proceed from OTP
  const handleVerifyOtpLocal = (e) => {
    e.preventDefault();
    if (!otp || otp.length !== 6) {
      setError('Please enter a valid 6-digit OTP.');
      return;
    }
    setError('');
    setStep(3); // Move to Details entry
  };

  // Step 3: Complete Signup
  const handleCompleteSignup = async (e) => {
    e.preventDefault();
    if (!username || !password || !role) {
      setError('Please fill in all fields.');
      return;
    }
    if (username.length < 3) {
      setError('Username must be at least 3 characters.');
      return;
    }
    if (password.length < 6 || password.length > 16) {
      setError('Password must be between 6 and 16 characters.');
      return;
    }

    setError('');
    setLoading(true);

    const result = await signup(username, email, password, otp, role);
    setLoading(false);

    if (result.success) {
      setStep(4); // Success screen
      setTimeout(() => {
        navigate('/login');
      }, 3000);
    } else {
      setError(result.message);
      // If OTP was invalid/expired, allow them to go back to step 2 or 1
      if (result.message.toLowerCase().includes('otp')) {
        setStep(2);
      }
    }
  };

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: 'calc(100vh - 150px)',
      padding: '20px'
    }}>
      <div className="glass-card" style={{ maxWidth: '460px', width: '100%' }}>
        
        {/* Step Progress Indicator */}
        {step <= 3 && (
          <div className="step-indicator">
            <div className={`step-node ${step >= 1 ? 'active' : ''} ${step > 1 ? 'completed' : ''}`}>1</div>
            <div className={`step-node ${step >= 2 ? 'active' : ''} ${step > 2 ? 'completed' : ''}`}>2</div>
            <div className={`step-node ${step >= 3 ? 'active' : ''} ${step > 3 ? 'completed' : ''}`}>3</div>
          </div>
        )}

        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          {step === 4 ? (
            <CheckCircle size={48} className="text-success" style={{ marginBottom: '12px' }} />
          ) : (
            <UserPlus size={40} className="text-primary" style={{ marginBottom: '12px' }} />
          )}
          
          {step === 1 && (
            <>
              <h2>Create Account</h2>
              <p className="text-muted" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
                Enter your email to verify and get started
              </p>
            </>
          )}
          {step === 2 && (
            <>
              <h2>Enter Verification Code</h2>
              <p className="text-muted" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
                We sent a 6-digit OTP to <strong style={{ color: 'var(--text-main)' }}>{email}</strong>
              </p>
            </>
          )}
          {step === 3 && (
            <>
              <h2>Complete Profile</h2>
              <p className="text-muted" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
                Choose a username, password, and account role
              </p>
            </>
          )}
          {step === 4 && (
            <>
              <h2>Registration Successful!</h2>
              <p className="text-muted" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
                Redirecting you to the login page...
              </p>
            </>
          )}
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

        {message && step === 2 && (
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
            <span>{message}</span>
          </div>
        )}

        {/* STEP 1: EMAIL */}
        {step === 1 && (
          <form onSubmit={handleSendOtp}>
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
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
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
              Get OTP <ArrowRight size={16} />
            </Button>
          </form>
        )}

        {/* STEP 2: OTP ENTRY */}
        {step === 2 && (
          <form onSubmit={handleVerifyOtpLocal}>
            <div className="form-group" style={{ marginBottom: '24px' }}>
              <label className="form-label">Verification Code (OTP)</label>
              <div style={{ position: 'relative' }}>
                <Key size={18} style={{
                  position: 'absolute',
                  left: '14px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  color: 'var(--text-dark)'
                }} />
                <input
                  type="text"
                  className="form-input"
                  style={{ paddingLeft: '45px', letterSpacing: '0.3em', textAlign: 'center', fontWeight: 'bold' }}
                  placeholder="123456"
                  maxLength={6}
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                  required
                />
              </div>
            </div>

            <div style={{ display: 'flex', gap: '12px' }}>
              <Button
                type="button"
                variant="secondary"
                style={{ flex: 1, padding: '14px' }}
                onClick={() => setStep(1)}
              >
                Back
              </Button>
              <Button
                type="submit"
                style={{ flex: 2, padding: '14px' }}
              >
                Verify OTP <ArrowRight size={16} />
              </Button>
            </div>
          </form>
        )}

        {/* STEP 3: DETAILS */}
        {step === 3 && (
          <form onSubmit={handleCompleteSignup}>
            <div className="form-group">
              <label className="form-label">Username</label>
              <div style={{ position: 'relative' }}>
                <User size={18} style={{
                  position: 'absolute',
                  left: '14px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  color: 'var(--text-dark)'
                }} />
                <input
                  type="text"
                  className="form-input"
                  style={{ paddingLeft: '45px' }}
                  placeholder="john_doe"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  disabled={loading}
                  required
                />
              </div>
            </div>

            <div className="form-group">
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
                  placeholder="Minimum 6 characters"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={loading}
                  required
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '28px' }}>
              <label className="form-label">Account Role</label>
              <select
                className="form-select"
                value={role}
                onChange={(e) => setRole(e.target.value)}
                disabled={loading}
              >
                <option value="USER">Customer (Book Tickets)</option>
                <option value="ORGANISER">Organiser (Host Events)</option>
              </select>
            </div>

            <div style={{ display: 'flex', gap: '12px' }}>
              <Button
                type="button"
                variant="secondary"
                style={{ flex: 1, padding: '14px' }}
                onClick={() => setStep(2)}
                disabled={loading}
              >
                Back
              </Button>
              <Button
                type="submit"
                loading={loading}
                style={{ flex: 2, padding: '14px' }}
              >
                Register
              </Button>
            </div>
          </form>
        )}

        {step === 4 && (
          <div style={{ textAlign: 'center', padding: '20px', color: 'var(--text-muted)' }}>
            Your account is ready! Opening sign-in screen...
          </div>
        )}

        {step <= 3 && (
          <div style={{
            textAlign: 'center',
            marginTop: '24px',
            fontSize: '0.9rem',
            color: 'var(--text-muted)'
          }}>
            Already have an account?{' '}
            <Link to="/login" className="text-primary" style={{ fontWeight: 600 }}>
              Sign in
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};

export default Signup;
