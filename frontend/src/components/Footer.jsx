import React from 'react';

export const Footer = () => {
  return (
    <footer style={{
      borderTop: '1px solid var(--border-main)',
      padding: '24px 0',
      marginTop: '48px',
      background: 'rgba(10, 15, 26, 0.4)',
      backdropFilter: 'blur(8px)'
    }}>
      <div style={{
        maxWidth: '1280px',
        margin: '0 auto',
        padding: '0 20px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '16px'
      }}>
        <div>
          <span style={{
            fontFamily: 'var(--font-heading)',
            fontWeight: 800,
            letterSpacing: '-0.03em',
            fontSize: '1.2rem',
            background: 'linear-gradient(to right, var(--primary), var(--accent))',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}>
            EventPass
          </span>
          <p style={{
            fontSize: '0.8rem',
            color: 'var(--text-dark)',
            marginTop: '4px'
          }}>
            Real-Time Ticket Booking Platform
          </p>
        </div>
        
        <div style={{
          fontSize: '0.85rem',
          color: 'var(--text-muted)'
        }}>
          &copy; {new Date().getFullYear()} EventPass. All rights reserved.
        </div>
      </div>
    </footer>
  );
};

export default Footer;
