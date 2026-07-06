import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useEventWebSocket } from '../hooks/useEventWebSocket';
import SeatMap from '../components/SeatMap';
import Button from '../components/Button';
import { Calendar, MapPin, Ticket, AlertCircle, Info, Clock, UserCheck } from 'lucide-react';

export const EventDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [event, setEvent] = useState(null);
  const [seats, setSeats] = useState([]);
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [justUpdatedSeatIds, setJustUpdatedSeatIds] = useState([]);
  
  // Waitlist states
  const [waitlistEntry, setWaitlistEntry] = useState(null);
  const [joiningWaitlist, setJoiningWaitlist] = useState(false);
  const [leavingWaitlist, setLeavingWaitlist] = useState(false);

  // Status states
  const [loading, setLoading] = useState(true);
  const [bookingLoading, setBookingLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchEventAndSeats = async () => {
    setLoading(true);
    try {
      const eventRes = await api.get(`/events/${id}`);
      setEvent(eventRes.data);

      const seatsRes = await api.get(`/events/${id}/seats`);
      setSeats(seatsRes.data || []);

      // If logged in, check if user is already on the waitlist for this event
      if (isAuthenticated) {
        const waitlistRes = await api.get('/waitlist/my');
        const activeEntry = waitlistRes.data?.find(
          (entry) => entry.eventId === Number(id) && 
          ['WAITING', 'OFFERED'].includes(entry.status)
        );
        if (activeEntry) {
          setWaitlistEntry(activeEntry);
        }
      }
    } catch (err) {
      console.error('Error fetching event details:', err);
      setError('Could not load event details. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEventAndSeats();
  }, [id, isAuthenticated]);

  // Handle live WebSocket seat updates
  const handleSeatUpdate = useCallback((message) => {
    // message is SeatUpdateMessage { seatId, seatNumber, status, eventType, eventId }
    setSeats((prevSeats) =>
      prevSeats.map((seat) =>
        seat.id === message.seatId ? { ...seat, status: message.status } : seat
      )
    );

    // Trigger flashing micro-animation
    setJustUpdatedSeatIds((prev) => [...prev, message.seatId]);
    setTimeout(() => {
      setJustUpdatedSeatIds((prev) => prev.filter((seatId) => seatId !== message.seatId));
    }, 800);
  }, []);

  // Connect to WebSocket using custom hook
  useEventWebSocket(id, handleSeatUpdate);

  const handleSeatSelect = (seat) => {
    const isAlreadySelected = selectedSeats.some((s) => s.id === seat.id);
    if (isAlreadySelected) {
      setSelectedSeats((prev) => prev.filter((s) => s.id !== seat.id));
    } else {
      setSelectedSeats((prev) => [...prev, seat]);
    }
  };

  const handleBookSeats = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    if (selectedSeats.length === 0) {
      setError('Please select at least one seat.');
      return;
    }

    setBookingLoading(true);
    setError('');

    try {
      const seatIds = selectedSeats.map((s) => s.id);
      const res = await api.post('/seats/hold', {
        seatIds,
        eventId: Number(id),
      });

      // res.data is BookingResponseDTO
      if (res.data && res.data.id) {
        navigate(`/checkout/${res.data.id}`);
      } else {
        setError(res.data?.status || 'Failed to hold seats. They may have been booked by someone else.');
      }
    } catch (err) {
      console.error('Error holding seats:', err);
      setError(err.response?.data?.status || 'Failed to hold seats. Some seats might have been claimed just now.');
      // Refresh seat map to show latest status
      const seatsRes = await api.get(`/events/${id}/seats`);
      setSeats(seatsRes.data || []);
      setSelectedSeats([]);
    } finally {
      setBookingLoading(false);
    }
  };

  const handleJoinWaitlist = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    setJoiningWaitlist(true);
    setError('');
    try {
      const res = await api.post('/waitlist', { eventId: Number(id) });
      if (res.data && res.data.success) {
        setWaitlistEntry({
          entryId: res.data.entryId,
          status: res.data.status,
          positionInLine: res.data.positionInLine,
        });
      } else {
        setError(res.data.message || 'Failed to join waitlist.');
      }
    } catch (err) {
      console.error('Error joining waitlist:', err);
      setError(err.response?.data?.message || 'Error joining waitlist.');
    } finally {
      setJoiningWaitlist(false);
    }
  };

  const handleLeaveWaitlist = async () => {
    if (!waitlistEntry) return;

    setLeavingWaitlist(true);
    setError('');
    try {
      const res = await api.delete(`/waitlist/${waitlistEntry.entryId}`);
      if (res.data && res.data.status) {
        setWaitlistEntry(null);
      } else {
        setError(res.data.message || 'Failed to leave waitlist.');
      }
    } catch (err) {
      console.error('Error leaving waitlist:', err);
      setError(err.response?.data?.message || 'Error leaving waitlist.');
    } finally {
      setLeavingWaitlist(false);
    }
  };

  const formatDate = (dateStr) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
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

  if (!event) {
    return (
      <div className="glass-card" style={{ textAlign: 'center', padding: '48px' }}>
        <AlertCircle size={32} className="text-danger" style={{ marginBottom: '12px' }} />
        <h3>Event Not Found</h3>
        <p className="text-muted" style={{ marginTop: '4px' }}>This event might have been cancelled or completed.</p>
        <Button style={{ marginTop: '24px' }} onClick={() => navigate('/')}>Back to Home</Button>
      </div>
    );
  }

  const hasSeatsAvailable = seats.some((s) => s.status === 'AVAILABLE');
  const totalPrice = selectedSeats.reduce((sum, seat) => sum + Number(seat.price), 0);

  return (
    <div>
      {/* Event Info Header */}
      <div className="glass-card" style={{ marginBottom: '32px', padding: '32px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '24px' }}>
          <div>
            <span className="badge badge-accent" style={{ marginBottom: '12px' }}>
              {event.status}
            </span>
            <h1 style={{ fontSize: '2.5rem', marginBottom: '16px' }}>{event.title}</h1>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', color: 'var(--text-muted)', fontSize: '0.95rem' }}>
              <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Calendar size={16} className="text-primary" />
                {formatDate(event.eventDate)}
              </span>
              <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <MapPin size={16} className="text-accent" />
                {event.venueName}, {event.venueCity}
              </span>
            </div>
          </div>

          <div style={{
            background: 'rgba(255, 255, 255, 0.02)',
            border: '1px solid var(--border-main)',
            borderRadius: '12px',
            padding: '16px 24px',
            textAlign: 'right',
            minWidth: '200px'
          }}>
            <span style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-muted)' }}>
              Availability
            </span>
            <h2 style={{ fontSize: '2rem', marginTop: '4px' }} className={hasSeatsAvailable ? 'text-success' : 'text-danger'}>
              {hasSeatsAvailable ? `${event.availableSeats} Left` : 'Sold Out'}
            </h2>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Total Capacity: {event.totalCapacity}
            </span>
          </div>
        </div>

        <div style={{ marginTop: '24px', borderTop: '1px solid var(--border-main)', paddingTop: '24px' }}>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '8px' }}>About the Event</h3>
          <p style={{ color: 'var(--text-muted)', lineHeight: '1.6', whiteSpace: 'pre-line' }}>
            {event.description || 'No description available for this event.'}
          </p>
        </div>
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
          fontSize: '0.9rem',
          marginBottom: '24px'
        }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Main Layout: Seat Map on Left, Sidebar on Right */}
      {event.status === 'CANCELLED' ? (
        <div className="glass-card" style={{ textAlign: 'center', padding: '48px 24px', borderLeft: '4px solid var(--color-booked)' }}>
          <AlertCircle size={32} className="text-danger" style={{ marginBottom: '12px' }} />
          <h3>Event Cancelled</h3>
          <p className="text-muted" style={{ marginTop: '4px' }}>This event has been cancelled. If you held tickets, refunds will be processed.</p>
        </div>
      ) : hasSeatsAvailable ? (
        <div className="grid-cols-1-2">
          {/* Left: Seat Map Grid */}
          <div>
            <h2 style={{ marginBottom: '16px', fontSize: '1.4rem' }}>Select Your Seats</h2>
            <SeatMap
              seats={seats}
              selectedSeatIds={selectedSeats.map((s) => s.id)}
              onSeatSelect={handleSeatSelect}
              justUpdatedSeatIds={justUpdatedSeatIds}
              maxSelection={10}
            />
          </div>

          {/* Right: Booking Panel */}
          <div>
            <div className="glass-card" style={{ position: 'sticky', top: '90px' }}>
              <h3 style={{ borderBottom: '1px solid var(--border-main)', paddingBottom: '12px', marginBottom: '16px' }}>
                Booking Summary
              </h3>

              {selectedSeats.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--text-muted)' }}>
                  <Ticket size={32} style={{ marginBottom: '12px', opacity: 0.5 }} />
                  <p>No seats selected yet</p>
                  <p style={{ fontSize: '0.8rem', marginTop: '4px' }}>Click available seats on the map to select (up to 10).</p>
                </div>
              ) : (
                <div>
                  {/* Selected Seats List */}
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '20px', maxHeight: '200px', overflowY: 'auto' }}>
                    {selectedSeats.map((seat) => (
                      <div key={seat.id} className="flex-between" style={{
                        padding: '10px 14px',
                        background: 'rgba(255,255,255,0.02)',
                        border: '1px solid var(--border-main)',
                        borderRadius: '6px'
                      }}>
                        <div>
                          <span style={{ fontWeight: 700 }}>Seat {seat.seatNumber}</span>
                          <span style={{ fontSize: '0.75rem', marginLeft: '8px' }} className={
                            seat.category === 'VIP' ? 'text-warning' : seat.category === 'PREMIUM' ? 'text-accent' : 'text-muted'
                          }>
                            ({seat.category})
                          </span>
                        </div>
                        <span className="mono-text" style={{ fontWeight: 600 }}>₹{seat.price}</span>
                      </div>
                    ))}
                  </div>

                  {/* Total pricing */}
                  <div style={{
                    borderTop: '1px solid var(--border-main)',
                    paddingTop: '16px',
                    marginBottom: '24px'
                  }}>
                    <div className="flex-between" style={{ marginBottom: '8px' }}>
                      <span className="text-muted">Total Tickets</span>
                      <span>{selectedSeats.length}</span>
                    </div>
                    <div className="flex-between">
                      <span style={{ fontWeight: 600 }}>Grand Total</span>
                      <span className="mono-text" style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--primary)' }}>
                        ₹{totalPrice.toFixed(2)}
                      </span>
                    </div>
                  </div>

                  <Button
                    onClick={handleBookSeats}
                    loading={bookingLoading}
                    style={{ width: '100%', padding: '14px' }}
                  >
                    {isAuthenticated ? 'Confirm & Hold Seats' : 'Login to Book Seats'}
                  </Button>
                  
                  <p className="text-muted" style={{ fontSize: '0.75rem', marginTop: '12px', display: 'flex', alignItems: 'center', gap: '4px', justifyContent: 'center' }}>
                    <Clock size={12} />
                    Holding seats creates a temporary 10-minute hold.
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      ) : (
        /* Event Sold Out — Waitlist options */
        <div className="glass-card" style={{
          padding: '40px',
          textAlign: 'center',
          maxWidth: '600px',
          margin: '0 auto',
          borderLeft: '4px solid var(--accent)'
        }}>
          <Info size={36} className="text-accent" style={{ marginBottom: '16px' }} />
          <h2>This Event is Sold Out</h2>
          <p className="text-muted" style={{ marginTop: '8px', marginBottom: '24px', fontSize: '1rem', lineHeight: '1.5' }}>
            All physical seats are currently booked or held by other users. Join our automated waitlist to secure a spot if anyone cancels or their hold expires.
          </p>

          {waitlistEntry ? (
            <div style={{
              background: 'rgba(99, 102, 241, 0.08)',
              border: '1px solid rgba(99, 102, 241, 0.3)',
              borderRadius: '8px',
              padding: '24px',
              maxWidth: '400px',
              margin: '0 auto'
            }}>
              {waitlistEntry.status === 'OFFERED' ? (
                <div>
                  <span className="badge badge-warning" style={{ marginBottom: '8px' }}>Seat Offered!</span>
                  <p style={{ fontWeight: 600 }}>We have offered you Seat {waitlistEntry.offeredSeatNumber}!</p>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                    Go to your Bookings page immediately to accept the offer before it expires.
                  </p>
                  <Button
                    variant="primary"
                    style={{ marginTop: '16px', width: '100%' }}
                    onClick={() => navigate('/my-bookings')}
                  >
                    View Offer
                  </Button>
                </div>
              ) : (
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', marginBottom: '8px' }}>
                    <UserCheck size={18} className="text-accent" />
                    <span style={{ fontWeight: 700, fontSize: '1.1rem' }}>You're in Queue</span>
                  </div>
                  <h3 style={{ fontSize: '2rem', margin: '8px 0' }} className="text-accent">
                    #{waitlistEntry.positionInLine}
                  </h3>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    Your current position in line. We will notify you via email when a seat becomes available.
                  </p>
                  <Button
                    variant="secondary"
                    loading={leavingWaitlist}
                    onClick={handleLeaveWaitlist}
                    style={{ marginTop: '16px', width: '100%', padding: '10px' }}
                  >
                    Leave Waitlist
                  </Button>
                </div>
              )}
            </div>
          ) : (
            <Button
              variant="accent"
              loading={joiningWaitlist}
              onClick={handleJoinWaitlist}
              style={{ padding: '14px 32px' }}
            >
              Join the Waitlist
            </Button>
          )}
        </div>
      )}
    </div>
  );
};

export default EventDetail;
