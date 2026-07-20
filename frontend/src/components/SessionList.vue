<template>
  <div>
    <div class="list-toolbar">
      <span class="last-synced">{{ lastSyncedLabel }}</span>
      <span v-if="syncError" class="sync-error">{{ syncError }}</span>
      <button class="sync-btn" :disabled="syncing" @click="onSync">
        {{ syncing ? 'Syncing…' : 'Sync' }}
      </button>
    </div>

    <div v-if="loading" class="status">Loading sessions...</div>
    <div v-else-if="error" class="status error">{{ error }}</div>
    <div v-else-if="sessions.length === 0" class="status">
      No sessions found. Click Sync to fetch them from WaveAI.
    </div>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { fetchSessions } from '../api/sessions.js'
import { syncSessions, fetchSyncStatus, syncErrorMessage } from '../api/sync.js'
import { useSse } from '../api/sse.js'
import SessionCard from './SessionCard.vue'

const sessions = ref([])
const loading = ref(true)
const error = ref(null)

const syncing = ref(false)
const syncError = ref(null)
const lastSyncedAt = ref(null)
const now = ref(Date.now())
let clock = null

async function loadSessions() {
  try {
    sessions.value = await fetchSessions()
  } catch (err) {
    error.value = 'Failed to load sessions. Please try again later.'
    console.error(err)
  } finally {
    loading.value = false
  }
}

async function loadSyncStatus() {
  try {
    const status = await fetchSyncStatus()
    lastSyncedAt.value = status.sessions
  } catch {
    // last-synced label is informational only
  }
}

async function onSync() {
  if (syncing.value) return
  syncing.value = true
  syncError.value = null
  try {
    const result = await syncSessions()
    if (result.lastSyncedAt) lastSyncedAt.value = result.lastSyncedAt
    await loadSessions()
    error.value = null
  } catch (err) {
    syncError.value = syncErrorMessage(err)
  } finally {
    syncing.value = false
  }
}

const lastSyncedLabel = computed(() => {
  if (!lastSyncedAt.value) return 'Not synced yet'
  const seconds = Math.max(0, Math.floor((now.value - new Date(lastSyncedAt.value).getTime()) / 1000))
  if (seconds < 60) return 'Last synced just now'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `Last synced ${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `Last synced ${hours}h ago`
  return `Last synced ${Math.floor(hours / 24)}d ago`
})

const { on, off } = useSse()

function onSessionsUpdated() {
  fetchSessions().then(data => { sessions.value = data }).catch(() => {})
  loadSyncStatus()
}

onMounted(() => {
  loadSessions()
  loadSyncStatus()
  on('sessions-updated', onSessionsUpdated)
  clock = setInterval(() => { now.value = Date.now() }, 30000)
})

onUnmounted(() => {
  off('sessions-updated', onSessionsUpdated)
  clearInterval(clock)
})
</script>

<style scoped>
.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.last-synced {
  font-size: 0.85rem;
  color: #9ca3af;
}

.sync-error {
  font-size: 0.85rem;
  color: #dc2626;
}

.sync-btn {
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 0.4rem 1rem;
  font-size: 0.875rem;
  cursor: pointer;
}

.sync-btn:hover:not(:disabled) {
  background: #1d4ed8;
}

.sync-btn:disabled {
  background: #93c5fd;
  cursor: default;
}

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
