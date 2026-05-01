import axios from 'axios';

const api = axios.create({
  baseURL: '/api', // Gateway URL, handled by proxy in dev or nginx in prod
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer \${token}`;
  }
  return config;
});

export default api;
