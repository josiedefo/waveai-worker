import { describe, it, expect } from 'vitest'
import router from './index.js'
import SessionList from '../components/SessionList.vue'
import SessionDetail from '../views/SessionDetail.vue'
import FolderList from '../views/FolderList.vue'

// Exercises the vue-router 5 configuration. vue-router 4 -> 5 is a major
// upgrade, so these assertions pin down that the router still builds and
// resolves routes with the /waveai/ base under the new version.
describe('router', () => {
  it('uses the /waveai/ history base', () => {
    // createWebHistory('/waveai/') normalizes the base without the trailing slash
    expect(router.options.history.base).toBe('/waveai')
  })

  it('registers exactly the three app routes', () => {
    const paths = router.getRoutes().map((r) => r.path).sort()
    expect(paths).toEqual(['/', '/folders', '/session/:id'])
  })

  it('resolves the session list at /', () => {
    const resolved = router.resolve('/')
    expect(resolved.matched[0].components.default).toBe(SessionList)
  })

  it('resolves /session/:id with the id param and detail view', () => {
    const resolved = router.resolve('/session/abc123')
    expect(resolved.params.id).toBe('abc123')
    expect(resolved.matched[0].components.default).toBe(SessionDetail)
  })

  it('resolves /folders to the folder list view', () => {
    const resolved = router.resolve('/folders')
    expect(resolved.matched[0].components.default).toBe(FolderList)
  })
})
