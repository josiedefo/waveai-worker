import axios from 'axios'

const api = axios.create({
  baseURL: '/waveai/api'
})

export function syncSessions() {
  return api.post('/sync/sessions').then(res => res.data)
}

export function syncSession(id) {
  return api.post(`/sync/sessions/${id}`).then(res => res.data)
}

export function syncFolders() {
  return api.post('/sync/folders').then(res => res.data)
}

export function fetchSyncStatus() {
  return api.get('/sync/status').then(res => res.data)
}

export function syncErrorMessage(err) {
  if (err.response?.status === 429) {
    const seconds = err.response.data?.retryAfterSeconds
    return seconds
      ? `WaveAI rate limit reached — try again in ${seconds}s`
      : 'WaveAI rate limit reached — try again later'
  }
  return 'Sync failed. Please try again later.'
}
