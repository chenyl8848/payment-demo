<script setup lang="ts">
// This starter template is using Vue 3 <script setup> SFCs
// Check out https://vuejs.org/api/sfc-script-setup.html#script-setup
import HeaderComponent from './components/AppHeader.vue'
import FooterComponent from "./components/AppFooter.vue";

import './assets/css/reset.css'
import './assets/css/theme.css'
import './assets/css/global.css'

</script>

<template>

  <!-- 公共头 -->
  <HeaderComponent/>

  <router-view></router-view>

  <!-- 公共底 -->
  <FooterComponent/>

</template>
