<script lang="ts" setup>

</script>

<template>
  <!-- 公共头 -->
  <header id="header">
    <section class="container">
      <h1 id="logo">
        <a href="/" title="谷粒学院">
          <img src="../assets/img/logo.png" alt="谷粒学院">
        </a>
      </h1>
      <div>
        <ul class="nav">
          <router-link to="/" custom v-slot="{ isActive,isExactActive,navigate }">
            <li @click="navigate" :class="{'current':isActive}">
              <a>首页</a>
            </li>
          </router-link>
          <router-link to="/orderList" custom v-slot="{ isActive,isExactActive,navigate }">
            <li @click="navigate" :class="{'current':isActive}">
              <a>我的订单</a>
            </li>
          </router-link>
          <router-link to="/download" custom v-slot="{ isActive,isExactActive,navigate }">
            <li @click="navigate" :class="{'current':isActive}">
              <a>下载账单</a>
            </li>
          </router-link>
        </ul>
      </div>
      <div class="clear"></div>
    </section>
  </header>
</template>

<style scoped>

</style>