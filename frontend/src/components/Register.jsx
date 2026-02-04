import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import './AuthLayout.css';
import { Button } from '@mui/material';
import GoogleIcon from '@mui/icons-material/Google';
import GitHubIcon from '@mui/icons-material/GitHub';
import LinkedInIcon from '@mui/icons-material/LinkedIn';
import { initiateGoogleLogin, initiateGithubLogin, initiateLinkedInLogin } from '../utils/oauth';
import { authAPI } from '../services/api';
import { STORAGE_KEYS } from '../config/constants';

const differentiators = [
  'Instant organiser onboarding',
  'Insight-ready attendee analytics',
  'Built-in marketing journeys',
];

const Register = () => {
  const [formData, setFormData] = useState({ displayName: '', email: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const handleOAuthCallback = async () => {
      const params = new URLSearchParams(location.search);
      const code = params.get('code');
      if (!code) return;

      const pathParts = location.pathname.split('/');
      const provider = pathParts[2];
      if (!provider) return;

      setLoading(true);
      setError('');

      try {
        const response = await authAPI.oauthCallback(provider, code);
        const token = response?.token;
        if (token) {
          localStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, token);
          navigate('/home');
        } else {
          setError('Authentication failed. No token received.');
        }
      } catch (err) {
        console.error('OAuth callback error:', err);
        setError(err.response?.data?.message || 'OAuth authentication failed.');
      } finally {
        setLoading(false);
      }
    };

    handleOAuthCallback();
  }, [location, navigate]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await authAPI.register(formData);
      navigate('/login');
    } catch (err) {
      console.error('Registration error:', err);
      let msg = err?.response?.data?.message ?? '';
      const status = err?.response?.status;
      if (!msg) {
        if (status === 409) msg = 'User already exists. Try logging in or use a different email.';
        else if (status === 400) msg = 'Invalid registration data. Please check your inputs.';
        else msg = 'Registration failed. Please try again.';
      }
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <section className="auth-panel auth-panel__intro">
          <p className="auth-eyebrow">Join the community</p>
          <h1>Launch events that attendees remember.</h1>
          <p className="auth-lede">
            Eventora keeps registrations, CRM data, and promotion workflows in one sleek workspace.
          </p>
          <ul className="auth-benefits">
            {differentiators.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </section>

        <section className="auth-panel auth-panel__form">
          <h2>Create your seat</h2>
          <p className="auth-form-subtitle">
            Host experiences, accept registrations, and collaborate with co-organisers the smart way.
          </p>

          {error && <div className="error-message">{error}</div>}
          {loading && <div className="info">Processing...</div>}

          <form onSubmit={handleSubmit} className="auth-form">
            <div className="form-group">
              <label htmlFor="displayName">Display name</label>
              <input
                id="displayName"
                name="displayName"
                value={formData.displayName}
                onChange={handleChange}
                required
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleChange}
                required
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="password">Password</label>
              <div className="password-field">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  value={formData.password}
                  onChange={handleChange}
                  required
                  disabled={loading}
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowPassword((prev) => !prev)}
                  disabled={loading}
                >
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              </div>
            </div>

            <button type="submit" className="submit-button" disabled={loading}>
              {loading ? 'Registering...' : 'Create account'}
            </button>
          </form>

          <div className="social-login">
            <div className="divider"><span>OR</span></div>

            <Button
              variant="outlined"
              startIcon={<GoogleIcon />}
              onClick={initiateGoogleLogin}
              fullWidth
              className="social-button"
              disabled={loading}
            >
              Sign up with Google
            </Button>

            <Button
              variant="outlined"
              startIcon={<GitHubIcon />}
              onClick={initiateGithubLogin}
              fullWidth
              className="social-button"
              disabled={loading}
            >
              Sign up with GitHub
            </Button>

            <Button
              variant="outlined"
              startIcon={<LinkedInIcon />}
              onClick={initiateLinkedInLogin}
              fullWidth
              className="social-button"
              disabled={loading}
            >
              Sign up with LinkedIn
            </Button>
          </div>

          <p className="login-link">
            Already registered? <a href="/login">Return to sign in</a>
          </p>
        </section>
      </div>
    </div>
  );
};

export default Register;

