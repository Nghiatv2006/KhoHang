import router from './router';

const BASE_URL = '';

export const api = {
  async fetch(url: string, options: RequestInit = {}) {
    const fullUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`;
    const response = await fetch(fullUrl, {
      ...options,
      credentials: 'include', // Gửi cookie JWT tự động
    });
    if (response.status === 401) {
      localStorage.removeItem('wh_user');
      router.push('/login');
      window.dispatchEvent(new Event('auth-failed'));
    }
    return response;
  },

  async get(url: string, options: RequestInit = {}) {
    return this.fetch(url, { ...options, method: 'GET', cache: 'no-store' });
  },

  async post(url: string, data: any, options: RequestInit = {}) {
    return this.fetch(url, {
      ...options,
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...options.headers },
      body: JSON.stringify(data),
    });
  },

  async upload(url: string, data: FormData, options: RequestInit = {}) {
    return this.fetch(url, {
      ...options,
      method: 'POST',
      body: data, // Browser automatically sets Content-Type to multipart/form-data with boundary
    });
  },

  async put(url: string, data: any, options: RequestInit = {}) {
    return this.fetch(url, {
      ...options,
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...options.headers },
      body: JSON.stringify(data),
    });
  },

  async patch(url: string, data: any, options: RequestInit = {}) {
    return this.fetch(url, {
      ...options,
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', ...options.headers },
      body: JSON.stringify(data),
    });
  },

  async delete(url: string, options: RequestInit = {}) {
    return this.fetch(url, { ...options, method: 'DELETE' });
  },
};
