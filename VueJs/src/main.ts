import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'

window.addEventListener('error', (e) => {
  document.body.innerHTML += `<div style="position:fixed;top:0;left:0;z-index:9999;background:red;color:white;padding:20px;font-family:sans-serif;">Error: ${e.message}</div>`;
});
window.addEventListener('unhandledrejection', (e) => {
  document.body.innerHTML += `<div style="position:fixed;top:50px;left:0;z-index:9999;background:red;color:white;padding:20px;font-family:sans-serif;">Promise Error: ${e.reason}</div>`;
});

createApp(App).use(router).mount('#app')
