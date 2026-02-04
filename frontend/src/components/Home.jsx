import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import EventCard from './EventCard';
import Watchlist from './Watchlist';
import Filters from './Filters';
import ProfileMenu from './ProfileMenu';
import { eventsAPI, authAPI, registrationsAPI } from '../services/api';
import { STORAGE_KEYS, PAGINATION } from '../config/constants';
import './Home.css';

const Home = () => {
  const [events, setEvents] = useState([]);
  const [filteredEvents, setFilteredEvents] = useState([]);
  const [search, setSearch] = useState('');
  const [organizerSearch, setOrganizerSearch] = useState('');
  const [isMyEventList, setIsMyEventList] = useState(false);
  const [showWatchlist, setShowWatchlist] = useState(false);
  const [loading, setLoading] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const [error, setError] = useState('');
  const [isLoggedIn, setIsLoggedIn] = useState(!!localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN));
  const [userInfo, setUserInfo] = useState(null);
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
  const [activeQuickFilter, setActiveQuickFilter] = useState('');
  const navigate = useNavigate();
  const location = useLocation();

  const highlightStats = useMemo(() => {
    const totalEvents = events.length;
    const attendeeCount = events.reduce((sum, ev) => {
      const current = ev?.currentParticipants ?? ev?.participantCount ?? 0;
      return sum + (Number.isFinite(current) ? Number(current) : 0);
    }, 0);
    const cityCount = new Set(events.map((ev) => ev?.city).filter(Boolean)).size;
    return [
      {
        label: 'Live experiences',
        value: totalEvents,
        detail: 'curated for this week',
      },
      {
        label: 'Attendees',
        value: attendeeCount,
        detail: 'already registered',
      },
      {
        label: 'Cities represented',
        value: cityCount,
        detail: 'across the community',
      },
    ];
  }, [events]);

  const quickFilters = useMemo(
    () => [
      { id: 'business', value: 'BUSINESS', label: 'Trending tech', detail: 'Product, SaaS, growth' },
      { id: 'culture', value: 'CULTURE', label: 'Culture & arts', detail: 'Museums, theatre, pop-ups' },
      { id: 'sports', value: 'SPORTS', label: 'Wellness & sports', detail: 'Outdoor runs, community yoga' },
      { id: 'community', value: 'COMMUNITY', label: 'Community impact', detail: 'Non-profits & local drives' },
    ],
    []
  );

  // update isLoggedIn if localStorage changes (e.g., login/logout in another tab)
  useEffect(() => {
    const onStorage = (e) => {
      if (e.key === STORAGE_KEYS.AUTH_TOKEN) {
        const newToken = e.newValue;
        setIsLoggedIn(!!newToken);
        if (!newToken) {
          setUserInfo(null);
        }
      }
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  // Fetch user info when logged in
  useEffect(() => {
    const fetchUserInfo = async () => {
      const token = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
      if (!token) return;

      try {
        const data = await authAPI.getUserInfo();
        setUserInfo(data);
      } catch (err) {
        console.error('Error fetching user info:', err?.message ?? err);
      }
    };

    if (isLoggedIn) {
      fetchUserInfo();
    }
  }, [isLoggedIn]);

  useEffect(() => {
    // On initial load, read query param to restore view if present
    const qs = new URLSearchParams(location.search);
    const view = qs.get('view');
      if (view === 'my') {
        setIsMyEventList(true);
        handleMyEvents();
      } else {
        setIsMyEventList(false);
        // Initial load: fetch recommendations instead of filtered events
        setLoading(true);
        eventsAPI.getRecommendations(10)
          .then((data) => {
            setEvents(Array.isArray(data) ? data : []);
            setFilteredEvents(Array.isArray(data) ? data : []);
          })
          .catch((err) => {
            setEvents([]);
            setFilteredEvents([]);
            setError('Could not load recommendations.');
          })
          .finally(() => setLoading(false));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const fetchSearchResults = async () => {
      if (!search.trim() && !organizerSearch.trim()) {
        setFilteredEvents(events);
        return;
      }

      setIsSearching(true);
      try {
        const params = {
          eventName: search.trim(),
          organizerName: organizerSearch.trim(),
          isMyEventList: isMyEventList,
          page: PAGINATION.DEFAULT_PAGE,
          size: PAGINATION.DEFAULT_SIZE,
        };

        const data = await eventsAPI.getByNameAndOrganizer(params);
        if (Array.isArray(data.content)) {
          setFilteredEvents(data.content);
        } else {
          setFilteredEvents([]);
        }
      } catch (err) {
        console.error('Search error:', err?.message ?? err);
        // Don't show error for search - just reset to all events
        setFilteredEvents(events);
      } finally {
        setIsSearching(false);
      }
    };

    // Debounce the search with 500ms delay
    const timeoutId = setTimeout(fetchSearchResults, 500);
    return () => clearTimeout(timeoutId);
  }, [search, organizerSearch, events]);

  const fetchEvents = async (filterPayload) => {
    setLoading(true);
    setError('');
    try {
      const data = await eventsAPI.getByFilter(filterPayload);
      
      if (Array.isArray(data)) {
        setEvents(data);
        setFilteredEvents(data);
      } else if (data && data.events) {
        setEvents(data.events);
        setFilteredEvents(data.events);
      } else {
        setEvents([]);
        setFilteredEvents([]);
      }
    } catch (err) {
      console.error('Error fetching events:', err?.message || err);
      let serverMsg = err?.response?.data?.message ?? err?.response?.data ?? err?.message ?? 'Could not load events.';
      if (typeof serverMsg === 'object') {
        try {
          serverMsg = JSON.stringify(serverMsg);
        } catch {
          serverMsg = String(serverMsg);
        }
      }
      setError(serverMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleQuickFilter = (category) => {
    setSearch('');
    setOrganizerSearch('');
    setIsMyEventList(false);

    if (activeQuickFilter === category) {
      setActiveQuickFilter('');
      fetchEvents({});
      return;
    }

    setActiveQuickFilter(category);
    fetchEvents({ eventCategory: category });
  };

  const handleApplyFilters = (filterPayload) => {
    setActiveQuickFilter('');
    fetchEvents(filterPayload);
  };

  const handleClearFilters = () => {
    setActiveQuickFilter('');
    fetchEvents({});
  };

  const handleMyEvents = async () => {
    const token = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
    if (!token) {
      setError('You must be logged in to view your events.');
      return;
    }

    setLoading(true);
    setError('');
    try {
      const data = await registrationsAPI.getMyEvents();
      
      if (Array.isArray(data)) {
        setEvents(data);
        setFilteredEvents(data);
      } else {
        setEvents(data?.events ?? []);
        setFilteredEvents(data?.events ?? []);
      }
    } catch (err) {
      console.error('Error loading my events:', err?.message ?? err);
      let serverMsg = err?.response?.data?.message ?? err?.response?.data ?? err?.message ?? 'Could not load my events.';
      if (typeof serverMsg === 'object') {
        try {
          serverMsg = JSON.stringify(serverMsg);
        } catch {
          serverMsg = String(serverMsg);
        }
      }
      setError(serverMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = () => {
    window.location.href = '/login';
  };

  const handleLogout = () => {
    localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
    setIsLoggedIn(false);
    setUserInfo(null);
    setIsProfileMenuOpen(false);
    window.location.href = '/home';
  };

  const handleSwitchToOrganizer = () => {
    // Navigate to organiser dashboard
    setIsProfileMenuOpen(false);
    navigate('/organiser');
  };

  // Like/unlike handler for all events
  const handleLikeToggle = async (eventId, newLiked) => {
    try {
      if (newLiked) {
        await eventsAPI.createLikeEvent(eventId);
      } else {
        await eventsAPI.deleteLikeEvent(eventId);
      }
      // Update liked state in all event lists
      setEvents((prev) => prev.map(ev => ev.id === eventId ? { ...ev, isLiked: newLiked } : ev));
      setFilteredEvents((prev) => prev.map(ev => ev.id === eventId ? { ...ev, isLiked: newLiked } : ev));
    } catch (e) {
      // Optionally show error
    }
  };

  return (
    <div className="home-container">
      <div className="home-header">
        <div className="home-hero">
          <div className="home-hero-copy">
            <p className="home-eyebrow">Curate · Host · Experience</p>
            <div className="home-hero-title">
              <h1>Eventora</h1>
              <p className="home-subtitle">
                Your modern control room for live experiences and audience engagement.
              </p>
            </div>
            <div className="home-hero-actions">
              <div className="view-toggle">
                <button 
                  className={`toggle-button ${!isMyEventList && !showWatchlist ? 'active' : ''}`} 
                  onClick={() => {
                    setIsMyEventList(false);
                    setShowWatchlist(false);
                    setSearch('');
                    setOrganizerSearch('');
                    fetchEvents({});
                  }}
                >
                  All Events
                </button>
                {isLoggedIn && (
                  <>
                    <button 
                      className={`toggle-button ${isMyEventList ? 'active' : ''}`}
                      onClick={() => {
                        setIsMyEventList(true);
                        setShowWatchlist(false);
                        setSearch('');
                        setOrganizerSearch('');
                        handleMyEvents();
                      }}
                    >
                      My Events
                    </button>
                    <button
                      className={`toggle-button ${showWatchlist ? 'active' : ''}`}
                      onClick={() => {
                        setIsMyEventList(false);
                        setShowWatchlist(true);
                      }}
                    >
                      Watchlist
                    </button>
                  </>
                )}
              </div>
              <p className="home-hero-note">
                Toggle views to jump between curated events and your private lineup.
              </p>
            </div>
          </div>
          <div className="home-stats">
            {highlightStats.map((stat) => (
              <div key={stat.label} className="home-stat-card">
                <span className="stat-value">
                  {typeof stat.value === 'number' ? stat.value.toLocaleString() : stat.value}
                </span>
                <span className="stat-label">{stat.label}</span>
                <span className="stat-detail">{stat.detail}</span>
              </div>
            ))}
          </div>
        </div>
        <div className="search-row">
          <div className="search-wrapper" style={{ display: 'flex', gap: 8 }}>
            <input
              className="search-input"
              placeholder="Search events by name"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ minWidth: 180 }}
            />
            <input
              className="search-input"
              placeholder="Organizer name (optional)"
              value={organizerSearch}
              onChange={(e) => setOrganizerSearch(e.target.value)}
              style={{ minWidth: 180 }}
            />
            {isSearching && <div className="search-spinner">Searching...</div>}
          </div>
          {isLoggedIn ? (
            <>
              <button 
                className="profile-button" 
                style={{ marginLeft: 8 }}
                onClick={() => setIsProfileMenuOpen(true)}
              >
                <div className="profile-icon">
                  {userInfo?.displayName?.charAt(0)?.toUpperCase() || '?'}
                </div>
              </button>
            </>
          ) : (
            <button style={{ marginLeft: 8 }} onClick={handleLogin}>Login</button>
          )}

          {isLoggedIn && (
            <ProfileMenu
              isOpen={isProfileMenuOpen}
              onClose={() => setIsProfileMenuOpen(false)}
              userInfo={userInfo || { displayName: 'User', email: 'Loading...' }}
              onLogout={handleLogout}
              onSwitchToOrganizer={handleSwitchToOrganizer}
              onMyEvents={handleMyEvents}
            />
          )}
        </div>

        <section className="home-quick-filters">
          <div className="quick-filters-head">
            <div>
              <p className="home-eyebrow">Curated views</p>
              <h3>Plan by vibe</h3>
            </div>
            <p className="home-hero-note">
              Tap a chip to instantly apply category filters. Tap again to reset.
            </p>
          </div>
          <div className="quick-chip-row">
            {quickFilters.map((chip) => (
              <button
                type="button"
                key={chip.id}
                className={`quick-chip ${activeQuickFilter === chip.value ? 'active' : ''}`}
                onClick={() => handleQuickFilter(chip.value)}
              >
                <span className="chip-label">{chip.label}</span>
                <span className="chip-desc">{chip.detail}</span>
              </button>
            ))}
          </div>
        </section>
      </div>

      <div className="home-body">
        {showWatchlist ? (
          <Watchlist />
        ) : (
          <>
            <aside className="home-filters">
              <h3>Filters</h3>
              <Filters onApply={handleApplyFilters} onClear={handleClearFilters} />
            </aside>

            <main className="home-list">
              {loading && <div className="info">Loading events...</div>}
              {error && <div className="error-message">{error}</div>}
              {!loading && filteredEvents.length === 0 && !error && <div className="info">No events found.</div>}

              <div className="events-grid">
                {filteredEvents.map((ev) => (
                  <EventCard
                    key={ev.id}
                    event={ev}
                    liked={!!ev.isLiked}
                    onLikeToggle={handleLikeToggle}
                    fromHomeView={isMyEventList ? 'my' : 'all'}
                  />
                ))}
              </div>
            </main>
          </>
        )}
      </div>
    </div>
  );
};

export default Home;
