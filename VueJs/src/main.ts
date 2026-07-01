import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'

window.addEventListener('error', (e) => {
  const div = document.createElement('div');
  div.style.cssText = 'position:fixed;top:0;left:0;z-index:9999;background:red;color:white;padding:20px;font-family:sans-serif;';
  div.innerText = `Error: ${e.message}`;
  document.body.appendChild(div);
  setTimeout(() => div.remove(), 5000);
});
window.addEventListener('unhandledrejection', (e) => {
  const div = document.createElement('div');
  div.style.cssText = 'position:fixed;top:50px;left:0;z-index:9999;background:red;color:white;padding:20px;font-family:sans-serif;';
  div.innerText = `Promise Error: ${e.reason}`;
  document.body.appendChild(div);
  setTimeout(() => div.remove(), 5000);
});

createApp(App).use(router).mount('#app')
