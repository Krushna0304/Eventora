/**
 * OAuth utility functions for social authentication
 */

// OAuth client IDs from environment variables
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || "631122116715-qrjgivvn1d0bt2q88f69a7arnfln7jcd.apps.googleusercontent.com";
const GITHUB_CLIENT_ID = import.meta.env.VITE_GITHUB_CLIENT_ID || "YOUR_GITHUB_CLIENT_ID";
const LINKEDIN_CLIENT_ID = import.meta.env.VITE_LINKEDIN_CLIENT_ID || "YOUR_LINKEDIN_CLIENT_ID";
const FRONTEND_URL = import.meta.env.VITE_FRONTEND_URL || "http://localhost:5173";

export const initiateGoogleLogin = () => {
  const redirectUri = `${FRONTEND_URL}/auth/google/callback`;
  const scope = "openid email profile";
  const authUrl = new URL("https://accounts.google.com/o/oauth2/v2/auth");
  
  authUrl.searchParams.append("client_id", GOOGLE_CLIENT_ID);
  authUrl.searchParams.append("redirect_uri", redirectUri);
  authUrl.searchParams.append("response_type", "code");
  authUrl.searchParams.append("scope", scope);
  authUrl.searchParams.append("access_type", "offline");
  authUrl.searchParams.append("prompt", "consent");
  
  window.location.href = authUrl.toString();
};

export const initiateGithubLogin = () => {
  const redirectUri = `${FRONTEND_URL}/auth/github/callback`;
  const scope = "user:email read:user";
  const authUrl = new URL("https://github.com/login/oauth/authorize");
  
  authUrl.searchParams.append("client_id", GITHUB_CLIENT_ID);
  authUrl.searchParams.append("redirect_uri", redirectUri);
  authUrl.searchParams.append("scope", scope);
  
  window.location.href = authUrl.toString();
};

export const initiateLinkedInLogin = () => {
  const redirectUri = `${FRONTEND_URL}/auth/linkedin/callback`;
  const scope = "r_emailaddress r_liteprofile";
  const authUrl = new URL("https://www.linkedin.com/oauth/v2/authorization");
  
  authUrl.searchParams.append("response_type", "code");
  authUrl.searchParams.append("client_id", LINKEDIN_CLIENT_ID);
  authUrl.searchParams.append("redirect_uri", redirectUri);
  authUrl.searchParams.append("scope", scope);
  
  window.location.href = authUrl.toString();
};