import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import Button from '../components/Button';
import { Search, Calendar, MapPin, Flame, RefreshCw, Star, Info } from 'lucide-react';

export const Home = () => {
  const { user } = useAuth();
  
  const [events, setEvents] = useState([]);
  const [trendingEvents, setTrendingEvents] = useState([]);
  const [loadingEvents, setLoadingEvents] = useState(true);
  const [loadingTrending, setLoadingTrending] = useState(true);
  const [recalculating, setRecalculating] = useState(false);
  const [adminMsg, setAdminMsg] = useState('');
  
  // Search / filter states
  const [searchQuery, setSearchQuery] = useState('');
  const [cityFilter, setCityFilter] = useState('');

  const fetchEvents = async () => {
    setLoadingEvents(true);
    try {
      const res = await api.get('/events');
      const activeEvents = (res.data || []).filter(
        (evt) => new Date(evt.eventDate) > new Date()
      );
      setEvents(activeEvents);
    } catch (err) {
      console.error('Error fetching events:', err);
    } finally {
      setLoadingEvents(false);
    }
  };

  const fetchTrending = async () => {
    setLoadingTrending(true);
    try {
      const res = await api.get('/events/trending?limit=4');
      const activeTrending = (res.data || []).filter(
        (item) => new Date(item.eventDate) > new Date()
      );
      setTrendingEvents(activeTrending);
    } catch (err) {
      console.error('Error fetching trending:', err);
    } finally {
      setLoadingTrending(false);
    }
  };

  useEffect(() => {
    fetchEvents();
    fetchTrending();
  }, []);

  const handleRecalculateTrending = async () => {
    setRecalculating(true);
    setAdminMsg('');
    try {
      const res = await api.post('/admin/trending/recalculate');
      if (res.data && res.data.status) {
        setAdminMsg('Trending scores recalculated successfully.');
        fetchTrending(); // Refresh trending list
      }
    } catch (err) {
      console.error('Error recalculating trending scores:', err);
      setAdminMsg('Failed to recalculate.');
    } finally {
      setRecalculating(false);
    }
  };

  // Filter events based on search query and city
  const filteredEvents = events.filter((evt) => {
    const matchesSearch = evt.title?.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          evt.description?.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCity = cityFilter ? evt.venueCity?.toLowerCase() === cityFilter.toLowerCase() : true;
    return matchesSearch && matchesCity;
  });

  // Get unique cities for filter dropdown
  const cities = Array.from(new Set(events.map((e) => e.venueCity).filter(Boolean)));

  const formatDate = (dateStr) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div>
      {/* Cinematic Hero Banner */}
      <div style={{
        position: 'relative',
        height: '400px',
        borderRadius: 'var(--radius-lg)',
        overflow: 'hidden',
        marginBottom: '48px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        padding: '48px',
        background: 'linear-gradient(135deg, rgba(15, 23, 42, 0.95), rgba(88, 28, 135, 0.45)), url("https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?auto=format&fit=crop&w=1200&q=80")',
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        border: '1px solid var(--border-main)',
        boxShadow: '0 10px 30px rgba(0,0,0,0.5)'
      }}>
        <div style={{ maxWidth: '600px', zIndex: 2 }}>
          <span className="badge badge-primary" style={{ marginBottom: '16px' }}>Real-time Booking</span>
          <h1 style={{ fontSize: '3rem', lineHeight: '1.1', marginBottom: '16px', fontWeight: 800 }}>
            Experience Live Events in Real-Time
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '1.1rem', marginBottom: '28px' }}>
            Book premium seats instantly. Watch live seat map updates as they happen. Skip the queue and join the waitlist for sold-out shows.
          </p>
          <div style={{ display: 'flex', gap: '12px' }}>
            <a href="#all-events" className="btn btn-primary">
              Browse Shows
            </a>
          </div>
        </div>
        <div style={{
          position: 'absolute',
          top: 0,
          right: 0,
          bottom: 0,
          left: 0,
          background: 'radial-gradient(circle at right, transparent 20%, var(--bg-main) 90%)',
          zIndex: 1
        }}></div>
      </div>

      {/* Admin Panel Recalculate Widget */}
      {user && user.role === 'ADMIN' && (
        <div className="glass-card" style={{
          marginBottom: '32px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          borderLeft: '4px solid var(--primary)',
          flexWrap: 'wrap',
          gap: '16px'
        }}>
          <div>
            <h3 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Star size={18} className="text-primary" />
              Admin Actions — Recalculate Trending
            </h3>
            <p className="text-muted" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
              Manual override to recalculate the event popularity velocity scores instantly.
            </p>
            {adminMsg && (
              <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--color-available)', marginTop: '4px', display: 'block' }}>
                {adminMsg}
              </span>
            )}
          </div>
          <Button
            onClick={handleRecalculateTrending}
            loading={recalculating}
            variant="accent"
            style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
          >
            <RefreshCw size={16} />
            Recalculate Scores
          </Button>
        </div>
      )}

      {/* Trending Section */}
      {trendingEvents.length > 0 && (
        <div style={{ marginBottom: '48px' }}>
          <h2 style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '24px' }}>
            <Flame size={24} className="text-primary" />
            Trending Now
          </h2>
          <div className="grid-cols-3" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))' }}>
            {trendingEvents.map((item) => (
              <Link key={item.eventId} to={`/event/${item.eventId}`}>
                <div className="glass-card" style={{
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'between',
                  position: 'relative',
                  overflow: 'hidden',
                  padding: '20px'
                }}>
                  {/* Hot tag with velocity score */}
                  <div style={{
                    position: 'absolute',
                    top: '12px',
                    right: '12px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    background: 'rgba(244, 63, 94, 0.2)',
                    border: '1px solid var(--primary)',
                    borderRadius: '12px',
                    padding: '4px 8px',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    color: 'var(--primary)'
                  }}>
                    <Flame size={12} />
                    <span>Score: {item.velocityScore.toFixed(1)}</span>
                  </div>

                  <div>
                    <h3 style={{ fontSize: '1.2rem', marginBottom: '12px', paddingRight: '70px', minHeight: '48px', overflow: 'hidden' }}>
                      {item.title}
                    </h3>
                    
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <MapPin size={14} />
                        {item.venueName}, {item.venueCity}
                      </span>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Calendar size={14} />
                        {formatDate(item.eventDate)}
                      </span>
                    </div>
                  </div>

                  <div style={{
                    marginTop: '20px',
                    paddingTop: '12px',
                    borderTop: '1px solid var(--border-main)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}>
                    {item.availableSeats > 0 ? (
                      <span className="text-success" style={{ fontWeight: 600, fontSize: '0.85rem' }}>
                        {item.availableSeats} seats left
                      </span>
                    ) : (
                      <span className="badge badge-danger">Sold Out</span>
                    )}
                    <span className="text-primary" style={{ fontSize: '0.85rem', fontWeight: 600 }}>
                      Book Now &rarr;
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}

      {/* Main Browse Section */}
      <div id="all-events" style={{ scrollMarginTop: '90px' }}>
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px',
          flexWrap: 'wrap',
          gap: '16px'
        }}>
          <h2>Upcoming Events</h2>
          
          {/* Filters */}
          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
            <div style={{ position: 'relative', width: '220px' }}>
              <Search size={16} style={{
                position: 'absolute',
                left: '12px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--text-dark)'
              }} />
              <input
                type="text"
                className="form-input"
                style={{ paddingLeft: '36px', paddingTop: '8px', paddingBottom: '8px' }}
                placeholder="Search events..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            
            <select
              className="form-select"
              style={{ width: '150px', paddingTop: '8px', paddingBottom: '8px' }}
              value={cityFilter}
              onChange={(e) => setCityFilter(e.target.value)}
            >
              <option value="">All Cities</option>
              {cities.map((city) => (
                <option key={city} value={city}>{city}</option>
              ))}
            </select>
          </div>
        </div>

        {loadingEvents ? (
          <div className="flex-center" style={{ minHeight: '200px', color: 'var(--text-muted)' }}>
            <div className="animate-spin" style={{
              border: '3px solid var(--border-main)',
              borderTop: '3px solid var(--primary)',
              borderRadius: '50%',
              width: '30px',
              height: '30px',
              animation: 'spin 1s linear infinite'
            }}></div>
          </div>
        ) : filteredEvents.length === 0 ? (
          <div className="glass-card" style={{ textAlign: 'center', padding: '48px 24px' }}>
            <Info size={32} className="text-muted" style={{ marginBottom: '12px' }} />
            <h3>No Events Found</h3>
            <p className="text-muted" style={{ marginTop: '4px' }}>
              Try adjusting your search query or city filter.
            </p>
          </div>
        ) : (
          <div className="grid-cols-3">
            {filteredEvents.map((evt) => (
              <Link key={evt.id} to={`/event/${evt.id}`}>
                <div className="glass-card" style={{
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'between',
                  padding: '24px'
                }}>
                  <div>
                    <h3 style={{ fontSize: '1.25rem', marginBottom: '12px' }}>
                      {evt.title}
                    </h3>
                    <p className="text-muted" style={{
                      fontSize: '0.9rem',
                      marginBottom: '16px',
                      display: '-webkit-box',
                      WebkitLineClamp: 3,
                      WebkitBoxOrient: 'vertical',
                      overflow: 'hidden',
                      height: '60px'
                    }}>
                      {evt.description || 'No description provided.'}
                    </p>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <MapPin size={14} />
                        {evt.venueName}, {evt.venueCity}
                      </span>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Calendar size={14} />
                        {formatDate(evt.eventDate)}
                      </span>
                    </div>
                  </div>

                  <div style={{
                    marginTop: '24px',
                    paddingTop: '16px',
                    borderTop: '1px solid var(--border-main)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}>
                    {evt.status === 'CANCELLED' ? (
                      <span className="badge badge-danger">Cancelled</span>
                    ) : evt.availableSeats > 0 ? (
                      <span className="text-success" style={{ fontWeight: 600, fontSize: '0.85rem' }}>
                        {evt.availableSeats} / {evt.totalCapacity} seats available
                      </span>
                    ) : (
                      <span className="badge badge-danger">Sold Out</span>
                    )}
                    <span className="text-primary" style={{ fontSize: '0.85rem', fontWeight: 600 }}>
                      View Details &rarr;
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Home;
