<script lang="ts" setup>
import {onMounted, reactive, ref, toRaw, watch} from "vue";
import productApi from "@/api/product";
import wxPayApi from "@/api/wxPay";
import orderApi from "@/api/order";
import QrcodeVue from "qrcode.vue";
import {ElMessage} from "element-plus";
import {useRouter} from "vue-router";

const productList = ref([]);
const getProductList = () => {
  productApi.list().then(res => {
    productList.value = res.data.productList
  })
}
onMounted(() => {
  getProductList()
})

const payOrder = reactive({
  productId: -1, //商品id
  payType: 'wxpay' //支付方式
});

const selectItem = (productId: number) => {
  payOrder.productId = productId;
};

const selectPayType = (payType: string) => {
  payOrder.payType = payType;
};


//确认支付按钮是否禁用
let payBtnDisabled = ref<boolean>(false);
// 微信支付二维码弹窗
let codeDialogVisible = ref<boolean>(false);
// 支付二维码
const codeUrl = ref<string>('');
// 订单号
const orderNo = ref<string>('');
// 定时器
let timer = ref<any>(null);
// 获取路由器
const router = useRouter();

const queryOrderStatus = (orderNo: string) => {
  orderApi.queryOrderStatus(orderNo).then(res => {
    console.log("订单状态:", res)
    // 支付成功后跳转成功页面
    if (res.code === 0) {
      console.log("清除定时器");
      clearInterval(timer);
      // 3秒后跳转到订单列表
      setTimeout(() => {
        router.push("/orderList");
      })
    }
  }).catch((error) => {
    console.log(error)
  })
}

const toPay = () => {
  // 防止重复提交
  payBtnDisabled.value = true;

  if (!payOrder.productId || payOrder.productId === -1) {
    ElMessage.error("请先选择商品!")
    payBtnDisabled.value = false;
    return;
  }

  if (payOrder.payType === 'wxpay') {
    wxPayApi.nativePay(toRaw(payOrder.productId)).then(res => {
      codeUrl.value = res.data.codeUrl;
      orderNo.value = res.data.orderNo;
      codeDialogVisible.value = true;

      // 启动定时器
      timer = setInterval(() => {
        // 查询订单是否支付成功
        queryOrderStatus(orderNo.value);
      }, 3000);
    }).catch(error => {
      console.log(error)
    })
  } else if (payOrder.payType === 'wxpayV2') {
    wxPayApi.nativePay(toRaw(payOrder.productId)).then(res => {
      codeUrl.value = res.data.codeUrl;
      orderNo.value = res.data.orderNo;
      codeDialogVisible.value = true;
      // 启动定时器
      timer = setInterval(() => {
        // 查询订单是否支付成功
        queryOrderStatus(orderNo.value);
      }, 3000);
    }).catch(error => {
      console.log(error)
    })
  } else if (payOrder.payType === 'alipay') {
    console.log("alipay..");

  } else {
    ElMessage.error({
      message: '支付方式错误',
      type: 'error'
    });
    payBtnDisabled.value = false;
  }
};

const closeDialog = () => {
  console.log('close.................')
  payBtnDisabled.value = false;
  console.log('清除定时器')
  clearInterval(timer);
  codeDialogVisible.value = false;
}

</script>

<template>
  <div class="bg-fa of">
    <section id="index" class="container">
      <header class="comm-title">
        <h2 class="fl tac">
          <span class="c-333">课程列表</span>
        </h2>
      </header>
      <ul>
        <li v-for="product in productList" :key="product.id">
          <a :class="['orderBtn', {current:payOrder.productId === product.id}]"
             @click="selectItem(product.id)"
             href="javascript:void(0);">
            {{ product.title }}
            ¥{{ product.price / 100 }}
          </a>
        </li>
      </ul>

      <div class="PaymentChannel_payment-channel-panel">
        <h3 class="PaymentChannel_title">
          选择支付方式
        </h3>
        <div class="PaymentChannel_channel-options">

          <!-- 选择微信 -->
          <div :class="['ChannelOption_payment-channel-option', {current:payOrder.payType === 'wxpay'}]"
               @click="selectPayType('wxpay')">
            <div class="ChannelOption_channel-icon">
              <img src="../assets/img/wxpay.png" class="ChannelOption_icon">
            </div>
            <div class="ChannelOption_channel-info">
              <div class="ChannelOption_channel-label">
                <div class="ChannelOption_label">微信V3支付</div>
                <div class="ChannelOption_sub-label"></div>
                <div class="ChannelOption_check-option"></div>
              </div>
            </div>
          </div>

          <div :class="['ChannelOption_payment-channel-option', {current:payOrder.payType === 'wxpayV2'}]"
               @click="selectPayType('wxpayV2')">
            <div class="ChannelOption_channel-icon">
              <img src="../assets/img/wxpay.png" class="ChannelOption_icon">
            </div>
            <div class="ChannelOption_channel-info">
              <div class="ChannelOption_channel-label">
                <div class="ChannelOption_label">微信V2支付</div>
                <div class="ChannelOption_sub-label"></div>
                <div class="ChannelOption_check-option"></div>
              </div>
            </div>
          </div>

          <!-- 选择支付宝 -->
          <div :class="['ChannelOption_payment-channel-option', {current:payOrder.payType === 'alipay'}]"
               @click="selectPayType('alipay')">
            <div class="ChannelOption_channel-icon">
              <img src="../assets/img/alipay.png" class="ChannelOption_icon">
            </div>
            <div class="ChannelOption_channel-info">
              <div class="ChannelOption_channel-label">
                <div class="ChannelOption_label">支付宝</div>
                <div class="ChannelOption_sub-label"></div>
                <div class="ChannelOption_check-option"></div>
              </div>
            </div>
          </div>

        </div>
      </div>

      <div class="payButton">
        <el-button
            :disabled="payBtnDisabled"
            type="warning"
            round
            style="width: 180px;height: 44px;font-size: 18px;"
            @click="toPay()">
          确认支付
        </el-button>
      </div>

    </section>

    <!-- 微信支付二维码 -->
    <el-dialog
        v-model="codeDialogVisible"
        width="350px"
        center
        @close="closeDialog">
      <QrcodeVue :value="codeUrl" :size="300"/>
      <!-- <img src="../assets/img/code.png" alt="" style="width:100%"><br> -->
      <span v-show="payOrder.payType === 'wxpay'">使用微信扫码支付</span>
      <span v-show="payOrder.payType === 'alipay'">使用支付宝扫码支付</span>
    </el-dialog>

  </div>
</template>

<style scoped>

</style>