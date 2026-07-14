import { describe, it, expect } from 'vitest'
import { mount, RouterLinkStub } from '@vue/test-utils'
import SessionCard from './SessionCard.vue'

// Component render tests. These exercise the vue 3.5 + @vitejs/plugin-vue 6
// SFC compile/mount path, plus the vue-router <RouterLink> integration.
function mountCard(session) {
  return mount(SessionCard, {
    props: { session },
    global: { stubs: { RouterLink: RouterLinkStub } }
  })
}

describe('SessionCard', () => {
  const base = {
    id: 's1',
    title: 'Weekly Standup',
    type: 'recording',
    platform: 'zoom',
    timestamp: '2026-01-15T10:00:00Z',
    durationSeconds: 3661
  }

  it('renders the session title', () => {
    const wrapper = mountCard(base)
    expect(wrapper.find('h3').text()).toBe('Weekly Standup')
  })

  it('falls back to "Untitled Session" when title is missing', () => {
    const wrapper = mountCard({ ...base, title: '' })
    expect(wrapper.find('h3').text()).toBe('Untitled Session')
  })

  it('renders the type badge', () => {
    const wrapper = mountCard(base)
    const badge = wrapper.find('.badge')
    expect(badge.text()).toBe('recording')
    expect(badge.classes()).toContain('recording')
  })

  it('formats the duration as hours and minutes', () => {
    const wrapper = mountCard(base) // 3661s -> 1h 1m
    expect(wrapper.text()).toContain('Duration: 1h 1m')
  })

  it('formats sub-hour durations as minutes only', () => {
    const wrapper = mountCard({ ...base, durationSeconds: 1800 }) // 30m
    expect(wrapper.text()).toContain('Duration: 30m')
  })

  it('links to the session detail route', () => {
    const wrapper = mountCard(base)
    expect(wrapper.getComponent(RouterLinkStub).props('to')).toBe('/session/s1')
  })

  it('omits the platform line when platform is absent', () => {
    const wrapper = mountCard({ ...base, platform: '' })
    expect(wrapper.text()).not.toContain('Platform:')
  })
})
