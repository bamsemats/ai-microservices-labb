import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/axios';
import BrandLogo from '../components/BrandLogo';

const RegisterPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.post('/register', { username, password, email });
      setSuccess(true);
      setTimeout(() => navigate('/login'), 2000);
    } catch (err: unknown) {
      console.error('Registration failed', err);
      setError('Registration failed. Username might be taken.');
    }
  };

  return (
    <div className="auth-container">
      <form className="auth-form" onSubmit={handleRegister}>
        <div className="auth-logo-wrapper">
          <BrandLogo size="lg" />
        </div>
        <h2>Register</h2>
        {error && <p className="error">{error}</p>}
        {success && <p className="success">Registration successful! Redirecting to login...</p>}
        <div className="input-group">
          <label htmlFor="username">Username</label>
          <input 
            id="username"
            type="text" 
            value={username} 
            onChange={(e) => setUsername(e.target.value)} 
            required 
          />
        </div>
        <div className="input-group">
          <label htmlFor="email">Email</label>
          <input 
            id="email"
            type="email" 
            value={email} 
            onChange={(e) => setEmail(e.target.value)} 
            required 
          />
        </div>
        <div className="input-group">
          <label htmlFor="password">Password</label>
          <input 
            id="password"
            type="password" 
            value={password} 
            onChange={(e) => setPassword(e.target.value)} 
            required 
          />
        </div>
        <button className="lumina-button" type="submit">Register</button>
        <div className="auth-footer">
          <p>Already have an account?</p>
          <Link to="/login" className="lumina-button secondary auth-footer-cta">Login here</Link>
        </div>
      </form>
    </div>
  );
};


export default RegisterPage;
