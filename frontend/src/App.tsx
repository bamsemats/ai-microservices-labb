import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/useAuthStore';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ChatPage from './pages/ChatPage';
import DiscoveryPage from './pages/DiscoveryPage';
import InsightsPage from './pages/InsightsPage';
import ProfilePage from './pages/ProfilePage';
import { useUIAdaptation } from './hooks/useUIAdaptation';
import { useKeyboardShortcuts } from './hooks/useKeyboardShortcuts';
import './index.css';

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, token } = useAuthStore();
  // We need both for a truly active session in this in-memory token setup
  return (isAuthenticated && token) ? <>{children}</> : <Navigate to="/login" />;
};

function App() {
  useUIAdaptation();
  useKeyboardShortcuts();
  const initialize = useAuthStore((state) => state.initialize);

  React.useEffect(() => {
    initialize();
  }, [initialize]);

  return (
    <div className="app-container">
      <Router>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route 
            path="/" 
            element={
              <ProtectedRoute>
                <ChatPage />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/explore" 
            element={
              <ProtectedRoute>
                <DiscoveryPage />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/insights" 
            element={
              <ProtectedRoute>
                <InsightsPage />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/profile" 
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            } 
          />
        </Routes>
      </Router>
    </div>
  );
}

export default App;
