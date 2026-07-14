import { describe, it, expect, beforeEach, vi } from 'vitest'

// Hoisted so the vi.mock factory below can reference it.
const { mockGet } = vi.hoisted(() => ({ mockGet: vi.fn() }))

// Mock axios so we can assert on the requests the API client makes.
// axios was upgraded (1.7 -> 1.18); these tests confirm the client wiring
// (baseURL + endpoints + res.data unwrapping) still behaves.
vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({ get: mockGet }))
  }
}))

import axios from 'axios'
import { fetchSessions, fetchSession, fetchTranscript } from './sessions.js'

describe('sessions api client', () => {
  beforeEach(() => {
    mockGet.mockReset()
  })

  it('creates the axios instance with the /waveai/api baseURL', () => {
    expect(axios.create).toHaveBeenCalledWith({ baseURL: '/waveai/api' })
  })

  it('fetchSessions GETs /sessions and returns the response data', async () => {
    const data = [{ id: 's1' }, { id: 's2' }]
    mockGet.mockResolvedValue({ data })

    const result = await fetchSessions()

    expect(mockGet).toHaveBeenCalledWith('/sessions')
    expect(result).toBe(data)
  })

  it('fetchSession GETs /sessions/:id', async () => {
    mockGet.mockResolvedValue({ data: { id: 'abc' } })

    const result = await fetchSession('abc')

    expect(mockGet).toHaveBeenCalledWith('/sessions/abc')
    expect(result).toEqual({ id: 'abc' })
  })

  it('fetchTranscript GETs /sessions/:id/transcript', async () => {
    mockGet.mockResolvedValue({ data: { segments: [] } })

    const result = await fetchTranscript('abc')

    expect(mockGet).toHaveBeenCalledWith('/sessions/abc/transcript')
    expect(result).toEqual({ segments: [] })
  })
})
