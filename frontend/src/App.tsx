import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/useAuthStore';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ChatPage from './pages/ChatPage';
import DiscoveryPage from './pages/DiscoveryPage';
import InsightsPage from './pages/InsightsPage';
import ProfilePage from './pages/ProfilePage';
import FeedbackWidget from './components/FeedbackWidget';
import { useUIAdaptation } from './hooks/useUIAdaptation';
import { useKeyboardShortcuts } from './hooks/useKeyboardShortcuts';
import './index.css';
import {usePresenceStore} from "./store/usePresenceStore.ts";

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, token } = useAuthStore();
  // We need both for a truly active session in this in-memory token setup
  return (isAuthenticated && token) ? <>{children}</> : <Navigate to="/login" />;
};

function AppContent() {
  useKeyboardShortcuts();
  return (
    <div className="app-container">
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
      <FeedbackWidget />
    </div>
  );
}

function App() {
  useUIAdaptation();
  const initialize = useAuthStore((state) => state.initialize);
  const fetchPresences = usePresenceStore((state) => state.fetchPresences);
  const token = useAuthStore((state) => state.token);

  React.useEffect(() => {
    initialize();
  }, [initialize]);

  React.useEffect(() => {
    if (token) {
      fetchPresences(token);
    }
  }, [token, fetchPresences]);

  return <AppContent />;
}

export default App;
