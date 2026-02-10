<template>
  <div class="card">
    <div class="card-header">
      <h3>{{ session.title || 'Untitled Session' }}</h3>
      <span class="badge" :class="session.type">{{ session.type }}</span>
    </div>
    <div class="meta">
      <span v-if="session.timestamp">{{ formatDate(session.timestamp) }}</span>
      <span v-if="session.durationSeconds">Duration: {{ formatDuration(session.durationSeconds) }}</span>
      <span v-if="session.platform">Platform: {{ session.platform }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  session: {
    type: Object,
    required: true
  }
})

function formatDate(dateStr) {
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatDuration(seconds) {
  const hrs = Math.floor(seconds / 3600)
  const mins = Math.floor((seconds % 3600) / 60)
  if (hrs > 0) return `${hrs}h ${mins}m`
  return `${mins}m`
}
</script>

<style scoped>
.card {
  background: #fff;
  border-radius: 8px;
  padding: 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: box-shadow 0.2s;
}

.card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}

h3 {
  font-size: 1.05rem;
  flex: 1;
}

.badge {
  font-size: 0.75rem;
  padding: 0.2rem 0.6rem;
  border-radius: 12px;
  background: #e5e7eb;
  color: #374151;
  text-transform: uppercase;
  white-space: nowrap;
  margin-left: 0.5rem;
}

.badge.recording {
  background: #d1fae5;
  color: #065f46;
}

.meta {
  font-size: 0.8rem;
  color: #9ca3af;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
</style>
