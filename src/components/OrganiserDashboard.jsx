import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';

import ProfileMenu from './ProfileMenu';
import EventCard from './EventCard';
import Dialog from './Dialog';
import Watchlist from './Watchlist';
import { authAPI, eventsAPI, mlAPI } from '../services/api';
import { STORAGE_KEYS, PAGINATION } from '../config/constants';
import './OrganiserDashboard.css';

const OrganiserDashboard = () => {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
  const [userInfo, setUserInfo] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [dialogProps, setDialogProps] = useState({ isOpen: false, message: '', mlResult: null });
  const [mlLoading, setMlLoading] = useState(false);

  useEffect(() => {
    const fetchUserInfo = async () => {
      const token = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
      if (!token) return;
      try {
        const data = await authAPI.getUserInfo();
        setUserInfo(data);
      } catch (err) {
        console.error('Failed to fetch user info:', err);
      }
    };
    fetchUserInfo();

    const fetchEvents = async () => {
      setLoading(true);
      setError('');
      try {
        const params = {
          eventName: search.trim(),
          page: PAGINATION.DEFAULT_PAGE,
          size: PAGINATION.DEFAULT_SIZE,
        };
        const data = await eventsAPI.getByOrganizer(params);
        setEvents(Array.isArray(data.content) ? data.content : []);
      } catch (err) {
        setError('Could not load your events.');
      } finally {
        setLoading(false);
      }
    };
    fetchEvents();
  }, [search]);

  const predictEvent = async (eventId) => {
    setMlLoading(true);
    setDialogProps({ isOpen: false, message: '', mlResult: null });
    try {
      const data = await mlAPI.predictEvent(eventId);
      setDialogProps({ isOpen: true, mlResult: data, message: '' });
    } catch (err) {
      console.error('Predict error', err);
      setDialogProps({
        isOpen: true,
        mlResult: null,
        message: err?.response?.data || err?.message || 'Prediction error',
      });
    } finally {
      setMlLoading(false);
    }
  };

  const getLatestPrediction = async (eventId) => {
    setMlLoading(true);
    setDialogProps({ isOpen: false, message: '', mlResult: null });
    try {
      const data = await mlAPI.getLatestPrediction(eventId);
      if (data) {
        setDialogProps({ isOpen: true, mlResult: data, message: '' });
      } else {
        setDialogProps({ isOpen: true, mlResult: null, message: 'No latest prediction available.' });
      }
    } catch (err) {
      console.error('Latest prediction error', err);
      setDialogProps({
        isOpen: true,
        mlResult: null,
        message: err?.response?.data || err?.message || 'Error fetching latest prediction',
      });
    } finally {
      setMlLoading(false);
    }
  };

  const getPredictionHistory = async (eventId) => {
    setMlLoading(true);
    setDialogProps({ isOpen: false, message: '', mlResult: null });
    try {
      const data = await mlAPI.getPredictionHistory(eventId);
      setDialogProps({ isOpen: true, mlResult: null, message: JSON.stringify(data, null, 2) });
    } catch (err) {
      console.error('History error', err);
      setDialogProps({
        isOpen: true,
        mlResult: null,
        message: err?.response?.data || err?.message || 'Error fetching prediction history',
      });
    } finally {
      setMlLoading(false);
    }
  };

  const checkMLHealth = async () => {
    setMlLoading(true);
    setDialogProps({ isOpen: false, message: '', mlResult: null });
    try {
      const data = await mlAPI.checkHealth();
      setDialogProps({ isOpen: true, mlResult: null, message: JSON.stringify(data, null, 2) });
    } catch (err) {
      console.error('ML health error', err);
      setDialogProps({
        isOpen: true,
        mlResult: null,
        message: err?.message || 'Error checking ML health',
      });
    } finally {
      setMlLoading(false);
    }
  };

  const getMLStats = async () => {
    setMlLoading(true);
    setDialogProps({ isOpen: false, message: '', mlResult: null });
    try {
      const data = await mlAPI.getStats();
      setDialogProps({ isOpen: true, mlResult: null, message: JSON.stringify(data, null, 2) });
    } catch (err) {
      console.error('ML stats error', err);
      setDialogProps({
        isOpen: true,
        mlResult: null,
        message: err?.message || 'Error fetching ML stats',
      });
    } finally {
      setMlLoading(false);
    }
  };

  const handleLaunchEvent = () => navigate('/create-event');
  const handleSwitchToParticipant = () => {
    setIsProfileMenuOpen(false);
    navigate('/home');
  };
  const handleLogout = () => {
    localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
    setIsProfileMenuOpen(false);
    navigate('/home');
    window.location.reload();
  };

  return (
    <div className="organiser-root">
      <header className="organiser-header glass-card">
        <h2 className="dashboard-title">🎯 Organiser Dashboard</h2>
        <div className="organiser-actions">
          <input
            className="search-input"
            placeholder="Search events..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <button className="launch-button" onClick={handleLaunchEvent}>+ Launch Event</button>
          <button className="ml-button" onClick={checkMLHealth} disabled={mlLoading}>ML Health</button>
          <button className="ml-button" onClick={getMLStats} disabled={mlLoading}>ML Stats</button>
          <button className="profile-button" onClick={() => setIsProfileMenuOpen(true)}>
            <div className="profile-icon">{userInfo?.displayName?.charAt(0)?.toUpperCase() || '?'}</div>
          </button>
        </div>
      </header>

      <main className="organiser-body">
        <Watchlist />
        <p className="welcome-text">Welcome back, <strong>{userInfo?.displayName || 'Organiser'}</strong>! Manage your events efficiently.</p>

        {loading && <div className="info">Loading your events...</div>}
        {error && <div className="error-message">{error}</div>}
        {!loading && !error && events.length === 0 && <div className="info">No events found.</div>}

          <div className="events-grid">
          {events.map(ev => (
            <div key={ev.id} className="event-with-actions glass-card">
              <div className="event-content">
                <EventCard event={ev} fromOrganiser={true} />
              </div>
              <div className="event-actions">
                <button
                  className="event-action predict"
                  onClick={() => predictEvent(ev.id)}
                  disabled={mlLoading}
                >
                  Predict
                </button>
                <button
                  className="event-action latest"
                  onClick={() => getLatestPrediction(ev.id)}
                  disabled={mlLoading}
                >
                  Latest
                </button>
                <button
                  className="event-action history"
                  onClick={() => getPredictionHistory(ev.id)}
                  disabled={mlLoading}
                >
                  History
                </button>
              </div>
            </div>
          ))}
        </div>
      </main>

      <Dialog
        isOpen={dialogProps.isOpen}
        message={dialogProps.message}
        mlResult={dialogProps.mlResult}
        onClose={() => setDialogProps({ isOpen: false, message: '', mlResult: null })}
      />

      <ProfileMenu
        isOpen={isProfileMenuOpen}
        onClose={() => setIsProfileMenuOpen(false)}
        userInfo={userInfo || { displayName: 'Organizer', email: '' }}
        onLogout={handleLogout}
        onSwitchToOrganizer={handleSwitchToParticipant}
        switchLabel="Switch to Participant"
      />
    </div>
  );
};

export default OrganiserDashboard;
