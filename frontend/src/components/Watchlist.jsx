import React, { useEffect, useState } from 'react';
import { eventsAPI } from '../services/api';
import EventCard from './EventCard';


const Watchlist = () => {
  const [watchlist, setWatchlist] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchWatchlist = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await eventsAPI.getWatchlist();
        if (Array.isArray(data)) {
          setWatchlist(data);
        } else if (data && Array.isArray(data.events)) {
          setWatchlist(data.events);
        } else {
          setWatchlist([]);
        }
      } catch (err) {
        setError('Failed to load watchlist');
      } finally {
        setLoading(false);
      }
    };
    fetchWatchlist();
  }, []);

  // Like/unlike handler for watchlist
  const handleLikeToggle = async (eventId, newLiked) => {
    try {
      if (newLiked) {
        await eventsAPI.createLikeEvent(eventId);
      } else {
        await eventsAPI.deleteLikeEvent(eventId);
      }
      // Remove from watchlist if unliked
      if (!newLiked) {
        setWatchlist((prev) => prev.filter(ev => ev.id !== eventId));
      } else {
        setWatchlist((prev) => prev.map(ev => ev.id === eventId ? { ...ev, isLiked: true } : ev));
      }
    } catch (e) {
      // Optionally show error
    }
  };

  if (loading) return <div>Loading watchlist...</div>;
  if (error) return <div>{error}</div>;
  if (!watchlist.length) return <div>No events in your watchlist.</div>;

  return (
    <div className="watchlist-section">
      <h2>My Watchlist</h2>
      <div className="events-grid">
        {watchlist.map(event => (
          <EventCard
            key={event.id}
            event={event}
            liked={!!event.isLiked}
            onLikeToggle={handleLikeToggle}
          />
        ))}
      </div>
    </div>
  );
};

export default Watchlist;
