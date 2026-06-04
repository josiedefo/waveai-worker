<template>
  <div>
    <div v-if="loading" class="status">Loading folders...</div>
    <div v-else-if="error" class="status error">{{ error }}</div>
    <div v-else-if="folders.length === 0" class="status">No folders found.</div>
    <div v-else class="folder-grid">
      <div
        v-for="folder in folders"
        :key="folder.id"
        class="folder-card"
        :style="{ borderLeftColor: folder.color || '#e5e7eb' }"
      >
        <div class="folder-icon" :style="{ background: folder.color || '#e5e7eb' }">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="white">
            <path d="M10 4H4a2 2 0 00-2 2v12a2 2 0 002 2h16a2 2 0 002-2V8a2 2 0 00-2-2h-8l-2-2z"/>
          </svg>
        </div>
        <div class="folder-info">
          <span class="folder-name">{{ folder.name }}</span>
          <span class="folder-count">{{ folder.sessionCount }} session{{ folder.sessionCount !== 1 ? 's' : '' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchFolders } from '../api/folders.js'

const folders = ref([])
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    folders.value = await fetchFolders()
  } catch {
    error.value = 'Failed to load folders. Please try again later.'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.status {
  text-align: center;
  padding: 3rem;
  color: #6b7280;
  font-size: 1.1rem;
}

.error {
  color: #dc2626;
}

.folder-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 1rem;
}

.folder-card {
  background: #fff;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border-left: 4px solid;
  display: flex;
  align-items: center;
  gap: 1rem;
  transition: box-shadow 0.2s, transform 0.15s;
}

.folder-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.folder-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.folder-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
}

.folder-name {
  font-size: 0.95rem;
  font-weight: 500;
  color: #1a1a2e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.folder-count {
  font-size: 0.78rem;
  color: #9ca3af;
}
</style>
