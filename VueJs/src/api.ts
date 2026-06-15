export const api = {
  async fetch(url: string, options: RequestInit = {}) {
    const response = await fetch(url, options);
    if (response.status === 401) {
      // Trigger global event for 401 Unauthorized
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
      body: JSON.stringify(data)
    });
  },

  async put(url: string, data: any, options: RequestInit = {}) {
    return this.fetch(url, {
      ...options,
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...options.headers },
      body: JSON.stringify(data)
    });
  },

  async delete(url: string, options: RequestInit = {}) {
    return this.fetch(url, { ...options, method: 'DELETE' });
  }
};
