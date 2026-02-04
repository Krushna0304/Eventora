import React, { useState } from 'react';
import './Filters.css';

const defaultFilter = {
  minPrice: '',
  maxPrice: '',
  city: '',
  state: '',
  country: '',
  eventCategory: '',
  isNearby: false,
  radiusInKm: '10',
  latitude: null,
  longitude: null,
};

const Filters = ({ onApply, onClear, initial = {} }) => {
  const [filters, setFilters] = useState({ ...defaultFilter, ...initial });
  const [locationError, setLocationError] = useState('');

  const getUserLocation = () => {
    if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setFilters(prev => ({
            ...prev,
            latitude: position.coords.latitude,
            longitude: position.coords.longitude
          }));
          setLocationError('');
        },
        (error) => {
          setLocationError('Could not get your location. Please enable location services.');
          setFilters(prev => ({ ...prev, isNearby: false }));
        }
      );
    } else {
      setLocationError('Geolocation is not supported by your browser');
      setFilters(prev => ({ ...prev, isNearby: false }));
    }
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    if (type === 'checkbox' && name === 'isNearby') {
      if (checked) {
        getUserLocation();
      } else {
        setFilters(f => ({
          ...f,
          isNearby: false,
          latitude: null,
          longitude: null
        }));
      }
    }
    setFilters((f) => ({ 
      ...f, 
      [name]: type === 'checkbox' ? checked : value 
    }));
  };

  const apply = (e) => {
    e.preventDefault();
    // Convert empty strings to undefined so backend can ignore
    const payload = {
      minPrice: filters.minPrice ? Number(filters.minPrice) : undefined,
      maxPrice: filters.maxPrice ? Number(filters.maxPrice) : undefined,
      city: filters.city || undefined,
      state: filters.state || undefined,
      country: filters.country || undefined,
      eventCategory: filters.eventCategory || undefined,
    };

    // Add location data if nearby events is enabled
    if (filters.isNearby && filters.latitude && filters.longitude) {
      payload.latitude = filters.latitude;
      payload.longitude = filters.longitude;
      payload.radiusInKm = filters.radiusInKm ? Number(filters.radiusInKm) : undefined;
    }

    onApply(payload);
  };

  const clear = () => {
    setFilters(defaultFilter);
    onClear();
  };

  return (
    <form className="filters-form" onSubmit={apply}>
      <div className="filters-grid">
        <label className="filters-field">
          <span>Min price</span>
          <input
            name="minPrice"
            type="number"
            step="0.01"
            placeholder="0"
            value={filters.minPrice}
            onChange={handleChange}
          />
        </label>
        <label className="filters-field">
          <span>Max price</span>
          <input
            name="maxPrice"
            type="number"
            step="0.01"
            placeholder="250"
            value={filters.maxPrice}
            onChange={handleChange}
          />
        </label>
        <label className="filters-field">
          <span>City</span>
          <input
            name="city"
            placeholder="City"
            value={filters.city}
            onChange={handleChange}
          />
        </label>
        <label className="filters-field">
          <span>State</span>
          <input
            name="state"
            placeholder="State"
            value={filters.state}
            onChange={handleChange}
          />
        </label>
        <label className="filters-field">
          <span>Country</span>
          <input
            name="country"
            placeholder="Country"
            value={filters.country}
            onChange={handleChange}
          />
        </label>
        <label className="filters-field">
          <span>Category</span>
          <select name="eventCategory" value={filters.eventCategory} onChange={handleChange}>
            <option value="">All categories</option>
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
        </label>
      </div>

      <div className="filters-row">
        <label className="filters-checkbox">
          <input
            type="checkbox"
            name="isNearby"
            checked={filters.isNearby}
            onChange={handleChange}
          />
          Show nearby events
        </label>
        {filters.isNearby && (
          <input
            className="radius-input"
            name="radiusInKm"
            type="number"
            placeholder="Radius in km"
            value={filters.radiusInKm}
            onChange={handleChange}
          />
        )}
        {locationError && (
          <span className="filters-error">{locationError}</span>
        )}
      </div>

      <div className="filters-actions">
        <button type="submit" className="filters-apply">Apply filters</button>
        <button type="button" className="filters-clear" onClick={clear}>Clear</button>
      </div>
    </form>
  );
};

export default Filters;
