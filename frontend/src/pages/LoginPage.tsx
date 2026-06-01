import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/axios';
import { useAuthStore } from '../store/useAuthStore';
import BrandLogo from '../components/BrandLogo';

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { setAuth, isAuthenticated } = useAuthStore();
  const navigate = useNavigate();

  React.useEffect(() => {
    if (isAuthenticated) {
      navigate('/');
    }
  }, [isAuthenticated, navigate]);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await api.post('/login', { username, password });
      const { 
        accessToken, 
        refreshToken, 
        userId, 
        username: loggedInUsername, 
        role, 
        displayName,
        forcePasswordChange 
      } = response.data;
      
      const success = setAuth(accessToken, userId, loggedInUsername, role, displayName, refreshToken, forcePasswordChange);
      if (success) {
        if (forcePasswordChange) {
          navigate('/profile');
        } else {
          navigate('/');
        }
      } else {
        setError('Login failed: Authentication session could not be established.');
      }
    } catch (err: unknown) {
      console.error('Login failed', err);
      setError('Login failed. Check your credentials.');
    }
  };

  return (
    <div className="auth-container">
      <form className="auth-form" onSubmit={handleLogin}>
        <div className="auth-logo-wrapper">
          <BrandLogo size="lg" />
        </div>
        <h2>Login</h2>
        {error && <p className="auth-error">{error}</p>}
        <div className="form-field">
          <label htmlFor="username" className="form-label">Username</label>
          <input 
            id="username"
            type="text" 
            value={username} 
            onChange={(e) => setUsername(e.target.value)} 
            required 
            autoComplete="username"
            className="input"
          />
        </div>
        <div className="form-field">
          <label htmlFor="password" className="form-label">Password</label>
          <input 
            id="password"
            type="password" 
            value={password} 
            onChange={(e) => setPassword(e.target.value)} 
            required 
            autoComplete="current-password"
            className="input"
          />
        </div>
        <button className="btn btn-primary" type="submit">Login</button>
        
        <div className="auth-footer">
          <p>Don't have an account?</p>
          <Link 
            to="/register" 
            className="btn btn-ghost"
          >
            Create New Account
          </Link>
        </div>
      </form>
    </div>
  );
};

export default LoginPage;
