import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useRazorpay } from '../hooks/useRazorpay';
import Button from '../components/Button';
import { AlertCircle, Clock, CheckCircle2, CreditCard, Ticket, ArrowLeft, ShieldAlert } from 'lucide-react';

export const BookingCheckout = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const rzpLoaded = useRazorpay();

  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [checkoutLoading, setCheckoutLoading] = useState(false);
  const [error, setError] = useState('');
  
  // Timer states
  const [timeLeft, setTimeLeft] = useState('');
  const [isExpired, setIsExpired] = useState(false);
  
  // Polling states
  const [isPolling, setIsPolling] = useState(false);
  const [pollingStatus, setPollingStatus] = useState(''); // 'verifying', 'success', 'failed'
  
  const timerRef = useRef(null);
  const pollIntervalRef = useRef(null);
  const pollTimeoutRef = useRef(null);

  const fetchBookingDetails = async () => {
    try {
      const res = await api.get(`/bookings/${id}`);
      setBooking(res.data);
      
      // If booking is already confirmed, show success immediately
      if (res.data.status === 'CONFIRMED') {
        setPollingStatus('success');
      } else if (['CANCELLED', 'PAYMENT_FAILED'].includes(res.data.status)) {
        setIsExpired(true);
      }
    } catch (err) {
      console.error('Error fetching booking details:', err);
      setError('Could not load booking details.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookingDetails();
    return () => {
      clearInterval(timerRef.current);
      clearInterval(pollIntervalRef.current);
      clearTimeout(pollTimeoutRef.current);
    };
  }, [id]);

  // Countdown timer calculation (10 minutes from booking creation)
  useEffect(() => {
    if (!booking || booking.status !== 'PENDING_PAYMENT') return;

    const calculateTimeLeft = () => {
      const createdAtTime = new Date(booking.createdAt).getTime();
      const expiryTime = createdAtTime + 10 * 60 * 1000; // 10 minutes hold
      const difference = expiryTime - Date.now();

      if (difference <= 0) {
        setTimeLeft('00:00');
        setIsExpired(true);
        clearInterval(timerRef.current);
        // If expired, let's update the status locally to match
        setBooking((prev) => prev ? { ...prev, status: 'CANCELLED' } : null);
        return;
      }

      const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((difference % (1000 * 60)) / 1000);
      
      const formattedMin = String(minutes).padStart(2, '0');
      const formattedSec = String(seconds).padStart(2, '0');
      
      setTimeLeft(`${formattedMin}:${formattedSec}`);
    };

    calculateTimeLeft();
    timerRef.current = setInterval(calculateTimeLeft, 1000);

    return () => clearInterval(timerRef.current);
  }, [booking]);

  // Start polling the backend booking status until confirmed
  const startPolling = () => {
    setIsPolling(true);
    setPollingStatus('verifying');
    
    let pollCount = 0;
    const maxPolls = 15; // 15 * 2 seconds = 30 seconds max polling

    pollIntervalRef.current = setInterval(async () => {
      pollCount++;
      try {
        const res = await api.get(`/bookings/${id}`);
        if (res.data && res.data.status === 'CONFIRMED') {
          setBooking(res.data);
          setPollingStatus('success');
          clearInterval(pollIntervalRef.current);
        } else if (res.data && res.data.status === 'PAYMENT_FAILED') {
          setBooking(res.data);
          setPollingStatus('failed');
          setError('Payment failed at processing. Please try again.');
          clearInterval(pollIntervalRef.current);
        }
      } catch (err) {
        console.error('Error polling booking status:', err);
      }

      if (pollCount >= maxPolls) {
        clearInterval(pollIntervalRef.current);
        setPollingStatus('timeout');
        setError('Verification is taking longer than expected. Please check "My Bookings" in a few moments.');
      }
    }, 2000);
  };

  const handlePay = async () => {
    if (!rzpLoaded) {
      setError('Payment gateway is loading. Please wait a second and try again.');
      return;
    }

    setCheckoutLoading(true);
    setError('');

    try {
      // 1. Create Razorpay Order on backend
      const orderRes = await api.post('/payments/create-order', {
        bookingId: Number(id)
      });
      
      const orderData = orderRes.data; // PaymentInitResponseDTO
      
      // 2. Configure and open Razorpay Checkout Popup
      const options = {
        key: orderData.razorpayKeyId,
        amount: orderData.amountInPaise,
        currency: orderData.currency || 'INR',
        name: 'EventPass',
        description: `Booking for ${booking.eventTitle}`,
        image: 'https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?auto=format&fit=crop&w=100&q=80',
        order_id: orderData.razorpayOrderId,
        prefill: {
          email: user?.email || '',
        },
        theme: {
          color: '#f43f5e', // Neon pink
        },
        modal: {
          ondismiss: () => {
            setCheckoutLoading(false);
          }
        },
        handler: function (response) {
          // Inside the success callback: DO NOT assume payment is confirmed.
          // Start polling the backend to verify the webhook has completed.
          startPolling();
        }
      };

      const rzp = new window.Razorpay(options);

      rzp.on('payment.failed', function (response) {
        // Explicitly handle failure cases
        setError(`Payment failed: ${response.error.description}. (Code: ${response.error.code})`);
        setCheckoutLoading(false);
      });

      rzp.open();
    } catch (err) {
      console.error('Error initiating payment:', err);
      setError(err.response?.data?.message || 'Could not initiate payment. Please try again.');
      setCheckoutLoading(false);
    }
  };

  const handleCancelBooking = async () => {
    if (window.confirm('Are you sure you want to release these seats and cancel your hold?')) {
      setLoading(true);
      try {
        await api.delete(`/bookings/${id}/cancel`);
        navigate(`/event/${booking.eventId}`);
      } catch (err) {
        console.error('Error cancelling booking:', err);
        setError('Failed to cancel hold.');
        setLoading(false);
      }
    }
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

  if (pollingStatus === 'verifying') {
    return (
      <div className="flex-center" style={{ minHeight: 'calc(100vh - 200px)', flexDirection: 'column', gap: '24px' }}>
        <div className="animate-spin" style={{
          border: '4px solid var(--border-main)',
          borderTop: '4px solid var(--primary)',
          borderRadius: '50%',
          width: '50px',
          height: '50px',
          animation: 'spin 1s linear infinite'
        }}></div>
        <div style={{ textAlign: 'center' }}>
          <h2>Verifying Payment...</h2>
          <p className="text-muted" style={{ marginTop: '8px', maxWidth: '400px' }}>
            We've received confirmation from Razorpay. Waiting for our secure backend webhook to confirm the transaction. Do not close or refresh this page.
          </p>
        </div>
      </div>
    );
  }

  if (pollingStatus === 'success' && booking) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 'calc(100vh - 200px)', padding: '20px' }}>
        <div className="glass-card" style={{ maxWidth: '550px', width: '100%', padding: '40px', borderLeft: '4px solid var(--color-available)' }}>
          <div style={{ textAlign: 'center', marginBottom: '32px' }}>
            <CheckCircle2 size={54} className="text-success" style={{ marginBottom: '16px' }} />
            <h2>Payment Confirmed!</h2>
            <p className="text-muted" style={{ fontSize: '0.95rem', marginTop: '6px' }}>
              Your tickets have been successfully booked.
            </p>
          </div>

          <div style={{
            background: 'rgba(255,255,255,0.02)',
            border: '1px solid var(--border-main)',
            borderRadius: '8px',
            padding: '20px',
            marginBottom: '32px'
          }}>
            <h4 style={{ marginBottom: '16px', borderBottom: '1px solid var(--border-main)', paddingBottom: '8px' }}>
              Booking Reference:{' '}
              <span className="mono-text text-primary" style={{ fontWeight: 700 }}>
                {booking.bookingRef}
              </span>
            </h4>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '0.95rem' }}>
              <div className="flex-between">
                <span className="text-muted">Show:</span>
                <span style={{ fontWeight: 600 }}>{booking.eventTitle}</span>
              </div>
              <div className="flex-between">
                <span className="text-muted">Venue:</span>
                <span>{booking.venueName}</span>
              </div>
              <div className="flex-between">
                <span className="text-muted">Seats Booked:</span>
                <span style={{ fontWeight: 600 }}>
                  {booking.seats?.map((s) => s.seatNumber).join(', ')}
                </span>
              </div>
              
              {booking.ticketNumbers && booking.ticketNumbers.length > 0 && (
                <div style={{ marginTop: '12px', paddingTop: '12px', borderTop: '1px dashed var(--border-main)' }}>
                  <span className="text-muted" style={{ display: 'block', marginBottom: '8px', fontSize: '0.85rem' }}>
                    Digital Tickets:
                  </span>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                    {booking.ticketNumbers.map((tkt) => (
                      <span key={tkt} className="badge badge-accent mono-text" style={{ fontSize: '0.8rem' }}>
                        {tkt}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>

          <div style={{ display: 'flex', gap: '16px' }}>
            <Button variant="secondary" style={{ flex: 1 }} onClick={() => navigate('/')}>
              Browse More
            </Button>
            <Button variant="primary" style={{ flex: 1 }} onClick={() => navigate('/my-bookings')}>
              Go to Bookings
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '750px', margin: '0 auto', padding: '20px' }}>
      <button onClick={() => navigate(-1)} style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        background: 'none',
        border: 'none',
        color: 'var(--text-muted)',
        cursor: 'pointer',
        marginBottom: '24px',
        fontSize: '0.95rem'
      }}>
        <ArrowLeft size={16} />
        Back
      </button>

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
          fontSize: '0.9rem',
          marginBottom: '24px'
        }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      <div className="grid-cols-1-2">
        {/* Left: Summary */}
        <div>
          <h2 style={{ marginBottom: '16px' }}>Verify & Pay</h2>
          <div className="glass-card" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div>
              <span className="text-muted" style={{ fontSize: '0.85rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Booking Reference
              </span>
              <h3 className="mono-text" style={{ fontSize: '1.25rem', marginTop: '4px', color: 'var(--text-main)' }}>
                {booking?.bookingRef || 'TKT-PENDING'}
              </h3>
            </div>

            <div>
              <span className="text-muted" style={{ fontSize: '0.85rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Event
              </span>
              <h3 style={{ fontSize: '1.2rem', marginTop: '4px' }}>{booking?.eventTitle}</h3>
              <p className="text-muted" style={{ fontSize: '0.85rem', marginTop: '2px' }}>{booking?.venueName}</p>
            </div>

            <div>
              <span className="text-muted" style={{ fontSize: '0.85rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Selected Seats
              </span>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '6px' }}>
                {booking?.seats?.map((seat) => (
                  <span key={seat.id} className="badge badge-accent">
                    {seat.seatNumber} ({seat.category})
                  </span>
                ))}
              </div>
            </div>

            <div style={{ borderTop: '1px solid var(--border-main)', paddingTop: '16px' }}>
              <div className="flex-between" style={{ fontSize: '1.1rem' }}>
                <span>Total Amount</span>
                <span className="mono-text" style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--primary)' }}>
                  ₹{booking?.totalAmount?.toFixed(2)}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Right: Checkout Actions */}
        <div>
          <div className="glass-card" style={{ borderLeft: isExpired ? '4px solid var(--color-booked)' : '4px solid var(--color-held)' }}>
            {isExpired ? (
              <div style={{ textAlign: 'center', padding: '20px 0' }}>
                <ShieldAlert size={40} className="text-danger" style={{ marginBottom: '12px' }} />
                <h3>Hold Expired</h3>
                <p className="text-muted" style={{ fontSize: '0.9rem', marginTop: '8px' }}>
                  The 10-minute hold window on these seats has closed and they have been released back to available. Please go back to the seat map and select again.
                </p>
                <Button
                  variant="primary"
                  style={{ width: '100%', marginTop: '24px' }}
                  onClick={() => navigate(`/event/${booking.eventId}`)}
                >
                  Return to Seat Map
                </Button>
              </div>
            ) : (
              <div>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '10px',
                  background: 'rgba(245, 158, 11, 0.08)',
                  padding: '12px',
                  borderRadius: '6px',
                  border: '1px solid rgba(245, 158, 11, 0.2)',
                  color: 'var(--color-held)',
                  marginBottom: '24px'
                }}>
                  <Clock size={20} className="animate-pulse" style={{ animation: 'pulse 1.5s infinite' }} />
                  <div>
                    <span style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.05em', display: 'block' }}>
                      Time Remaining to Pay
                    </span>
                    <span style={{ fontSize: '1.3rem', fontWeight: 800 }}>{timeLeft}</span>
                  </div>
                </div>

                <p className="text-muted" style={{ fontSize: '0.85rem', marginBottom: '24px', lineHeight: '1.5' }}>
                  To secure your booking, please click below to pay via Razorpay. Your seats are held exclusively for you until the timer runs out.
                </p>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  <Button
                    onClick={handlePay}
                    loading={checkoutLoading}
                    style={{ width: '100%', padding: '14px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px' }}
                  >
                    <CreditCard size={18} />
                    Pay via Razorpay
                  </Button>
                  
                  <Button
                    variant="secondary"
                    onClick={handleCancelBooking}
                    style={{ width: '100%', padding: '10px' }}
                  >
                    Cancel & Release Seats
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default BookingCheckout;
