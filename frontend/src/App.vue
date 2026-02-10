<template>
  <div class="app">
    <header>
      <h1>WaveAI Sessions</h1>
      <p class="subtitle">Session data from WaveAI Note Taking</p>
    </header>
    <main>
      <SessionList />
    </main>
  </div>
</template>

<script setup>
import SessionList from './components/SessionList.vue'
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

h1 {
  font-size: 1.8rem;
  color: #1a1a2e;
}

.subtitle {
  color: #6b7280;
  margin-top: 0.25rem;
}
</style>
