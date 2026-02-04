import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './AuthLayout.css';
import { Button } from '@mui/material';
import GoogleIcon from '@mui/icons-material/Google';
import GitHubIcon from '@mui/icons-material/GitHub';
import LinkedInIcon from '@mui/icons-material/LinkedIn';
import { initiateGoogleLogin, initiateGithubLogin, initiateLinkedInLogin } from '../utils/oauth';
import { authAPI } from '../services/api';
import { STORAGE_KEYS } from '../config/constants';

const Login = () => {
    const [formData, setFormData] = useState({ email: '', password: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();

    const spotlight = [
        'Real-time attendance dashboards',
        'One-click organiser workflows',
        'Enterprise-grade OAuth security'
    ];

    // Handle OAuth Callback
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const code = params.get('code');
        
        if (!code) return;

        // Determine provider from pathname (e.g., "/auth/google/callback")
        const pathMatch = location.pathname.match(/\/auth\/(\w+)\/callback/);
        
        if (!pathMatch) {
            console.error('Invalid OAuth callback path:', location.pathname);
            return;
        }

        const provider = pathMatch[1]; // Extract provider name (google, github, linkedin)

        // Prevent multiple calls by checking if already processing
        const isProcessing = sessionStorage.getItem(STORAGE_KEYS.OAUTH_PROCESSING);
        if (isProcessing === 'true') {
            console.log('OAuth already processing, skipping...');
            return;
        }

        const handleOAuthCallback = async () => {
            sessionStorage.setItem(STORAGE_KEYS.OAUTH_PROCESSING, 'true');
            setLoading(true);
            setError('');

            try {
                // Send code to backend - endpoint should match provider name
                const response = await authAPI.oauthCallback(provider, code);
                const token = response?.token;
                
                if (token) {
                    localStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, token);
                    sessionStorage.removeItem(STORAGE_KEYS.OAUTH_PROCESSING);
                    // Clear the OAuth params from URL before navigating
                    navigate('/home', { replace: true });
                } else {
                    setError('Authentication failed. No token received.');
                    sessionStorage.removeItem(STORAGE_KEYS.OAUTH_PROCESSING);
                    // Navigate back to login after 3 seconds
                    setTimeout(() => navigate('/login', { replace: true }), 3000);
                }
            } catch (err) {
                console.error('OAuth callback error:', err);
                const errorMessage = err.response?.data?.message || 
                                   err.message || 
                                   'OAuth authentication failed.';
                setError(errorMessage);
                sessionStorage.removeItem(STORAGE_KEYS.OAUTH_PROCESSING);
                // Navigate back to login after 3 seconds
                setTimeout(() => navigate('/login', { replace: true }), 3000);
            } finally {
                setLoading(false);
            }
        };

        handleOAuthCallback();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []); // Empty dependency array - only run once on mount



    
    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prevState => ({
            ...prevState,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const response = await authAPI.login(formData);
            const token = response?.token;
            if (token) {
                localStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, token);
                navigate('/home');
            } else {
                setError('Login failed. No token received.');
            }
        } catch (err) {
            console.error('Login error:', err);
            let serverMsg = err?.response?.data?.message ?? 'Login failed. Please try again.';
            setError(serverMsg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-card">
                <div className="auth-panel auth-panel__intro">
                    <p className="auth-eyebrow">Eventora OS</p>
                    <h1>Welcome back.</h1>
                    <p className="auth-lede">
                        Reconnect with your control centre for immersive events, unified guest data, and actionable insights.
                    </p>
                    <ul className="auth-benefits">
                        {spotlight.map((item) => (
                            <li key={item}>{item}</li>
                        ))}
                    </ul>
                </div>

                <div className="auth-panel auth-panel__form">
                    <h2>Sign in to continue</h2>
                    <p className="auth-form-subtitle">
                        Access organiser dashboards, forecasts, and attendee pipelines in one place.
                    </p>

                    {error && <div className="error-message">{error}</div>}
                    {loading && <div className="loading-message">Processing...</div>}

                    <form onSubmit={handleSubmit} className="auth-form">
                        <div className="form-group">
                            <label htmlFor="email">Email</label>
                            <input 
                                type="email" 
                                id="email" 
                                name="email" 
                                value={formData.email} 
                                onChange={handleChange} 
                                required 
                                disabled={loading}
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="password">Password</label>
                            <input 
                                type="password" 
                                id="password" 
                                name="password" 
                                value={formData.password} 
                                onChange={handleChange} 
                                required 
                                disabled={loading}
                            />
                        </div>

                        <button type="submit" className="submit-button" disabled={loading}>
                            {loading ? 'Logging in...' : 'Login'}
                        </button>
                    </form>

                    <div className="social-login">
                        <div className="divider"><span>Continue with</span></div>

                        <Button 
                            variant="outlined" 
                            startIcon={<GoogleIcon />} 
                            onClick={initiateGoogleLogin} 
                            fullWidth 
                            className="social-button"
                            disabled={loading}
                        >
                            Google
                        </Button>

                        <Button 
                            variant="outlined" 
                            startIcon={<GitHubIcon />} 
                            onClick={initiateGithubLogin} 
                            fullWidth 
                            className="social-button"
                            disabled={loading}
                        >
                            GitHub
                        </Button>

                        <Button 
                            variant="outlined" 
                            startIcon={<LinkedInIcon />} 
                            onClick={initiateLinkedInLogin} 
                            fullWidth 
                            className="social-button"
                            disabled={loading}
                        >
                            LinkedIn
                        </Button>
                    </div>

                    <p className="register-link">
                        Need an organiser seat? <a href="/register">Create an account</a>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default Login;