<script lang="ts" setup>

import {ref} from "vue";
import wxPayApi from "@/api/wxPay";

const billDate = ref('');
const disabledDate = (time: Date) => {
  const currentTime = new Date();
  const beforeDay = new Date(currentTime.getTime() - 24 * 60 * 60 * 1000);
  return time.getTime() > beforeDay.getTime();
}

const downloadBill = (type: string) => {
  wxPayApi.downloadBill(type, billDate.value).then((res) => {
    console.log(res, "rrrrrrrrr");
  }).catch((error) => {
    console.log(error)
  })
}

</script>

<template>
  <div class="bg-fa of">
    <section id="index" class="container">
      <header class="comm-title">
        <h2 class="fl tac">
          <span class="c-333">账单申请</span>
        </h2>
      </header>

      <el-form :inline="true">
        <el-form-item>
          <el-date-picker v-model="billDate" value-format="YYYY-MM-DD" :disabled-date="disabledDate"
                          placeholder="选择账单日期"/>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="downloadBill('tradebill')">下载交易账单</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="downloadBill('fundflowbill')">下载资金账单</el-button>
        </el-form-item>
      </el-form>
    </section>

  </div>
</template>

<style scoped>

</style>