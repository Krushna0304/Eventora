import React, { useEffect, useState } from 'react';
import { useParams, Link, useLocation } from 'react-router-dom';
import './EventDetailCard.css';
import Dialog from './Dialog';
import { eventsAPI, registrationsAPI } from '../services/api';
import { STORAGE_KEYS } from '../config/constants';

const EventDetailCard = () => {
  const { id } = useParams();
  const location = useLocation();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [dialogProps, setDialogProps] = useState({ isOpen: false, message: '' });
  const [registering, setRegistering] = useState(false);

  useEffect(() => {
    if (!id) return;
    fetchEvent(id);
  }, [id]);

  const fetchEvent = async (eventId) => {
    setLoading(true);
    setError('');
    try {
      const data = await eventsAPI.getById(eventId);
      setEvent(data);
    } catch (err) {
      console.error('Error loading event detail:', err?.message ?? err);
      let serverMsg = err?.response?.data?.message ?? err?.response?.data ?? err?.message ?? 'Could not load event.';
      if (typeof serverMsg === 'object') {
        try {
          serverMsg = JSON.stringify(serverMsg);
        } catch (e) {
          serverMsg = String(serverMsg);
        }
      }
      setError(serverMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleRegistration = async () => {
    const token = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
    if (!token) {
      window.location.href = '/login';
      return;
    }

    if (event.userRegistrationStatus === 'CANCELLED') {
      setDialogProps({
        isOpen: true,
        message: 'You can only register for an event once. This event was previously cancelled.'
      });
      return;
    }

    setRegistering(true);
    try {
      const response = await registrationsAPI.register(id);
      
      // Refresh event details to get updated registration status
      await fetchEvent(id);
      setDialogProps({
        isOpen: true,
        message: response || 'Successfully registered for the event!'
      });
    } catch (err) {
      console.error('Registration error:', err?.message ?? err);
      const msg = err?.response?.data ?? err?.message ?? 'Failed to register for the event.';
      setDialogProps({
        isOpen: true,
        message: msg
      });
    } finally {
      setRegistering(false);
    }
  };

  const handleUnregister = async () => {
    const token = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
    if (!token) return;

    setRegistering(true);
    try {
      const response = await registrationsAPI.cancel(id);
      
      // Refresh event details to get updated registration status
      await fetchEvent(id);
      setDialogProps({
        isOpen: true,
        message: response || 'Successfully cancelled registration'
      });
    } catch (err) {
      console.error('Unregistration error:', err?.message ?? err);
      const msg = err?.response?.data ?? err?.message ?? 'Failed to cancel registration.';
      setDialogProps({
        isOpen: true,
        message: msg
      });
    } finally {
      setRegistering(false);
    }
  };

  const getRegistrationButton = () => {
    if (!event) return null;

    const status = event.userRegistrationStatus || 'NONE';
    const eventState = (event.eventStatus || '').toUpperCase();

    // Only allow new registrations when the event is in SCHEDULED state.
    // If event is not SCHEDULED, show the event state as a disabled button.
    // Preserve cancel behavior for users who are already REGISTERED.
    if (eventState !== 'SCHEDULED') {
      if (status === 'REGISTERED') {
        return (
          <button 
            className="registration-button registered"
            onClick={() => setDialogProps({
              isOpen: true,
              message: 'Are you sure you want to cancel your registration for this event?',
              isConfirm: true,
              onConfirm: handleUnregister
            })}
            disabled={registering}
          >
            {registering ? 'Cancelling...' : 'Cancel Registration'}
          </button>
        );
      }

      if (status === 'CANCELLED') {
        return (
          <button 
            className="registration-button cancelled"
            onClick={() => setDialogProps({
              isOpen: true,
              message: 'You can only register for an event once. This event was previously cancelled.'
            })}
            disabled
          >
            Cancelled
          </button>
        );
      }

      const pretty = eventState ? eventState.charAt(0).toUpperCase() + eventState.slice(1).toLowerCase() : 'Unavailable';
      return (
        <button className="registration-button cancelled" disabled>
          {pretty}
        </button>
      );
    }

    // eventState is SCHEDULED -> follow the previous registration logic
    switch (status) {
      case 'REGISTERED':
        return (
          <button 
            className="registration-button registered"
            onClick={() => setDialogProps({
              isOpen: true,
              message: 'Are you sure you want to cancel your registration for this event?',
              isConfirm: true,
              onConfirm: handleUnregister
            })}
            disabled={registering}
          >
            {registering ? 'Cancelling...' : 'Cancel Registration'}
          </button>
        );
      case 'NONE':
        return (
          <button 
            className="registration-button register"
            onClick={handleRegistration}
            disabled={registering}
          >
            {registering ? 'Registering...' : 'Register'}
          </button>
        );
      case 'CANCELLED':
        return (
          <button 
            className="registration-button cancelled"
            onClick={() => setDialogProps({
              isOpen: true,
              message: 'You can only register for an event once. This event was previously cancelled.'
            })}
          >
            Cancelled
          </button>
        );
      default:
        return null;
    }
  };

  const formatDate = (dt) => {
    if (!dt) return 'TBA';
    try {
      return new Date(dt).toLocaleString();
    } catch {
      return dt;
    }
  };

  // Determine back target based on query params (or location.state in future)
  const qs = new URLSearchParams(location.search);
  const fromParam = qs.get('from');
  const viewParam = qs.get('view');

  let backTarget = '/home';
  if (fromParam === 'organiser') {
    backTarget = '/organiser';
  } else if (fromParam === 'home') {
    // If view is 'my' go back to home with view=my so Home can restore My Events
    if (viewParam === 'my') backTarget = '/home?view=my';
    else backTarget = '/home';
  }

  return (
    <div className="event-detail-root">
      <div className="detail-header">
        <Link to={backTarget} className="back-link">◀ Back</Link>
      </div>

      {loading && <div className="info">Loading event...</div>}
      {error && <div className="error-message">{error}</div>}

      {event && (
        <div className="event-detail">
          {event.imageUrl && (
            <div className="image-wrap">
              <img src={event.imageUrl} alt={event.title} />
            </div>
          )}

          <div className="detail-main">
            <h2 className="title">{event.title}</h2>
            <div className="meta-row">
              <div>Organizer: <strong>{event.organizerDisplayName || '—'}</strong></div>
              <div>Category: {event.eventCategory || '—'}</div>
              <div>Status: {event.eventStatus || '—'}</div>
            </div>

            <div className="location-row">
              <div>{event.locationName || ''}</div>
              <div>{[event.city, event.state, event.country].filter(Boolean).join(', ')}</div>
            </div>

            <div className="dates-row">
              <div>Starts: {formatDate(event.startDate)}</div>
              <div>Ends: {formatDate(event.endDate)}</div>
            </div>

            <div className="participants-row">
              <div>Participants: {event.currentParticipants ?? 0} / {event.maxParticipants ?? '—'}</div>
              <div>Price: {event.price != null ? Number(event.price).toLocaleString() : 'Free'}</div>
            </div>

            {event.tags && event.tags.length > 0 && (
              <div className="tags">
                {event.tags.map((t, i) => <span key={i} className="tag">{t}</span>)}
              </div>
            )}

            <div className="description">
              <h3>Description</h3>
              <p>{event.description || 'No description provided.'}</p>
            </div>

            <div className="registration-section">
              <div className="user-registration">
                <strong>Your status:</strong> {event.userRegistrationStatus ?? 'NOT_REGISTERED'}
              </div>
              {getRegistrationButton()}
            </div>
          </div>
        </div>
      )}
      
      <Dialog 
        isOpen={dialogProps.isOpen}
        message={dialogProps.message}
        onClose={() => setDialogProps({ isOpen: false, message: '', isConfirm: false })}
        onConfirm={dialogProps.onConfirm}
        isConfirm={dialogProps.isConfirm}
      />
    </div>
  );
};

export default EventDetailCard;
