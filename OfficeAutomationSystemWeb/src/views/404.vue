<script setup lang="ts"></script>

<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <router-link to="/">返回首页</router-link>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  gap: 12px;
  color: #333;
}

.not-found h1 {
  font-size: 72px;
  margin: 0;
  color: #1677ff;
}

.not-found p {
  font-size: 16px;
  color: #666;
}

.not-found a {
  color: #1677ff;
  text-decoration: none;
}

.not-found a:hover {
  text-decoration: underline;
}
</style>