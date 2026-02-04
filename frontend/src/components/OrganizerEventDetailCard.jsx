import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import './EventDetailCard.css';
import './OrganizerEventDetailCard.css';
import Dialog from './Dialog';
import { eventsAPI } from '../services/api';
import { STORAGE_KEYS } from '../config/constants';

const OrganizerEventDetailCard = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [dialogProps, setDialogProps] = useState({ isOpen: false, message: '' });
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    if (!id) return;
    fetchEvent(id);
  }, [id]);

  const fetchEvent = async (eventId) => {
    setLoading(true);
    setError('');
    try {
      const token = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
      if (!token) {
        navigate('/login');
        return;
      }
      
      const data = await eventsAPI.getById(eventId);
      setEvent(data);
    } catch (err) {
      console.error('Error loading event detail:', err?.message ?? err);
      const serverMsg = err?.response?.data?.message ?? err?.response?.data ?? err?.message ?? 'Could not load event.';
      setError(typeof serverMsg === 'object' ? JSON.stringify(serverMsg) : serverMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleSchedule = async () => {
    setProcessing(true);
    try {
      await eventsAPI.schedule(id);
      await fetchEvent(id);
      setDialogProps({
        isOpen: true,
        message: 'Event Scheduled successfully!'
      });
    } catch (err) {
      const msg = err?.response?.data ?? err?.message ?? 'Failed to Schedule event.';
      setDialogProps({
        isOpen: true,
        message: msg
      });
    } finally {
      setProcessing(false);
    }
  };

  const handleCancel = async () => {
    setProcessing(true);
    try {
      await eventsAPI.cancel(id);
      await fetchEvent(id);
      setDialogProps({
        isOpen: true,
        message: 'Event cancelled successfully!'
      });
    } catch (err) {
      const msg = err?.response?.data ?? err?.message ?? 'Failed to cancel event.';
      setDialogProps({
        isOpen: true,
        message: msg
      });
    } finally {
      setProcessing(false);
    }
  };

  const handleEdit = () => {
    navigate(`/edit-event/${id}`);
  };

  const formatDate = (dt) => {
    if (!dt) return 'TBA';
    try {
      return new Date(dt).toLocaleString();
    } catch {
      return dt;
    }
  };

  return (
    <div className="event-detail-root">
      <div className="detail-header">
        <Link to="/organiser" className="back-link">◀ Back to Dashboard</Link>
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

            <div className="organizer-actions">
              <button
                className="action-button schedule"
                onClick={handleSchedule}
                disabled={processing || event.eventStatus === 'SCHEDULED'}
              >
                {processing ? 'Processing...' : 'Schedule'}
              </button>
              <button
                className="action-button edit"
                onClick={handleEdit}
                disabled={processing || event.eventStatus === 'SCHEDULED'}
              >
                Edit
              </button>
              <button
                className="action-button cancel"
                onClick={() => setDialogProps({
                  isOpen: true,
                  message: 'Are you sure you want to cancel this event?',
                  isConfirm: true,
                  onConfirm: handleCancel
                })}
                disabled={processing || event.eventStatus === 'CANCELLED'}
              >
                Cancel Event
              </button>
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

export default OrganizerEventDetailCard;