import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import Button from '../components/Button';
import { 
  Plus, MapPin, Calendar, Layout, Award, DollarSign, Upload, AlertCircle, 
  Info, RefreshCw, X, BarChart3, TrendingUp, Users, Download, Search, PieChart, Ticket
} from 'lucide-react';

export const OrganiserDashboard = () => {
  const navigate = useNavigate();
  const [venues, setVenues] = useState([]);
  const [myEvents, setMyEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Navigation states
  const [activeSubTab, setActiveSubTab] = useState('events'); // 'events' | 'analytics' | 'create-event' | 'create-venue'

  // Analytics states
  const [analyticsData, setAnalyticsData] = useState(null);
  const [loadingAnalytics, setLoadingAnalytics] = useState(false);
  const [showRosterModal, setShowRosterModal] = useState(false);
  const [selectedEventForRoster, setSelectedEventForRoster] = useState(null);
  const [attendeeList, setAttendeeList] = useState([]);
  const [loadingAttendees, setLoadingAttendees] = useState(false);
  const [attendeeSearch, setAttendeeSearch] = useState('');

  // Form States: Venue
  const [venueName, setVenueName] = useState('');
  const [venueCity, setVenueCity] = useState('');
  const [venueAddress, setVenueAddress] = useState('');
  const [venueCapacity, setVenueCapacity] = useState('');
  const [creatingVenue, setCreatingVenue] = useState(false);

  // Form States: Event
  const [eventTitle, setEventTitle] = useState('');
  const [eventDesc, setEventDesc] = useState('');
  const [eventDate, setEventDate] = useState('');
  const [selectedVenueId, setSelectedVenueId] = useState('');
  const [seatLayout, setSeatLayout] = useState([
    { rowPrefix: 'A', category: 'GENERAL', quantity: 10, price: '500.00' }
  ]);
  const [creatingEvent, setCreatingEvent] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const venuesRes = await api.get('/organiser/venues');
      setVenues(venuesRes.data || []);

      const eventsRes = await api.get('/organiser/events');
      setMyEvents(eventsRes.data || []);
    } catch (err) {
      console.error('Error fetching organiser data:', err);
      setError('Could not load dashboard data. Are you logged in as an Organiser?');
    } finally {
      setLoading(false);
    }
  };

  const fetchAnalytics = async () => {
    setLoadingAnalytics(true);
    try {
      const res = await api.get('/organiser/analytics');
      setAnalyticsData(res.data);
    } catch (err) {
      console.error('Error fetching analytics:', err);
    } finally {
      setLoadingAnalytics(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    if (activeSubTab === 'analytics') {
      fetchAnalytics();
    }
  }, [activeSubTab]);

  const handleOpenRoster = async (eventSummary) => {
    setSelectedEventForRoster(eventSummary);
    setShowRosterModal(true);
    setAttendeeSearch('');
    setLoadingAttendees(true);
    try {
      const res = await api.get(`/organiser/events/${eventSummary.eventId || eventSummary.id}/attendees`);
      setAttendeeList(res.data || []);
    } catch (err) {
      console.error('Error fetching attendee roster:', err);
      alert('Could not load attendee roster.');
    } finally {
      setLoadingAttendees(false);
    }
  };

  const exportAttendeesCSV = (eventTitle, attendees) => {
    if (!attendees || attendees.length === 0) {
      alert('No attendees available to export.');
      return;
    }
    const headers = ['Booking Ref', 'Attendee Name', 'Email', 'Assigned Seat(s)', 'Total Seats', 'Amount Paid (INR)', 'Booking Status', 'Date'];
    const rows = attendees.map(a => [
      `"${a.bookingRef || ''}"`,
      `"${a.userName || ''}"`,
      `"${a.userEmail || ''}"`,
      `"${a.seatNumbers || ''}"`,
      a.seatCount || 0,
      a.totalAmount ? a.totalAmount.toFixed(2) : '0.00',
      `"${a.status || ''}"`,
      `"${a.bookingDate ? new Date(a.bookingDate).toLocaleString() : ''}"`
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `Roster_${(eventTitle || 'Event').replace(/[^a-z0-9]/gi, '_')}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Handle Venue Submit
  const handleCreateVenue = async (e) => {
    e.preventDefault();
    if (!venueName || !venueCity || !venueAddress || !venueCapacity) {
      alert('Please fill in all venue fields.');
      return;
    }

    setCreatingVenue(true);
    try {
      const res = await api.post('/organiser/venues', {
        name: venueName,
        city: venueCity,
        address: venueAddress,
        totalCapacity: Number(venueCapacity)
      });

      if (res.data && res.data.id) {
        alert('Venue created successfully!');
        setVenueName('');
        setVenueCity('');
        setVenueAddress('');
        setVenueCapacity('');
        await fetchData();
        setActiveSubTab('events');
      }
    } catch (err) {
      console.error('Error creating venue:', err);
      alert(err.response?.data?.name || 'Failed to create venue.');
    } finally {
      setCreatingVenue(false);
    }
  };

  // Dynamic Seat Layout Rows
  const handleAddLayoutRow = () => {
    setSeatLayout([...seatLayout, { rowPrefix: '', category: 'GENERAL', quantity: 10, price: '500.00' }]);
  };

  const handleRemoveLayoutRow = (index) => {
    const layout = [...seatLayout];
    layout.splice(index, 1);
    setSeatLayout(layout);
  };

  const handleLayoutChange = (index, field, value) => {
    const layout = [...seatLayout];
    layout[index][field] = value;
    setSeatLayout(layout);
  };

  const getLayoutTotalCapacity = () => {
    return seatLayout.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0);
  };

  const handleCreateEvent = async (e) => {
    e.preventDefault();

    if (!eventTitle || !eventDate || !selectedVenueId) {
      alert('Please fill in all required event fields.');
      return;
    }

    const selectedVenue = venues.find((v) => v.id === Number(selectedVenueId));
    if (!selectedVenue) {
      alert('Invalid venue selected.');
      return;
    }

    const layoutTotal = getLayoutTotalCapacity();
    if (layoutTotal !== selectedVenue.totalCapacity) {
      alert(`Capacity Mismatch! The seat layout defines ${layoutTotal} seats, but the venue capacity is ${selectedVenue.totalCapacity}. They must match exactly.`);
      return;
    }

    setCreatingEvent(true);
    try {
      const formattedLayout = seatLayout.map((row) => ({
        rowPrefix: row.rowPrefix.toUpperCase(),
        category: row.category,
        quantity: Number(row.quantity),
        price: Number(row.price)
      }));

      const res = await api.post('/organiser/events', {
        title: eventTitle,
        description: eventDesc,
        eventDate: eventDate,
        venueId: Number(selectedVenueId),
        seatLayout: formattedLayout
      });

      if (res.data && res.data.id) {
        alert('Draft Event created successfully!');
        setEventTitle('');
        setEventDesc('');
        setEventDate('');
        setSelectedVenueId('');
        setSeatLayout([{ rowPrefix: 'A', category: 'GENERAL', quantity: 10, price: '500.00' }]);

        await fetchData();
        setActiveSubTab('events');
      }
    } catch (err) {
      console.error('Error creating event:', err);
      alert('Failed to create event.');
    } finally {
      setCreatingEvent(false);
    }
  };

  const handlePublishEvent = async (eventId) => {
    if (window.confirm('Are you sure you want to publish this event? This will generate the seat map and make the event live.')) {
      setLoading(true);
      try {
        await api.patch(`/organiser/events/${eventId}/publish`);
        alert('Event published successfully!');
        fetchData();
      } catch (err) {
        console.error('Error publishing event:', err);
        alert('Failed to publish event.');
        setLoading(false);
      }
    }
  };

  const handleCancelEvent = async (eventId) => {
    if (window.confirm('WARNING: Are you sure you want to cancel this event? This will release all holds and cancel all bookings.')) {
      setLoading(true);
      try {
        await api.patch(`/organiser/events/${eventId}/cancel`);
        alert('Event cancelled.');
        fetchData();
      } catch (err) {
        console.error('Error cancelling event:', err);
        alert('Failed to cancel event.');
        setLoading(false);
      }
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

  const filteredAttendees = attendeeList.filter(a => {
    const term = attendeeSearch.toLowerCase();
    return (
      (a.bookingRef && a.bookingRef.toLowerCase().includes(term)) ||
      (a.userName && a.userName.toLowerCase().includes(term)) ||
      (a.userEmail && a.userEmail.toLowerCase().includes(term)) ||
      (a.seatNumbers && a.seatNumbers.toLowerCase().includes(term))
    );
  });

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
        <div>
          <h2>Organiser Dashboard</h2>
          <p className="text-muted" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
            Manage venues, monitor ticket sales analytics, and design seat layouts
          </p>
        </div>
        <button onClick={() => { fetchData(); if (activeSubTab === 'analytics') fetchAnalytics(); }} className="btn btn-secondary" style={{ padding: '8px 12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
          <RefreshCw size={14} />
          Refresh
        </button>
      </div>

      {error && (
        <div style={{
          background: 'rgba(239, 68, 68, 0.12)',
          border: '1px solid var(--color-booked)',
          color: 'var(--color-booked)',
          padding: '16px',
          borderRadius: '6px',
          marginBottom: '32px'
        }}>
          {error}
        </div>
      )}

      {/* Sub Tabs */}
      <div style={{ display: 'flex', gap: '12px', marginBottom: '32px', flexWrap: 'wrap' }}>
        <Button
          variant={activeSubTab === 'events' ? 'primary' : 'secondary'}
          onClick={() => setActiveSubTab('events')}
          style={{ padding: '10px 20px', fontSize: '0.9rem' }}
        >
          My Events ({myEvents.length})
        </Button>
        <Button
          variant={activeSubTab === 'analytics' ? 'primary' : 'secondary'}
          onClick={() => setActiveSubTab('analytics')}
          style={{ padding: '10px 20px', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <BarChart3 size={16} />
          Revenue Analytics & Reports
        </Button>
        <Button
          variant={activeSubTab === 'create-event' ? 'primary' : 'secondary'}
          onClick={() => setActiveSubTab('create-event')}
          style={{ padding: '10px 20px', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <Plus size={16} />
          Create Event
        </Button>
        <Button
          variant={activeSubTab === 'create-venue' ? 'primary' : 'secondary'}
          onClick={() => setActiveSubTab('create-venue')}
          style={{ padding: '10px 20px', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <Plus size={16} />
          Create Venue
        </Button>
      </div>

      {/* SUBTAB 1: EVENTS LIST */}
      {activeSubTab === 'events' && (
        <div>
          {myEvents.length === 0 ? (
            <div className="glass-card" style={{ textAlign: 'center', padding: '64px 24px' }}>
              <Layout size={40} className="text-muted" style={{ marginBottom: '16px' }} />
              <h3>No Events Created</h3>
              <p className="text-muted" style={{ marginTop: '4px', marginBottom: '20px' }}>
                Get started by setting up a venue and designing a seating layout.
              </p>
              <Button onClick={() => setActiveSubTab('create-event')}>Create Your First Event</Button>
            </div>
          ) : (
            <div className="table-wrapper">
              <table className="custom-table">
                <thead>
                  <tr>
                    <th>Event Details</th>
                    <th>Venue</th>
                    <th>Date</th>
                    <th>Status</th>
                    <th>Availability</th>
                    <th style={{ textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {myEvents.map((evt) => (
                    <tr key={evt.id}>
                      <td>
                        <strong style={{ display: 'block', fontSize: '1.05rem' }}>{evt.title}</strong>
                        <span className="text-muted" style={{ fontSize: '0.8rem' }}>ID: #{evt.id}</span>
                      </td>
                      <td>
                        <span>{evt.venueName}</span>
                        <span className="text-muted" style={{ display: 'block', fontSize: '0.8rem' }}>{evt.venueCity}</span>
                      </td>
                      <td>{formatDate(evt.eventDate)}</td>
                      <td>
                        {evt.status === 'DRAFT' && <span className="badge badge-warning">Draft</span>}
                        {evt.status === 'PUBLISHED' && <span className="badge badge-success">Live</span>}
                        {evt.status === 'CANCELLED' && <span className="badge badge-danger">Cancelled</span>}
                        {evt.status === 'COMPLETED' && <span className="badge badge-secondary">Completed</span>}
                      </td>
                      <td>
                        {evt.status === 'DRAFT' ? (
                          <span style={{ color: 'var(--text-dark)', fontSize: '0.9rem' }}>Pending Publish</span>
                        ) : (
                          <span>{evt.availableSeats} / {evt.totalCapacity} left</span>
                        )}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                          {evt.status === 'DRAFT' && (
                            <Button
                              variant="accent"
                              style={{ padding: '6px 12px', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                              onClick={() => handlePublishEvent(evt.id)}
                            >
                              <Upload size={12} />
                              Publish
                            </Button>
                          )}
                          {evt.status === 'PUBLISHED' && (
                            <Button
                              variant="secondary"
                              style={{ padding: '6px 12px', fontSize: '0.8rem', color: 'var(--color-booked)', borderColor: 'var(--color-booked)' }}
                              onClick={() => handleCancelEvent(evt.id)}
                            >
                              Cancel Show
                            </Button>
                          )}
                          {(evt.status === 'PUBLISHED' || evt.status === 'COMPLETED') && (
                            <>
                              <Button
                                variant="secondary"
                                style={{ padding: '6px 12px', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                                onClick={() => handleOpenRoster(evt)}
                              >
                                <Users size={12} />
                                Roster
                              </Button>
                              <Button
                                variant="secondary"
                                style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                                onClick={() => navigate(`/event/${evt.id}`)}
                              >
                                View Map
                              </Button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* SUBTAB 2: REVENUE ANALYTICS & DASHBOARD */}
      {activeSubTab === 'analytics' && (
        <div>
          {loadingAnalytics ? (
            <div className="flex-center" style={{ minHeight: '300px' }}>
              <div className="animate-spin" style={{
                border: '3px solid var(--border-main)',
                borderTop: '3px solid var(--primary)',
                borderRadius: '50%',
                width: '32px',
                height: '32px',
                animation: 'spin 1s linear infinite'
              }}></div>
            </div>
          ) : !analyticsData ? (
            <div className="glass-card" style={{ textAlign: 'center', padding: '48px 24px' }}>
              <AlertCircle size={40} className="text-muted" style={{ marginBottom: '16px' }} />
              <h3>Analytics Unavailable</h3>
              <p className="text-muted" style={{ marginTop: '4px' }}>Could not fetch organiser sales data.</p>
            </div>
          ) : (
            <div>
              {/* Stat Metric Cards Grid */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                gap: '20px',
                marginBottom: '32px'
              }}>
                {/* Stat 1: Total Revenue */}
                <div className="glass-card" style={{ padding: '24px', borderLeft: '4px solid var(--color-available)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                    <span className="text-muted" style={{ fontSize: '0.85rem', fontWeight: 600 }}>GROSS REVENUE</span>
                    <DollarSign size={20} style={{ color: 'var(--color-available)' }} />
                  </div>
                  <div className="mono-text" style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--text-main)' }}>
                    ₹{analyticsData.totalRevenue ? analyticsData.totalRevenue.toFixed(2) : '0.00'}
                  </div>
                  <span className="text-muted" style={{ fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                    From confirmed ticket bookings
                  </span>
                </div>

                {/* Stat 2: Tickets Sold */}
                <div className="glass-card" style={{ padding: '24px', borderLeft: '4px solid var(--primary)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                    <span className="text-muted" style={{ fontSize: '0.85rem', fontWeight: 600 }}>TICKETS SOLD</span>
                    <Ticket size={20} style={{ color: 'var(--primary)' }} />
                  </div>
                  <div className="mono-text" style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--text-main)' }}>
                    {analyticsData.totalTicketsSold || 0} <span style={{ fontSize: '1rem', color: 'var(--text-muted)' }}>Seats</span>
                  </div>
                  <span className="text-muted" style={{ fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                    Across {analyticsData.totalEvents || 0} events
                  </span>
                </div>

                {/* Stat 3: Occupancy Rate */}
                <div className="glass-card" style={{ padding: '24px', borderLeft: '4px solid var(--accent)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                    <span className="text-muted" style={{ fontSize: '0.85rem', fontWeight: 600 }}>AVG OCCUPANCY</span>
                    <TrendingUp size={20} style={{ color: 'var(--accent)' }} />
                  </div>
                  <div className="mono-text" style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--text-main)' }}>
                    {analyticsData.averageOccupancyRate ? analyticsData.averageOccupancyRate.toFixed(1) : '0.0'}%
                  </div>
                  <span className="text-muted" style={{ fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                    Capacity utilization rate
                  </span>
                </div>

                {/* Stat 4: Waitlist Volume */}
                <div className="glass-card" style={{ padding: '24px', borderLeft: '4px solid var(--color-held)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                    <span className="text-muted" style={{ fontSize: '0.85rem', fontWeight: 600 }}>WAITLIST VOLUME</span>
                    <Users size={20} style={{ color: 'var(--color-held)' }} />
                  </div>
                  <div className="mono-text" style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--text-main)' }}>
                    {analyticsData.totalWaitlistCount || 0} <span style={{ fontSize: '1rem', color: 'var(--text-muted)' }}>Queued</span>
                  </div>
                  <span className="text-muted" style={{ fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                    Active queue demand
                  </span>
                </div>
              </div>

              {/* Detailed Per-Event Analytics Cards */}
              <h3 style={{ marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <PieChart size={20} className="text-primary" />
                Event Performance Breakdown
              </h3>

              {(!analyticsData.eventSummaries || analyticsData.eventSummaries.length === 0) ? (
                <div className="glass-card" style={{ textAlign: 'center', padding: '32px' }}>
                  <p className="text-muted">No published events found for analytics.</p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                  {analyticsData.eventSummaries.map((evt) => {
                    const occPct = evt.occupancyPercentage || 0;
                    let occColor = 'var(--color-booked)';
                    if (occPct >= 80) occColor = 'var(--color-available)';
                    else if (occPct >= 40) occColor = 'var(--color-held)';

                    return (
                      <div key={evt.eventId} className="glass-card" style={{ padding: '28px' }}>
                        <div style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'flex-start',
                          flexWrap: 'wrap',
                          gap: '16px',
                          marginBottom: '20px'
                        }}>
                          <div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '4px' }}>
                              <h3 style={{ fontSize: '1.3rem', margin: 0 }}>{evt.eventTitle}</h3>
                              {evt.status === 'PUBLISHED' && <span className="badge badge-success">Live</span>}
                              {evt.status === 'DRAFT' && <span className="badge badge-warning">Draft</span>}
                              {evt.status === 'COMPLETED' && <span className="badge badge-secondary">Completed</span>}
                            </div>
                            <div style={{ display: 'flex', gap: '16px', fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '6px' }}>
                              <span>Venue: <strong style={{ color: 'var(--text-main)' }}>{evt.venueName}</strong></span>
                              <span>Date: {formatDate(evt.eventDate)}</span>
                            </div>
                          </div>

                          <div style={{ textAlign: 'right', display: 'flex', alignItems: 'center', gap: '16px' }}>
                            <div>
                              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Event Revenue</span>
                              <span className="mono-text" style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--color-available)' }}>
                                ₹{evt.revenue ? evt.revenue.toFixed(2) : '0.00'}
                              </span>
                            </div>
                            <Button
                              variant="secondary"
                              style={{ padding: '8px 14px', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
                              onClick={() => handleOpenRoster(evt)}
                            >
                              <Users size={14} />
                              View Roster
                            </Button>
                          </div>
                        </div>

                        {/* Occupancy Progress Visualizer */}
                        <div style={{ marginBottom: '20px' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '6px' }}>
                            <span className="text-muted">
                              Seat Occupancy: <strong>{evt.bookedSeats}</strong> Booked / <strong>{evt.totalCapacity}</strong> Total
                              {evt.heldSeats > 0 && <span style={{ color: 'var(--color-held)', marginLeft: '8px' }}>({evt.heldSeats} Held)</span>}
                            </span>
                            <strong style={{ color: occColor }}>{occPct.toFixed(1)}% Occupied</strong>
                          </div>
                          
                          {/* Progress Bar Container */}
                          <div style={{
                            width: '100%',
                            height: '10px',
                            background: 'rgba(255, 255, 255, 0.08)',
                            borderRadius: '50px',
                            overflow: 'hidden',
                            position: 'relative'
                          }}>
                            <div style={{
                              height: '100%',
                              width: `${Math.min(occPct, 100)}%`,
                              background: occColor,
                              borderRadius: '50px',
                              transition: 'width 0.6s ease'
                            }} />
                          </div>
                        </div>

                        {/* Category Breakdown Split Chips */}
                        {evt.categoryBreakdown && evt.categoryBreakdown.length > 0 && (
                          <div>
                            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '8px', fontWeight: 600 }}>
                              SEAT CATEGORY PERFORMANCE
                            </span>
                            <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                              {evt.categoryBreakdown.map((cat, idx) => (
                                <div key={idx} style={{
                                  background: 'rgba(0, 0, 0, 0.25)',
                                  border: '1px solid var(--border-main)',
                                  padding: '10px 14px',
                                  borderRadius: '8px',
                                  flex: '1 1 180px',
                                  display: 'flex',
                                  flexDirection: 'column',
                                  gap: '4px'
                                }}>
                                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <span style={{ fontWeight: 700, fontSize: '0.85rem', color: 'var(--accent)' }}>
                                      {cat.category}
                                    </span>
                                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                                      {cat.bookedSeats} / {cat.totalSeats}
                                    </span>
                                  </div>
                                  <span className="mono-text" style={{ fontSize: '0.95rem', fontWeight: 700 }}>
                                    ₹{cat.revenueGenerated ? cat.revenueGenerated.toFixed(2) : '0.00'}
                                  </span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* SUBTAB 3: CREATE EVENT */}
      {activeSubTab === 'create-event' && (
        <div className="glass-card" style={{ maxWidth: '800px', margin: '0 auto' }}>
          <h3 style={{ marginBottom: '24px', borderBottom: '1px solid var(--border-main)', paddingBottom: '12px' }}>
            New Event Builder
          </h3>

          {venues.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '24px', color: 'var(--color-held)' }}>
              <AlertCircle size={24} style={{ marginBottom: '8px' }} />
              <p>You must create a Venue before you can set up an Event.</p>
              <Button style={{ marginTop: '16px' }} onClick={() => setActiveSubTab('create-venue')}>
                Go Create Venue
              </Button>
            </div>
          ) : (
            <form onSubmit={handleCreateEvent}>
              <div className="form-group">
                <label className="form-label">Event Title</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Shakespeare's Hamlet Live"
                  value={eventTitle}
                  onChange={(e) => setEventTitle(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea
                  className="form-input"
                  style={{ minHeight: '100px', resize: 'vertical' }}
                  placeholder="Enter details about the show, performers, or rules..."
                  value={eventDesc}
                  onChange={(e) => setEventDesc(e.target.value)}
                  maxLength={5000}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '20px' }}>
                <div className="form-group">
                  <label className="form-label">Event Date & Time</label>
                  <input
                    type="datetime-local"
                    className="form-input"
                    value={eventDate}
                    onChange={(e) => setEventDate(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Select Venue</label>
                  <select
                    className="form-select"
                    value={selectedVenueId}
                    onChange={(e) => setSelectedVenueId(e.target.value)}
                    required
                  >
                    <option value="">-- Select Venue --</option>
                    {venues.map((v) => (
                      <option key={v.id} value={v.id}>
                        {v.name} ({v.city}) — Capacity: {v.totalCapacity}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Seat Layout Builder */}
              <div style={{
                borderTop: '1px solid var(--border-main)',
                paddingTop: '20px',
                marginBottom: '24px'
              }}>
                <div className="flex-between" style={{ marginBottom: '16px' }}>
                  <div>
                    <h4 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Layout size={18} className="text-primary" />
                      Seat Layout & Pricing Grid
                    </h4>
                    <p className="text-muted" style={{ fontSize: '0.8rem', marginTop: '2px' }}>
                      Configure seat rows, pricing categories, and capacities.
                    </p>
                  </div>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    style={{ padding: '6px 12px', fontSize: '0.85rem' }}
                    onClick={handleAddLayoutRow}
                  >
                    Add Row/Zone
                  </button>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '20px' }}>
                  {seatLayout.map((row, index) => (
                    <div key={index} style={{
                      display: 'grid',
                      gridTemplateColumns: '2fr 3fr 3fr 3fr 1fr',
                      gap: '12px',
                      alignItems: 'center',
                      background: 'rgba(0, 0, 0, 0.1)',
                      padding: '12px',
                      borderRadius: '6px',
                      border: '1px solid var(--border-main)'
                    }}>
                      <div>
                        <span className="form-label" style={{ fontSize: '0.7rem' }}>Row Prefix</span>
                        <input
                          type="text"
                          className="form-input"
                          placeholder="e.g. A"
                          value={row.rowPrefix}
                          maxLength={5}
                          onChange={(e) => handleLayoutChange(index, 'rowPrefix', e.target.value)}
                          required
                        />
                      </div>

                      <div>
                        <span className="form-label" style={{ fontSize: '0.7rem' }}>Category</span>
                        <select
                          className="form-select"
                          value={row.category}
                          onChange={(e) => handleLayoutChange(index, 'category', e.target.value)}
                        >
                          <option value="GENERAL">GENERAL</option>
                          <option value="PREMIUM">PREMIUM</option>
                          <option value="VIP">VIP</option>
                        </select>
                      </div>

                      <div>
                        <span className="form-label" style={{ fontSize: '0.7rem' }}>Seat Quantity</span>
                        <input
                          type="number"
                          className="form-input"
                          min={1}
                          value={row.quantity}
                          onChange={(e) => handleLayoutChange(index, 'quantity', e.target.value)}
                          required
                        />
                      </div>

                      <div>
                        <span className="form-label" style={{ fontSize: '0.7rem' }}>Price (INR)</span>
                        <input
                          type="number"
                          className="form-input"
                          min={0.01}
                          step={0.01}
                          value={row.price}
                          onChange={(e) => handleLayoutChange(index, 'price', e.target.value)}
                          required
                        />
                      </div>

                      <div style={{ textAlign: 'center', marginTop: '16px' }}>
                        <button
                          type="button"
                          style={{ background: 'none', border: 'none', color: 'var(--color-booked)', cursor: 'pointer' }}
                          onClick={() => handleRemoveLayoutRow(index)}
                          disabled={seatLayout.length <= 1}
                        >
                          <X size={20} />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>

                {selectedVenueId && (
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 16px',
                    borderRadius: '6px',
                    fontSize: '0.9rem',
                    background: getLayoutTotalCapacity() === venues.find(v => v.id === Number(selectedVenueId))?.totalCapacity
                      ? 'rgba(16, 185, 129, 0.08)'
                      : 'rgba(245, 158, 11, 0.08)',
                    border: getLayoutTotalCapacity() === venues.find(v => v.id === Number(selectedVenueId))?.totalCapacity
                      ? '1px solid rgba(16, 185, 129, 0.2)'
                      : '1px solid rgba(245, 158, 11, 0.2)',
                    color: getLayoutTotalCapacity() === venues.find(v => v.id === Number(selectedVenueId))?.totalCapacity
                      ? 'var(--color-available)'
                      : 'var(--color-held)',
                    marginBottom: '20px'
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Info size={16} />
                      <span>
                        Seating Capacity: Defined <strong style={{ color: 'var(--text-main)' }}>{getLayoutTotalCapacity()}</strong> /{' '}
                        Required <strong style={{ color: 'var(--text-main)' }}>{venues.find(v => v.id === Number(selectedVenueId))?.totalCapacity}</strong>
                      </span>
                    </div>
                    {getLayoutTotalCapacity() === venues.find(v => v.id === Number(selectedVenueId))?.totalCapacity ? (
                      <span style={{ fontWeight: 700 }}>Matches!</span>
                    ) : (
                      <span style={{ fontWeight: 700 }}>Mismatch!</span>
                    )}
                  </div>
                )}
              </div>

              <div style={{ display: 'flex', gap: '16px', justifyContent: 'flex-end' }}>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setActiveSubTab('events')}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  loading={creatingEvent}
                >
                  Create Draft Event
                </Button>
              </div>
            </form>
          )}
        </div>
      )}

      {/* SUBTAB 4: CREATE VENUE */}
      {activeSubTab === 'create-venue' && (
        <div className="glass-card" style={{ maxWidth: '600px', margin: '0 auto' }}>
          <h3 style={{ marginBottom: '24px', borderBottom: '1px solid var(--border-main)', paddingBottom: '12px' }}>
            Register New Venue
          </h3>

          <form onSubmit={handleCreateVenue}>
            <div className="form-group">
              <label className="form-label">Venue Name</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. Royal Opera House"
                value={venueName}
                onChange={(e) => setVenueName(e.target.value)}
                required
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label">City</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Mumbai"
                  value={venueCity}
                  onChange={(e) => setVenueCity(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Total Seating Capacity</label>
                <input
                  type="number"
                  className="form-input"
                  min={1}
                  max={100000}
                  placeholder="e.g. 500"
                  value={venueCapacity}
                  onChange={(e) => setVenueCapacity(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '28px' }}>
              <label className="form-label">Full Address</label>
              <input
                type="text"
                className="form-input"
                placeholder="Street address, landmark details..."
                value={venueAddress}
                onChange={(e) => setVenueAddress(e.target.value)}
                required
              />
            </div>

            <div style={{ display: 'flex', gap: '16px', justifyContent: 'flex-end' }}>
              <Button
                type="button"
                variant="secondary"
                onClick={() => setActiveSubTab('events')}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                loading={creatingVenue}
              >
                Create Venue
              </Button>
            </div>
          </form>
        </div>
      )}

      {/* ATTENDEE ROSTER MODAL */}
      {showRosterModal && selectedEventForRoster && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '900px', width: '90%' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px' }}>
              <div>
                <h3 style={{ margin: 0 }}>Attendee Roster & Guest List</h3>
                <p className="text-muted" style={{ fontSize: '0.85rem', marginTop: '4px' }}>
                  {selectedEventForRoster.title || selectedEventForRoster.eventTitle}
                </p>
              </div>
              <button
                style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
                onClick={() => setShowRosterModal(false)}
              >
                <X size={24} />
              </button>
            </div>

            {/* Modal Toolbar */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '16px', marginBottom: '20px', flexWrap: 'wrap' }}>
              <div style={{ position: 'relative', flex: '1 1 300px' }}>
                <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  type="text"
                  className="form-input"
                  placeholder="Search by name, email, booking ref..."
                  style={{ paddingLeft: '38px', fontSize: '0.85rem' }}
                  value={attendeeSearch}
                  onChange={(e) => setAttendeeSearch(e.target.value)}
                />
              </div>

              <Button
                variant="primary"
                style={{ padding: '8px 16px', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
                onClick={() => exportAttendeesCSV(selectedEventForRoster.title || selectedEventForRoster.eventTitle, attendeeList)}
                disabled={attendeeList.length === 0}
              >
                <Download size={14} />
                Export CSV Report
              </Button>
            </div>

            {/* Roster Table */}
            {loadingAttendees ? (
              <div className="flex-center" style={{ minHeight: '200px' }}>
                <div className="animate-spin" style={{
                  border: '3px solid var(--border-main)',
                  borderTop: '3px solid var(--primary)',
                  borderRadius: '50%',
                  width: '28px',
                  height: '28px',
                  animation: 'spin 1s linear infinite'
                }}></div>
              </div>
            ) : filteredAttendees.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--text-muted)' }}>
                <Users size={32} style={{ marginBottom: '12px' }} />
                <p>No confirmed attendees found matching search filter.</p>
              </div>
            ) : (
              <div className="table-wrapper" style={{ maxHeight: '400px', overflowY: 'auto' }}>
                <table className="custom-table" style={{ fontSize: '0.85rem' }}>
                  <thead>
                    <tr>
                      <th>Booking Ref</th>
                      <th>Attendee</th>
                      <th>Email</th>
                      <th>Seat(s)</th>
                      <th>Amount Paid</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredAttendees.map((att) => (
                      <tr key={att.bookingId}>
                        <td>
                          <span className="mono-text" style={{ fontWeight: 700, color: 'var(--accent)' }}>
                            {att.bookingRef}
                          </span>
                        </td>
                        <td>
                          <strong style={{ display: 'block' }}>{att.userName}</strong>
                        </td>
                        <td>
                          <span className="text-muted">{att.userEmail}</span>
                        </td>
                        <td>
                          <span className="mono-text">{att.seatNumbers}</span>
                        </td>
                        <td>
                          <span className="mono-text">₹{att.totalAmount ? att.totalAmount.toFixed(2) : '0.00'}</span>
                        </td>
                        <td>
                          {att.status === 'CONFIRMED' ? (
                            <span className="badge badge-success">Confirmed</span>
                          ) : (
                            <span className="badge badge-warning">{att.status}</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default OrganiserDashboard;
