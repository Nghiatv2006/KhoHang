<template>
  <div class="cherry-blossoms-container" aria-hidden="true">
    <div v-for="n in 80" :key="n" class="petal" :style="getPetalStyle()"></div>
  </div>
</template>

<script setup lang="ts">
const getPetalStyle = () => {
  const left = Math.random() * 100;
  const animationDuration = 6 + Math.random() * 8; // Faster fall
  const animationDelay = Math.random() * -15; 
  const size = 10 + Math.random() * 14; // Slightly larger
  const opacity = 0.85 + Math.random() * 0.15; // Much more opaque
  const rotateStart = Math.random() * 360;
  
  return {
    left: `${left}vw`,
    width: `${size}px`,
    height: `${size}px`,
    opacity: opacity,
    animationDuration: `${animationDuration}s`,
    animationDelay: `${animationDelay}s`,
    '--rot-start': `${rotateStart}deg`,
    '--rot-end': `${rotateStart + 360 + Math.random() * 720}deg`
  };
};
</script>

<style scoped>
.cherry-blossoms-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  pointer-events: none;
  z-index: 9999;
  overflow: hidden;
}

.petal {
  position: absolute;
  top: -20px;
  background: radial-gradient(circle at 30% 30%, #ffc2d1, #ff6b8b);
  border-radius: 2px 20px 2px 20px;
  box-shadow: 0 0 10px rgba(255,107,139,0.8); /* Glow effect for dark mode */
  animation: fall linear infinite;
  transform-origin: center center;
}

@keyframes fall {
  0% { transform: translateY(-20px) translateX(0) rotate(var(--rot-start)); }
  50% { transform: translateY(50vh) translateX(80px) rotate(calc(var(--rot-start) + 180deg)); }
  100% { transform: translateY(105vh) translateX(-50px) rotate(var(--rot-end)); }
}
</style>
