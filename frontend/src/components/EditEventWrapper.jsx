import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import CreateEventCard from './CreateEventCard';
import { eventsAPI } from '../services/api';
import { STORAGE_KEYS } from '../config/constants';

const EditEventWrapper = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchEvent = async () => {
      try {
        const token = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
        if (!token) {
          navigate('/login');
          return;
        }

        const data = await eventsAPI.getById(id);
        setEvent(data);
      } catch (err) {
        console.error('Error fetching event:', err);
        setError(err?.response?.data?.message || 'Failed to load event');
      } finally {
        setLoading(false);
      }
    };

    fetchEvent();
  }, [id, navigate]);

  if (loading) return <div className="info">Loading event details...</div>;
  if (error) return <div className="error-message">{error}</div>;
  if (!event) return <div className="error-message">Event not found</div>;

  return <CreateEventCard event={event} />;
};

export default EditEventWrapper;