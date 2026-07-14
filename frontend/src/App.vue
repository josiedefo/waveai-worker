<template>
  <div class="app">
    <header>
      <div class="header-top">
        <div>
          <h1>WaveAI Sessions</h1>
          <p class="subtitle">Session data from WaveAI Note Taking</p>
        </div>
        <nav>
          <RouterLink to="/" class="nav-link">Sessions</RouterLink>
          <RouterLink to="/folders" class="nav-link">Folders</RouterLink>
        </nav>
      </div>
    </header>
    <main>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useSse } from './api/sse.js'

onMounted(() => {
  useSse() // opens the SSE connection once for the lifetime of the app
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: #f5f7fa;
  color: #1a1a2e;
}

.app {
  max-width: 960px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

header {
  margin-bottom: 2rem;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
}

h1 {
  font-size: 1.8rem;
  color: #1a1a2e;
}

.subtitle {
  color: #6b7280;
  margin-top: 0.25rem;
}

nav {
  display: flex;
  gap: 0.25rem;
}

.nav-link {
  padding: 0.4rem 0.9rem;
  border-radius: 6px;
  font-size: 0.9rem;
  text-decoration: none;
  color: #6b7280;
  transition: background 0.15s, color 0.15s;
}

.nav-link:hover {
  background: #f3f4f6;
  color: #1a1a2e;
}

.nav-link.router-link-active {
  background: #1a1a2e;
  color: #fff;
}
</style>
