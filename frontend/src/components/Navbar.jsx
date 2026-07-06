import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LogOut, User, Ticket, Calendar, Shield, Cpu } from 'lucide-react';

export const Navbar = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getRoleBadge = (role) => {
    switch (role) {
      case 'ADMIN':
        return <span className="badge badge-primary">Admin</span>;
      case 'ORGANISER':
        return <span className="badge badge-accent">Organiser</span>;
      default:
        return <span className="badge badge-success">User</span>;
    }
  };

  return (
    <nav style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      height: '70px',
      background: 'rgba(10, 15, 26, 0.75)',
      backdropFilter: 'blur(12px)',
      borderBottom: '1px solid var(--border-main)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 24px',
      zIndex: 50,
      transition: 'var(--transition-normal)'
    }}>
      {/* Brand Logo */}
      <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <Cpu size={24} className="text-primary" />
        <span style={{
          fontFamily: 'var(--font-heading)',
          fontSize: '1.4rem',
          fontWeight: 800,
          letterSpacing: '-0.04em',
          background: 'linear-gradient(to right, var(--primary), var(--accent))',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent'
        }}>
          EventPass
        </span>
      </Link>

      {/* Nav Links */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
        <Link to="/" style={{ fontSize: '0.95rem', fontWeight: 500, color: 'var(--text-muted)' }} className="nav-link">
          Explore
        </Link>

        {isAuthenticated && (
          <>
            {user.role === 'USER' && (
              <Link to="/my-bookings" style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.95rem', fontWeight: 500 }} className="nav-link">
                <Ticket size={16} />
                My Bookings
              </Link>
            )}
            
            {user.role === 'ORGANISER' && (
              <Link to="/organiser" style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.95rem', fontWeight: 500 }} className="nav-link">
                <Calendar size={16} />
                Organiser Hub
              </Link>
            )}

            {user.role === 'ADMIN' && (
              <Link to="/admin" style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.95rem', fontWeight: 500 }} className="nav-link">
                <Shield size={16} />
                Admin Panel
              </Link>
            )}
          </>
        )}
      </div>

      {/* User Actions / Auth Buttons */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {isAuthenticated ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            {/* Profile Summary */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              padding: '6px 12px',
              background: 'rgba(255, 255, 255, 0.03)',
              borderRadius: '20px',
              border: '1px solid var(--border-main)'
            }}>
              <div style={{
                width: '24px',
                height: '24px',
                borderRadius: '50%',
                background: 'var(--bg-main)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--text-muted)'
              }}>
                <User size={14} />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 600, maxWidth: '120px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {user.email}
                </span>
              </div>
              {getRoleBadge(user.role)}
            </div>

            {/* Logout Button */}
            <button
              onClick={handleLogout}
              className="btn btn-secondary"
              style={{
                padding: '8px 12px',
                fontSize: '0.85rem',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                borderRadius: '20px'
              }}
            >
              <LogOut size={14} />
              Logout
            </button>
          </div>
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Link to="/login" className="btn btn-secondary" style={{ padding: '8px 18px', borderRadius: '20px', fontSize: '0.9rem' }}>
              Sign In
            </Link>
            <Link to="/signup" className="btn btn-primary" style={{ padding: '8px 18px', borderRadius: '20px', fontSize: '0.9rem' }}>
              Sign Up
            </Link>
          </div>
        )}
      </div>

      {/* Inline styles for navbar link hover effects */}
      <style>{`
        .nav-link {
          color: var(--text-muted);
          transition: var(--transition-fast);
        }
        .nav-link:hover {
          color: var(--text-main);
        }
      `}</style>
    </nav>
  );
};

export default Navbar;
