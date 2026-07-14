import { defineConfig, mergeConfig } from 'vitest/config'
import viteConfig from './vite.config.js'

// Reuse the production vite config (notably the @vitejs/plugin-vue transform)
// and layer the test-only settings on top.
export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      globals: true,
      include: ['src/**/*.{test,spec}.{js,mjs,ts}']
    }
  })
)
