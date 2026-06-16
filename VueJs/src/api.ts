const BASE_URL = 'http://localhost:8080';

export const api = {
  async fetch(url: string, options: RequestInit = {}) {
    const fullUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`;
    const response = await fetch(fullUrl, {
      ...options,
      credentials: 'include', // Gửi cookie JWT tự động
    });
    if (response.status === 401) {
      window.dispatchEvent(new Event('auth-failed'));
    }
    return response;
  },

  async get(url: string, options: RequestInit = {}) {
    return this.fetch(url, { ...options, method: 'GET' });
  },

  async post(url: string, data: any, options: RequestInit = {}) {
    return this.fetch(url, {
      ...options,
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...options.headers },
      body: JSON.stringify(data),
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
