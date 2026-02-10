import axios from 'axios'

const api = axios.create({
  baseURL: '/api'
})

export function fetchSessions() {
  return api.get('/sessions').then(res => res.data)
}
