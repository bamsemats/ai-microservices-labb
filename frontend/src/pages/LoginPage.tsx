import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { useAuthStore } from '../store/useAuthStore';
import logoWithName from '../assets/logo-with-name.png';

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const setAuth = useAuthStore((state) => state.setAuth);
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await api.post('/login', { username, password });
      const { accessToken, userId, username: loggedInUsername } = response.data;
      
      setAuth(accessToken, userId, loggedInUsername);
      navigate('/');
    } catch (err: unknown) {
      console.error('Login failed', err);
      setError('Login failed. Check your credentials.');
    }
  };

  return (
    <div className="auth-container">
      <form className="auth-form" onSubmit={handleLogin}>
        <div className="auth-logo-wrapper">
          <img src={logoWithName} alt="AdaptaChat" className="auth-logo" />
        </div>
        <h2>Login</h2>
        {error && <p className="error">{error}</p>}
        <div className="input-group">
          <label>Username</label>
          <input 
            type="text" 
            value={username} 
            onChange={(e) => setUsername(e.target.value)} 
            required 
          />
        </div>
        <div className="input-group">
          <label>Password</label>
          <input 
            type="password" 
            value={password} 
            onChange={(e) => setPassword(e.target.value)} 
            required 
          />
        </div>
        <button className="lumina-button" type="submit">Login</button>
        
        <div className="auth-footer">
          <p>Don't have an account?</p>
          <button 
            type="button" 
            className="lumina-button secondary" 
            onClick={() => navigate('/register')}
            style={{ width: '100%', marginTop: '0.5rem' }}
          >
            Create New Account
          </button>
        </div>
      </form>
    </div>
  );
};

export default LoginPage;
