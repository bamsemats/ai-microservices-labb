import axios from 'axios';
import { useAuthStore } from '../store/useAuthStore';

const api = axios.create({
  baseURL: '/api', // Gateway URL, handled by proxy in dev or nginx in prod
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token && token !== 'undefined' && token !== 'null') {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
