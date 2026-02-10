<template>
  <div>
    <div v-if="loading" class="status">Loading sessions...</div>
    <div v-else-if="error" class="status error">{{ error }}</div>
    <div v-else-if="sessions.length === 0" class="status">No sessions found.</div>
    <div v-else class="session-grid">
      <SessionCard
        v-for="session in sessions"
        :key="session.id"
        :session="session"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchSessions } from '../api/sessions.js'
import SessionCard from './SessionCard.vue'

const sessions = ref([])
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    sessions.value = await fetchSessions()
  } catch (err) {
    error.value = 'Failed to load sessions. Please try again later.'
    console.error(err)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.session-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

.status {
  text-align: center;
  padding: 3rem;
  color: #6b7280;
  font-size: 1.1rem;
}

.error {
  color: #dc2626;
}
</style>
