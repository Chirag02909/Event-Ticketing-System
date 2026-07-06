import React, { useState, useEffect } from 'react';
import api from '../services/api';
import Button from '../components/Button';
import { Shield, Users, Calendar, Trash2, UserCog, AlertCircle, RefreshCw, Search } from 'lucide-react';

export const AdminDashboard = () => {
  const [users, setUsers] = useState([]);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Navigation states
  const [activeSubTab, setActiveSubTab] = useState('users'); // 'users' | 'events'
  
  // Filter states
  const [userRoleFilter, setUserRoleFilter] = useState(''); // '' | 'USER' | 'ORGANISER'
  const [userSearchQuery, setUserSearchQuery] = useState('');
  const [eventStatusFilter, setEventStatusFilter] = useState(''); // '' | 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED'

  const fetchUsers = async () => {
    try {
      const url = userRoleFilter ? `/admin/users?role=${userRoleFilter}` : '/admin/users';
      const res = await api.get(url);
      setUsers(res.data || []);
    } catch (err) {
      console.error('Error fetching admin users:', err);
      setError('Failed to fetch user list.');
    }
  };

  const fetchEvents = async () => {
    try {
      const url = eventStatusFilter ? `/admin/events?status=${eventStatusFilter}` : '/admin/events';
      const res = await api.get(url);
      setEvents(res.data || []);
    } catch (err) {
      console.error('Error fetching admin events:', err);
      setError('Failed to fetch events list.');
    }
  };

  const fetchData = async () => {
    setLoading(true);
    setError('');
    await Promise.all([fetchUsers(), fetchEvents()]);
    setLoading(false);
  };

  useEffect(() => {
    fetchData();
  }, [userRoleFilter, eventStatusFilter]);

  const handleChangeRole = async (userId, currentRole) => {
    const nextRole = currentRole === 'USER' ? 'ORGANISER' : 'USER';
    if (window.confirm(`Are you sure you want to change this user's role to ${nextRole}?`)) {
      setLoading(true);
      try {
        await api.patch(`/admin/users/${userId}/role`, { role: nextRole });
        alert('User role updated successfully.');
        await fetchUsers();
      } catch (err) {
        console.error('Error updating user role:', err);
        alert(err.response?.data?.message || 'Failed to update role.');
      } finally {
        setLoading(false);
      }
    }
  };

  const handleDeleteUser = async (userId, username) => {
    if (window.confirm(`WARNING: Are you sure you want to permanently delete the account of "${username}"? This will erase their user profile. This action cannot be undone.`)) {
      setLoading(true);
      try {
        await api.delete(`/admin/users/${userId}`);
        alert('User account deleted successfully.');
        await fetchUsers();
      } catch (err) {
        console.error('Error deleting user:', err);
        alert(err.response?.data?.message || 'Failed to delete user.');
      } finally {
        setLoading(false);
      }
    }
  };

  const handleCancelEvent = async (eventId) => {
    if (window.confirm('Are you sure you want to cancel this event? This will release all holds and refund all bookings.')) {
      setLoading(true);
      try {
        await api.patch(`/organiser/events/${eventId}/cancel`);
        alert('Event cancelled successfully.');
        await fetchEvents();
      } catch (err) {
        console.error('Error cancelling event:', err);
        alert('Failed to cancel event.');
      } finally {
        setLoading(false);
      }
    }
  };

  // Filter users client-side by search query
  const filteredUsers = users.filter((u) => {
    return u.username?.toLowerCase().includes(userSearchQuery.toLowerCase()) ||
           u.email?.toLowerCase().includes(userSearchQuery.toLowerCase());
  });

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

  if (loading && users.length === 0 && events.length === 0) {
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
          <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Shield className="text-primary" />
            Admin Panel
          </h2>
          <p className="text-muted" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
            System-wide moderation of users, roles, and event schedules
          </p>
        </div>
        <button onClick={fetchData} className="btn btn-secondary" style={{ padding: '8px 12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
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

      {/* Navigation Subtabs */}
      <div style={{ display: 'flex', gap: '12px', marginBottom: '32px' }}>
        <Button
          variant={activeSubTab === 'users' ? 'primary' : 'secondary'}
          onClick={() => setActiveSubTab('users')}
          style={{ padding: '10px 20px', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <Users size={16} />
          Manage Users ({users.length})
        </Button>
        <Button
          variant={activeSubTab === 'events' ? 'primary' : 'secondary'}
          onClick={() => setActiveSubTab('events')}
          style={{ padding: '10px 20px', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <Calendar size={16} />
          Moderate Events ({events.length})
        </Button>
      </div>

      {/* SUBTAB 1: USERS */}
      {activeSubTab === 'users' && (
        <div>
          {/* Filters Bar */}
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '20px',
            flexWrap: 'wrap',
            gap: '12px'
          }}>
            <div style={{ position: 'relative', width: '260px' }}>
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
                placeholder="Search username or email..."
                value={userSearchQuery}
                onChange={(e) => setUserSearchQuery(e.target.value)}
              />
            </div>

            <select
              className="form-select"
              style={{ width: '180px', paddingTop: '8px', paddingBottom: '8px' }}
              value={userRoleFilter}
              onChange={(e) => setUserRoleFilter(e.target.value)}
            >
              <option value="">All Roles</option>
              <option value="USER">USER</option>
              <option value="ORGANISER">ORGANISER</option>
            </select>
          </div>

          <div className="table-wrapper">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>User Details</th>
                  <th>Email</th>
                  <th>Current Role</th>
                  <th>Created At</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '24px' }}>
                      No users found matching the filter.
                    </td>
                  </tr>
                ) : (
                  filteredUsers.map((u) => (
                    <tr key={u.id}>
                      <td>
                        <strong>{u.username}</strong>
                        <span className="text-muted" style={{ display: 'block', fontSize: '0.75rem' }}>ID: #{u.id}</span>
                      </td>
                      <td>{u.email}</td>
                      <td>
                        {u.role === 'ADMIN' && <span className="badge badge-primary">Admin</span>}
                        {u.role === 'ORGANISER' && <span className="badge badge-accent">Organiser</span>}
                        {u.role === 'USER' && <span className="badge badge-success">User</span>}
                      </td>
                      <td>{formatDate(u.createdAt)}</td>
                      <td>
                        {u.verified ? (
                          <span style={{ color: 'var(--color-available)', fontSize: '0.85rem' }}>Verified</span>
                        ) : (
                          <span style={{ color: 'var(--text-dark)', fontSize: '0.85rem' }}>Unverified</span>
                        )}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        {u.role !== 'ADMIN' && (
                          <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                            <Button
                              variant="secondary"
                              style={{ padding: '6px 10px', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                              onClick={() => handleChangeRole(u.id, u.role)}
                            >
                              <UserCog size={12} />
                              Toggle Role
                            </Button>
                            
                            <Button
                              variant="secondary"
                              style={{ padding: '6px 10px', fontSize: '0.75rem', color: 'var(--color-booked)', borderColor: 'rgba(239, 68, 68, 0.3)' }}
                              onClick={() => handleDeleteUser(u.id, u.username)}
                            >
                              <Trash2 size={12} />
                              Delete
                            </Button>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* SUBTAB 2: EVENTS */}
      {activeSubTab === 'events' && (
        <div>
          {/* Filters Bar */}
          <div style={{
            display: 'flex',
            justifyContent: 'flex-end',
            marginBottom: '20px'
          }}>
            <select
              className="form-select"
              style={{ width: '180px', paddingTop: '8px', paddingBottom: '8px' }}
              value={eventStatusFilter}
              onChange={(e) => setEventStatusFilter(e.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="DRAFT">DRAFT</option>
              <option value="PUBLISHED">PUBLISHED</option>
              <option value="CANCELLED">CANCELLED</option>
              <option value="COMPLETED">COMPLETED</option>
            </select>
          </div>

          <div className="table-wrapper">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Event Title</th>
                  <th>Organiser ID</th>
                  <th>Venue</th>
                  <th>Date</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {events.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '24px' }}>
                      No events found.
                    </td>
                  </tr>
                ) : (
                  events.map((evt) => (
                    <tr key={evt.id}>
                      <td>
                        <strong>{evt.title}</strong>
                        <span className="text-muted" style={{ display: 'block', fontSize: '0.75rem' }}>ID: #{evt.id}</span>
                      </td>
                      <td>Organiser #{evt.organiserId}</td>
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
                      <td style={{ textAlign: 'right' }}>
                        {evt.status === 'PUBLISHED' && (
                          <Button
                            variant="secondary"
                            style={{ padding: '6px 10px', fontSize: '0.75rem', color: 'var(--color-booked)', borderColor: 'rgba(239, 68, 68, 0.3)' }}
                            onClick={() => handleCancelEvent(evt.id)}
                          >
                            Cancel Event
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminDashboard;
