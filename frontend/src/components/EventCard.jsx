import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import './EventCard.css';
import { eventsAPI } from '../services/api';


const EventCard = ({ event, liked, onLikeToggle, fromOrganiser = false, fromHomeView = '' }) => {
  const [likeLoading, setLikeLoading] = useState(false);
  if (!event) return null;
  const handleLikeToggle = async () => {
    setLikeLoading(true);
    try {
      await onLikeToggle(event.id, !liked);
    } finally {
      setLikeLoading(false);
    }
  };

  const {
    id,
    title,
    organizerName,
    eventCategory,
    city,
    state,
    country,
    locationName,
    eventStatus,
    startDate,
    participantCount,
    currentParticipants,
  } = event;

  const formattedDate = startDate ? new Date(startDate).toLocaleString() : 'TBA';
  const locationParts = [locationName, city, state, country].filter(Boolean);
  const locationLabel = locationParts.length ? locationParts.join(', ') : 'Hybrid / Online';
  const participantsRaw = currentParticipants ?? participantCount ?? 0;
  const participants = Number.isFinite(Number(participantsRaw)) ? Number(participantsRaw) : 0;
  const formattedParticipants = `${participants.toLocaleString()} attending`;
  const statusLabel = eventStatus || 'SCHEDULED';
  const statusClass = `status-${statusLabel.toLowerCase()}`;
  const footerCopy = fromOrganiser ? 'Monitor performance' : 'Secure your seat';

  let eventLink = fromOrganiser ? `/organiser/events/${id}` : `/events/${id}`;
  if (!fromOrganiser && fromHomeView) {
    // fromHomeView expected values: 'my' or 'all'
    eventLink += `?from=home&view=${encodeURIComponent(fromHomeView)}`;
  }

  return (
    <article className="event-card" key={id}>
      <div className="event-card__chips">
        <span className="event-pill event-pill--category">{eventCategory || 'General'}</span>
        <span className={`event-pill event-pill--status ${statusClass}`}>
          {statusLabel}
        </span>
        <button
          className={`event-like-btn${liked ? ' liked' : ''}`}
          onClick={handleLikeToggle}
          disabled={likeLoading}
          title={liked ? 'Unlike' : 'Like'}
          style={{ background: 'none', border: 'none', cursor: 'pointer', marginLeft: 8 }}
        >
          <span
            role="img"
            aria-label={liked ? 'liked' : 'like'}
            style={{ color: liked ? '#e25555' : '#aaa', fontSize: 20 }}
          >
            {liked ? '❤️' : '🤍'}
          </span>
        </button>
      </div>
      <Link className="event-card__title" to={eventLink}>
        {title}
      </Link>
      <p className="event-card__meta">
        <span>{organizerName || 'Event organiser'}</span>
        <span className="meta-dot" />
        <span>{locationLabel}</span>
      </p>
      <div className="event-card__body">
        <div>
          <p className="label">Starts</p>
          <p className="value">{formattedDate}</p>
        </div>
        <div>
          <p className="label">Attendees</p>
          <p className="value">{formattedParticipants}</p>
        </div>
      </div>
      <div className="event-card__footer">
        <p className="event-card__hint">{footerCopy}</p>
        <Link to={eventLink} className="event-card__cta">
          View details
        </Link>
      </div>
    </article>
  );
};

export default EventCard;
