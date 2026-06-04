import { createRouter, createWebHistory } from 'vue-router'
import SessionList from '../components/SessionList.vue'
import SessionDetail from '../views/SessionDetail.vue'
import FolderList from '../views/FolderList.vue'

export default createRouter({
  history: createWebHistory('/waveai/'),
  routes: [
    { path: '/', component: SessionList },
    { path: '/session/:id', component: SessionDetail },
    { path: '/folders', component: FolderList }
  ]
})
