import axios from 'axios'

const api = axios.create({
  baseURL: '/waveai/api'
})

export function fetchSessions() {
  return api.get('/sessions').then(res => res.data)
}

export function fetchSession(id) {
  return api.get(`/sessions/${id}`).then(res => res.data)
}

export function fetchTranscript(id) {
  return api.get(`/sessions/${id}/transcript`).then(res => res.data)
}
