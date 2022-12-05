<script lang="ts" setup>

import {onMounted, ref} from "vue";
import orderApi from "@/api/order";
import wxPayApi from "@/api/wxPay";
import {ElMessage, ElMessageBox} from "element-plus";


const list = ref<[]>();

const getData = () => {
  orderApi.list().then(res => {
    list.value = res.data.orderInfoList;
  }).catch((error) => {
    console.log(error)
  });
}

onMounted(() => {
  getData();
})

// 取消支付
const cancel = (orderNo: string) => {
  console.log("cancel", "取消订单", orderNo);
  ElMessageBox.confirm('确定取消订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    wxPayApi.nativeCancel(orderNo).then((res) => {
      if (res.code === 0) {
        ElMessage({
          type: 'success',
          message: '取消订单成功',
        })

        getData();
      }
    }).catch((error) => {

    });
  }).catch(() => {
  })


}
// 退款原因
const reason = ref<string>('');
// 确定按钮
const refundSubmitBtnDisabled = ref<boolean>(false)
// 对话框
let refundDialogVisible = ref<boolean>(false)
let refundOrderNo = '';

// 去退款
const refund = (orderNo: string) => {
  refundDialogVisible.value = true;
  reason.value = ''
  refundOrderNo = orderNo;
}


// 退款
const confirmRefund = () => {
  refundSubmitBtnDisabled.value = true;
  if (reason.value === '') {
    ElMessage.warning({
      message: '请选择退款原因!',
      duration: 3000
    });
    refundSubmitBtnDisabled.value = false;
    return;
  }

  wxPayApi.nativeRefund(refundOrderNo, reason.value).then((res) => {
    console.log(res, "Rrrrrrrrrrrrrrr");
    getData();
    refundSubmitBtnDisabled.value = false;
    refundDialogVisible.value = false;
  }).catch((error) => {
    console.log(error)
  })

}

// 关闭对话款
const closeDialog = () => {
  refundSubmitBtnDisabled.value = false;
}

</script>

<template>
  <div class="bg-fa of">
    <section id="index" class="container">
      <header class="comm-title">
        <h2 class="fl tac">
          <span class="c-333">订单列表</span>
        </h2>
      </header>
      <el-table :data="list" border style="width: 100%">
        <el-table-column type="index" width="50"></el-table-column>
        <el-table-column prop="orderNo" label="订单编号" width="230"></el-table-column>
        <el-table-column prop="title" label="订单标题"></el-table-column>
        <el-table-column prop="totalFee" label="订单金额">
          <template #default="scope">
            {{ scope.row.totalFee / 100 }} 元
          </template>
        </el-table-column>
        <el-table-column label="订单状态">
          <template #default="scope">
            <el-tag v-if="scope.row.orderStatus === '未支付'">
              {{ scope.row.orderStatus }}
            </el-tag>
            <el-tag v-if="scope.row.orderStatus === '支付成功'" type="success">
              {{ scope.row.orderStatus }}
            </el-tag>
            <el-tag v-if="scope.row.orderStatus === '超时已关闭'" type="warning">
              {{ scope.row.orderStatus }}
            </el-tag>
            <el-tag v-if="scope.row.orderStatus === '用户已取消'" type="info">
              {{ scope.row.orderStatus }}
            </el-tag>
            <el-tag v-if="scope.row.orderStatus === '退款中'" type="danger">
              {{ scope.row.orderStatus }}
            </el-tag>
            <el-tag v-if="scope.row.orderStatus === '已退款'" type="info">
              {{ scope.row.orderStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间"></el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="scope">
            <el-button v-if="scope.row.orderStatus === '未支付'" @click="cancel(scope.row.orderNo)">取消
            </el-button>
            <el-button v-if="scope.row.orderStatus === '支付成功'" @click="refund(scope.row.orderNo)">退款
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <!-- 退款对话框 -->
    <el-dialog
        v-model="refundDialogVisible"
        @close="closeDialog"
        width="350px"
        center>
      <el-form>
        <el-form-item label="退款原因">
          <el-select v-model="reason" placeholder="请选择退款原因">
            <el-option label="不喜欢" value="不喜欢"></el-option>
            <el-option label="买错了" value="买错了"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="confirmRefund()" :disabled="refundSubmitBtnDisabled">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>

</style>