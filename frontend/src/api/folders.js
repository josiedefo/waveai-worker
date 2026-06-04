import axios from 'axios'

const api = axios.create({ baseURL: '/waveai/api' })

export function fetchFolders() {
  return api.get('/folders').then(res => res.data)
}
