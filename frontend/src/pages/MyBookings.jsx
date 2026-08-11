import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import Button from '../components/Button';
import { Ticket, Calendar, Clock, RotateCcw, HelpCircle, XCircle, CheckCircle, RefreshCw, LogOut, Info } from 'lucide-react';

export const MyBookings = () => {
  const navigate = useNavigate();
  
  const [bookings, setBookings] = useState([]);
  const [waitlist, setWaitlist] = useState([]);
  
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('bookings'); // 'bookings' or 'waitlist'
  const [error, setError] = useState('');
  
  // Refund Modal States
  const [showRefundModal, setShowRefundModal] = useState(false);
  const [selectedBookingId, setSelectedBookingId] = useState(null);
  const [refundReason, setRefundReason] = useState('');
  const [submittingRefund, setSubmittingRefund] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const bookingsRes = await api.get('/bookings/my');
      const sortedBookings = (bookingsRes.data || []).sort((a, b) => b.id - a.id);
      setBookings(sortedBookings);

      const waitlistRes = await api.get('/waitlist/my');
      const sortedWaitlist = (waitlistRes.data || []).sort((a, b) => b.entryId - a.entryId);
      setWaitlist(sortedWaitlist);
    } catch (err) {
      console.error('Error fetching dashboard data:', err);
      setError('Could not load bookings. Please sign in again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCancelHold = async (bookingId) => {
    if (window.confirm('Are you sure you want to cancel this hold and release the seats?')) {
      try {
        await api.delete(`/bookings/${bookingId}/cancel`);
        fetchData(); // Refresh list
      } catch (err) {
        console.error('Error cancelling hold:', err);
        alert('Failed to cancel hold. It may have already expired.');
      }
    }
  };

  const handleOpenRefundModal = (bookingId) => {
    setSelectedBookingId(bookingId);
    setRefundReason('');
    setShowRefundModal(true);
  };

  const handleRequestRefund = async (e) => {
    e.preventDefault();
    if (!selectedBookingId) return;

    setSubmittingRefund(true);
    try {
      const res = await api.post(`/bookings/${selectedBookingId}/refund`, {
        reason: refundReason,
      });
      if (res.data && res.data.success) {
        alert('Refund request submitted successfully.');
        setShowRefundModal(false);
        fetchData(); // Refresh list
      } else {
        alert(res.data.message || 'Failed to submit refund.');
      }
    } catch (err) {
      console.error('Error requesting refund:', err);
      alert(err.response?.data?.message || 'Failed to submit refund.');
    } finally {
      setSubmittingRefund(false);
    }
  };

  const handleLeaveWaitlist = async (entryId) => {
    if (window.confirm('Are you sure you want to leave this waitlist?')) {
      try {
        await api.delete(`/waitlist/${entryId}`);
        fetchData(); // Refresh list
      } catch (err) {
        console.error('Error leaving waitlist:', err);
        alert('Failed to leave waitlist.');
      }
    }
  };

  const handleAcceptOffer = async (entryId) => {
    try {
      const res = await api.post(`/waitlist/${entryId}/accept`);
      // Res contains BookingResponseDTO
      if (res.data && res.data.id) {
        alert('Offer accepted! Redirecting to payment checkout...');
        navigate(`/checkout/${res.data.id}`);
      }
    } catch (err) {
      console.error('Error accepting waitlist offer:', err);
      alert(err.response?.data?.message || 'Failed to claim seat. The offer window may have expired.');
      fetchData(); // Refresh list
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'CONFIRMED':
        return <span className="badge badge-success">Confirmed</span>;
      case 'PENDING_PAYMENT':
        return <span className="badge badge-warning">Held (Unpaid)</span>;
      case 'CANCELLED':
        return <span className="badge badge-secondary">Cancelled</span>;
      case 'PAYMENT_FAILED':
        return <span className="badge badge-danger">Failed</span>;
      default:
        return <span className="badge badge-secondary">{status}</span>;
    }
  };

  const getWaitlistStatusBadge = (status) => {
    switch (status) {
      case 'WAITING':
        return <span className="badge badge-accent">In Queue</span>;
      case 'OFFERED':
        return <span className="badge badge-warning">Seat Offered</span>;
      case 'CLAIMED':
        return <span className="badge badge-success">Claimed</span>;
      case 'EXPIRED':
        return <span className="badge badge-secondary">Expired</span>;
      case 'CANCELLED':
        return <span className="badge badge-secondary">Left</span>;
      default:
        return <span className="badge badge-secondary">{status}</span>;
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="flex-center" style={{ minHeight: '400px' }}>
        <div className="animate-spin" style={{
          border: '3px solid var(--border-main)',
          borderTop: '3px solid var(--primary)',
          borderRadius: '50%',
          width: '32px',
          height: '32px',
          animation: 'spin 1s linear infinite'
        }}></div>
      </div>
    );
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
        <h2>My Dashboard</h2>
        <button onClick={fetchData} className="btn btn-secondary" style={{ padding: '8px 12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
          <RefreshCw size={14} />
          Refresh
        </button>
      </div>

      {/* Tabs */}
      <div style={{
        display: 'flex',
        gap: '8px',
        borderBottom: '1px solid var(--border-main)',
        marginBottom: '24px'
      }}>
        <button
          onClick={() => setActiveTab('bookings')}
          style={{
            background: 'none',
            border: 'none',
            padding: '12px 24px',
            color: activeTab === 'bookings' ? 'var(--primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'bookings' ? '2px solid var(--primary)' : 'none',
            fontSize: '1rem',
            fontWeight: 600,
            cursor: 'pointer'
          }}
        >
          My Bookings ({bookings.length})
        </button>
        <button
          onClick={() => setActiveTab('waitlist')}
          style={{
            background: 'none',
            border: 'none',
            padding: '12px 24px',
            color: activeTab === 'waitlist' ? 'var(--primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'waitlist' ? '2px solid var(--primary)' : 'none',
            fontSize: '1rem',
            fontWeight: 600,
            cursor: 'pointer'
          }}
        >
          Waitlist Queue ({waitlist.filter((w) => ['WAITING', 'OFFERED'].includes(w.status)).length})
        </button>
      </div>

      {/* Error Message */}
      {error && (
        <div style={{
          background: 'rgba(239, 68, 68, 0.12)',
          border: '1px solid var(--color-booked)',
          color: 'var(--color-booked)',
          padding: '12px',
          borderRadius: '6px',
          marginBottom: '20px'
        }}>
          {error}
        </div>
      )}

      {/* TAB 1: BOOKINGS */}
      {activeTab === 'bookings' && (
        <div>
          {bookings.length === 0 ? (
            <div className="glass-card" style={{ textAlign: 'center', padding: '64px 24px' }}>
              <Ticket size={40} className="text-muted" style={{ marginBottom: '16px' }} />
              <h3>No Tickets Booked Yet</h3>
              <p className="text-muted" style={{ marginTop: '4px', marginBottom: '20px' }}>Explore upcoming shows and reserve your seats.</p>
              <Button onClick={() => navigate('/')}>Find Events</Button>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              {bookings.map((booking) => (
                <div key={booking.id} className="glass-card" style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '24px'
                }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
                      <span className="mono-text text-primary" style={{ fontWeight: 700 }}>
                        {booking.bookingRef || 'TKT-PENDING'}
                      </span>
                      {getStatusBadge(booking.status)}
                    </div>
                    
                    <h3 style={{ fontSize: '1.25rem', marginBottom: '6px' }}>{booking.eventTitle}</h3>
                    
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      <span>Venue: {booking.venueName}</span>
                      <span>Show Date: {formatDate(booking.eventDate)}</span>
                      <span>
                        Seats:{' '}
                        <strong style={{ color: 'var(--text-main)' }}>
                          {booking.seats?.map((s) => s.seatNumber).join(', ')}
                        </strong>
                      </span>
                    </div>
                  </div>

                  <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', gap: '12px', minWidth: '180px' }}>
                    <div>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block' }}>Total Paid</span>
                      <span className="mono-text" style={{ fontSize: '1.3rem', fontWeight: 800 }}>
                        ₹{booking.totalAmount?.toFixed(2)}
                      </span>
                    </div>

                    <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                      {booking.status === 'PENDING_PAYMENT' && (
                        <>
                          <Button
                            variant="secondary"
                            style={{ padding: '8px 12px', fontSize: '0.85rem' }}
                            onClick={() => handleCancelHold(booking.id)}
                          >
                            Release
                          </Button>
                          <Button
                            variant="primary"
                            style={{ padding: '8px 16px', fontSize: '0.85rem' }}
                            onClick={() => navigate(`/checkout/${booking.id}`)}
                          >
                            Pay
                          </Button>
                        </>
                      )}

                      {booking.status === 'CONFIRMED' && (
                        <Button
                          variant="secondary"
                          style={{ padding: '8px 12px', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                          onClick={() => handleOpenRefundModal(booking.id)}
                        >
                          <RotateCcw size={12} />
                          Request Refund
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: WAITLIST */}
      {activeTab === 'waitlist' && (
        <div>
          {waitlist.length === 0 ? (
            <div className="glass-card" style={{ textAlign: 'center', padding: '64px 24px' }}>
              <Clock size={40} className="text-muted" style={{ marginBottom: '16px' }} />
              <h3>No Waitlist Entries</h3>
              <p className="text-muted" style={{ marginTop: '4px' }}>You aren't currently waiting in line for any sold-out events.</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              {waitlist.map((entry) => (
                <div key={entry.entryId} className="glass-card" style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '24px',
                  borderLeft: entry.status === 'OFFERED' ? '4px solid var(--color-held)' : '1px solid var(--border-main)'
                }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
                      <span className="mono-text" style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                        Queue #{entry.entryId}
                      </span>
                      {getWaitlistStatusBadge(entry.status)}
                    </div>
                    
                    <h3 style={{ fontSize: '1.25rem', marginBottom: '6px' }}>Event ID: {entry.eventId}</h3>
                    
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      {entry.status === 'WAITING' && (
                        <span style={{ fontWeight: 600, color: 'var(--accent)' }}>
                          Current Queue Position: #{entry.positionInLine}
                        </span>
                      )}
                      
                      {entry.status === 'OFFERED' && (
                        <div>
                          <span style={{ fontWeight: 700, color: 'var(--color-held)' }}>
                            Offered Seat: {entry.offeredSeatNumber}
                          </span>
                          <span style={{ display: 'block', fontSize: '0.8rem', marginTop: '4px', color: 'var(--color-booked)' }}>
                            Offer expires at: {formatDate(entry.offerExpiresAt)}
                          </span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div>
                    {entry.status === 'WAITING' && (
                      <Button
                        variant="secondary"
                        style={{ padding: '8px 16px', fontSize: '0.85rem' }}
                        onClick={() => handleLeaveWaitlist(entry.entryId)}
                      >
                        Leave Queue
                      </Button>
                    )}

                    {entry.status === 'OFFERED' && (
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <Button
                          variant="secondary"
                          style={{ padding: '8px 12px', fontSize: '0.85rem' }}
                          onClick={() => handleLeaveWaitlist(entry.entryId)}
                        >
                          Decline
                        </Button>
                        <Button
                          variant="primary"
                          style={{ padding: '8px 16px', fontSize: '0.85rem' }}
                          onClick={() => handleAcceptOffer(entry.entryId)}
                        >
                          Accept & Book
                        </Button>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Refund Request Modal */}
      {showRefundModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={{ marginBottom: '16px' }}>Request Ticket Refund</h3>
            <p className="text-muted" style={{ fontSize: '0.9rem', marginBottom: '20px' }}>
              Are you sure you want to request a refund? Your seats will be released immediately back to the public pool upon approval.
            </p>

            <form onSubmit={handleRequestRefund}>
              <div className="form-group" style={{ marginBottom: '24px' }}>
                <label className="form-label">Reason for Refund</label>
                <textarea
                  className="form-input"
                  style={{ minHeight: '100px', resize: 'vertical' }}
                  placeholder="Tell us why you need a refund (optional)..."
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  maxLength={500}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setShowRefundModal(false)}
                  disabled={submittingRefund}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  loading={submittingRefund}
                >
                  Submit Request
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default MyBookings;
