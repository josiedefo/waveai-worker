<template>
  <div>
    <button class="back-btn" @click="$router.back()">← Back</button>

    <div v-if="loading" class="status">Loading session...</div>
    <div v-else-if="error" class="status error">{{ error }}</div>

    <div v-else class="detail">
      <div class="detail-header">
        <h2>{{ session.title || 'Untitled Session' }}</h2>
        <span class="badge" :class="session.type">{{ session.type }}</span>
      </div>

      <div class="meta-grid">
        <div class="meta-item">
          <span class="label">Date</span>
          <span>{{ formatDate(session.timestamp) }}</span>
        </div>
        <div class="meta-item">
          <span class="label">Time</span>
          <span>{{ formatTime(session.timestamp) }}</span>
        </div>
        <div v-if="session.durationSeconds" class="meta-item">
          <span class="label">Duration</span>
          <span>{{ formatDuration(session.durationSeconds) }}</span>
        </div>
        <div v-if="session.platform" class="meta-item">
          <span class="label">Platform</span>
          <span>{{ session.platform }}</span>
        </div>
        <div v-if="session.language" class="meta-item">
          <span class="label">Language</span>
          <span>{{ session.language }}</span>
        </div>
      </div>

      <div v-if="session.speakers && session.speakers.length" class="section">
        <h3>Speakers</h3>
        <div class="speakers">
          <span v-for="speaker in session.speakers" :key="speaker" class="speaker-chip">{{ speaker }}</span>
        </div>
      </div>

      <div v-if="session.summary" class="section">
        <h3>Summary</h3>
        <div class="summary" v-html="renderedSummary"></div>
      </div>

      <div v-if="session.notes" class="section">
        <h3>Notes</h3>
        <p class="notes">{{ session.notes }}</p>
      </div>

      <div v-if="session.sessionUrl" class="section">
        <a :href="session.sessionUrl" target="_blank" rel="noopener" class="wave-link">Open in Wave ↗</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { fetchSession } from '../api/sessions.js'

const route = useRoute()
const session = ref(null)
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    session.value = await fetchSession(route.params.id)
  } catch {
    error.value = 'Failed to load session details. Please try again later.'
  } finally {
    loading.value = false
  }
})

const renderedSummary = computed(() => {
  if (!session.value?.summary) return ''
  // Convert basic markdown to HTML (headings, bold, line breaks)
  return session.value.summary
    .replace(/^### (.+)$/gm, '<h4>$1</h4>')
    .replace(/^## (.+)$/gm, '<h3>$1</h3>')
    .replace(/^# (.+)$/gm, '<h2>$1</h2>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/\n{2,}/g, '<br><br>')
    .replace(/\n/g, '<br>')
})

function formatDate(ts) {
  return new Date(ts).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })
}

function formatTime(ts) {
  return new Date(ts).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}

function formatDuration(seconds) {
  const hrs = Math.floor(seconds / 3600)
  const mins = Math.floor((seconds % 3600) / 60)
  if (hrs > 0) return `${hrs}h ${mins}m`
  return `${mins}m`
}
</script>

<style scoped>
.back-btn {
  background: none;
  border: none;
  color: #6b7280;
  font-size: 0.95rem;
  cursor: pointer;
  padding: 0;
  margin-bottom: 1.5rem;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.back-btn:hover {
  color: #1a1a2e;
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

.detail {
  background: #fff;
  border-radius: 10px;
  padding: 2rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  gap: 1rem;
}

.detail-header h2 {
  font-size: 1.4rem;
  line-height: 1.3;
}

.badge {
  font-size: 0.75rem;
  padding: 0.2rem 0.6rem;
  border-radius: 12px;
  background: #e5e7eb;
  color: #374151;
  text-transform: uppercase;
  white-space: nowrap;
  flex-shrink: 0;
}

.badge.recording {
  background: #d1fae5;
  color: #065f46;
}

.meta-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 1.25rem;
  padding: 1rem 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 1.5rem;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #9ca3af;
}

.meta-item span:last-child {
  font-size: 0.95rem;
  color: #1a1a2e;
}

.section {
  margin-top: 1.5rem;
}

.section h3 {
  font-size: 0.95rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #9ca3af;
  margin-bottom: 0.75rem;
}

.speakers {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.speaker-chip {
  background: #f3f4f6;
  border-radius: 20px;
  padding: 0.25rem 0.75rem;
  font-size: 0.875rem;
  color: #374151;
}

.summary {
  font-size: 0.9rem;
  line-height: 1.7;
  color: #374151;
}

.summary :deep(h2),
.summary :deep(h3),
.summary :deep(h4) {
  margin-top: 1rem;
  margin-bottom: 0.25rem;
  color: #1a1a2e;
}

.summary :deep(li) {
  margin-left: 1.25rem;
  list-style: disc;
}

.notes {
  font-size: 0.9rem;
  line-height: 1.7;
  color: #374151;
  white-space: pre-wrap;
}

.wave-link {
  color: #2563eb;
  font-size: 0.9rem;
  text-decoration: none;
}

.wave-link:hover {
  text-decoration: underline;
}
</style>
