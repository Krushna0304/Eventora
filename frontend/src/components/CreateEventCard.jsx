import React, { useState } from 'react';
import './CreateEventCard.css';
import { useNavigate } from 'react-router-dom';
import { eventsAPI } from '../services/api';

const CreateEventCard = ({ event }) => {
  const navigate = useNavigate();
  const [form, setForm] = useState(event ? {
    ...event,
    startDate: event.startDate ? new Date(event.startDate).toISOString().slice(0, 16) : '',
    endDate: event.endDate ? new Date(event.endDate).toISOString().slice(0, 16) : '',
  } : {
    title: '',
    description: '',
    eventCategory: '',
    locationName: '',
    city: '',
    state: '',
    country: '',
    latitude: '',
    longitude: '',
    startDate: '',
    endDate: '',
    maxParticipants: '',
    price: '',
    imageUrl: '',
    tags: [],
    eventStatus: '',
    promotionSpend: '',
    socialMentions: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm(f => ({ ...f, [name]: value }));
  };

  const handleTagsChange = (index, value) => {
    const t = [...form.tags];
    t[index] = value;
    setForm(f => ({ ...f, tags: t }));
  };

  const addTag = () => {
    setForm(f => ({ ...f, tags: [...(f.tags || []), ''] }));
  };

  const removeTag = (index) => {
    const t = [...form.tags];
    t.splice(index, 1);
    setForm(f => ({ ...f, tags: t }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMessage('');

    const payload = {
      ...form,
      latitude: form.latitude ? Number(form.latitude) : undefined,
      longitude: form.longitude ? Number(form.longitude) : undefined,
      maxParticipants: form.maxParticipants ? Number(form.maxParticipants) : undefined,
      price: form.price ? Number(form.price) : undefined,
      promotionSpend: form.promotionSpend ? Number(form.promotionSpend) : undefined,
      socialMentions: form.socialMentions ? Number(form.socialMentions) : undefined,
      tags: Array.isArray(form.tags) ? form.tags.filter(Boolean) : undefined,
    };

    setLoading(true);
    try {
      let resultEvent;
      if (event?.id) {
        // Update existing event
        resultEvent = await eventsAPI.update(event.id, payload);
        setSuccessMessage('Event updated successfully');
      } else {
        // Create new event
        resultEvent = await eventsAPI.create(payload);
        setSuccessMessage('Event created successfully');
      }
      
      if (resultEvent?.id) navigate(`/organiser/events/${resultEvent.id}`);
      else navigate('/organiser');
    } catch (err) {
      console.error('Error creating event:', err);
      const msg = err?.response?.data?.message ?? err?.response?.data ?? err?.message ?? 'Failed to create event';
      setError(typeof msg === 'object' ? JSON.stringify(msg) : String(msg));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="create-event-root">
      <form className="create-event-form" onSubmit={handleSubmit}>
        <h3>{event ? 'Update Event Info' : 'Create Event'}</h3>

        {error && <div className="error-message">{error}</div>}
        {successMessage && <div className="success-message">{successMessage}</div>}

        {/* Title and Description */}
        <div className="form-row">
          <label>Title</label>
          <input name="title" value={form.title} onChange={handleChange} required />
        </div>

        <div className="form-row">
          <label>Description</label>
          <textarea name="description" value={form.description} onChange={handleChange} rows={4} />
        </div>

        {/* Category and Image */}
        <div className="form-row two-col">
          <div>
            <label>Category</label>
            <select name="eventCategory" value={form.eventCategory} onChange={handleChange} required>
              <option value="">Select category</option>
              <option value="EDUCATION">Education</option>
              <option value="HEALTH">Health</option>
              <option value="SPORTS">Sports</option>
              <option value="CULTURE">Culture</option>
              <option value="MUSIC">Music</option>
              <option value="COMMUNITY">Community</option>
              <option value="BUSINESS">Business</option>
              <option value="ENTERTAINMENT">Entertainment</option>
              <option value="OTHER">Other</option>
            </select>
          </div>

          <div>
            <label>Image URL</label>
            <input name="imageUrl" value={form.imageUrl} onChange={handleChange} />
          </div>
        </div>

        {/* Location */}
        <div className="form-row two-col">
          <div>
            <label>Location name</label>
            <input name="locationName" value={form.locationName} onChange={handleChange} />
          </div>
          <div>
            <label>City</label>
            <input name="city" value={form.city} onChange={handleChange} />
          </div>
        </div>

        <div className="form-row two-col">
          <div>
            <label>State</label>
            <input name="state" value={form.state} onChange={handleChange} />
          </div>
          <div>
            <label>Country</label>
            <input name="country" value={form.country} onChange={handleChange} />
          </div>
        </div>

        {/* Coordinates */}
        <div className="form-row two-col">
          <div>
            <label>Latitude</label>
            <input name="latitude" value={form.latitude} onChange={handleChange} />
          </div>
          <div>
            <label>Longitude</label>
            <input name="longitude" value={form.longitude} onChange={handleChange} />
          </div>
        </div>

        {/* Dates */}
        <div className="form-row two-col">
          <div>
            <label>Starts</label>
            <input type="datetime-local" name="startDate" value={form.startDate} onChange={handleChange} />
          </div>
          <div>
            <label>Ends</label>
            <input type="datetime-local" name="endDate" value={form.endDate} onChange={handleChange} />
          </div>
        </div>

        {/* Participants and Price */}
        <div className="form-row two-col">
          <div>
            <label>Max participants</label>
            <input name="maxParticipants" type="number" value={form.maxParticipants} onChange={handleChange} />
          </div>
          <div>
            <label>Price</label>
            <input name="price" type="number" step="0.01" value={form.price} onChange={handleChange} />
          </div>
        </div>

        {/* Tags */}
        <div className="form-row">
          <label>Tags</label>
          <div className="tags">
            {(form.tags || []).map((t, i) => (
              <div key={i} className="tag-item">
                <input value={t} onChange={(e) => handleTagsChange(i, e.target.value)} />
                <button type="button" onClick={() => removeTag(i)}>×</button>
              </div>
            ))}
            <button type="button" onClick={addTag}>+ Add Tag</button>
          </div>
        </div>

        {/* Status, Promotion, Mentions */}
        <div className="form-row two-col">
          <div>
            <label>Event status</label>
            <select name="eventStatus" value={form.eventStatus} onChange={handleChange} required>
              <option value="">Select status</option>
              <option value="UPCOMING">Upcoming</option>
              <option value="ONGOING">Ongoing</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
              <option value="DRAFT">Draft</option>
              <option value="SCHEDULED">Schedule</option>
            </select>
          </div>
          <div>
            <label>Promotion spend</label>
            <input name="promotionSpend" type="number" value={form.promotionSpend} onChange={handleChange} />
          </div>
        </div>

        <div className="form-row two-col">
          <div>
            <label>Social mentions</label>
            <input name="socialMentions" type="number" value={form.socialMentions} onChange={handleChange} />
          </div>
          <div style={{ display: 'flex', alignItems: 'end', justifyContent: 'flex-end', gap: '10px' }}>
            <button type="button" className="cancel-button" onClick={() => navigate('/organiser')} disabled={loading}>Cancel</button>
            <button type="submit" className="submit-button" disabled={loading}>
              {loading ? (event ? 'Updating...' : 'Creating...') : (event ? 'Confirm' : 'Create Event')}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default CreateEventCard;
