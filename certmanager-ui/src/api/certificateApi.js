import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  timeout: 30000,
})

/**
 * Read the token at request time directly from sessionStorage where
 * oidc-client-ts stores it. This avoids any timing/race-condition
 * between the auth useEffect and the first API call.
 *
 * oidc-client-ts key format: "oidc.user:<authority>:<clientId>"
 */
api.interceptors.request.use(config => {
  const oidcKey = Object.keys(sessionStorage).find(k => k.startsWith('oidc.user:'))
  if (oidcKey) {
    try {
      const user = JSON.parse(sessionStorage.getItem(oidcKey))
      if (user?.access_token) {
        config.headers.Authorization = `Bearer ${user.access_token}`
      }
    } catch (_) { /* ignore parse errors */ }
  }
  return config
})

export const certificateApi = {
  list:         ()          => api.get('/certificates').then(r => r.data),
  upload:       (file)      => { const f = new FormData(); f.append('file', file); return api.post('/certificates/upload', f).then(r => r.data) },
  fetchFromUrl: (url, port) => api.post('/certificates/fetch-url', { url, port: String(port) }).then(r => r.data),
  delete:       (id)        => api.delete(`/certificates/${id}`),
  downloadUrl:  (id)        => `${api.defaults.baseURL}/certificates/${id}/download`,
}
