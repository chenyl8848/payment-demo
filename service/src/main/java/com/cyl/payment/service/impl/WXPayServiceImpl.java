package com.cyl.payment.service.impl;

import com.cyl.payment.config.WXPayConfig;
import com.cyl.payment.entity.OrderInfo;
import com.cyl.payment.entity.RefundInfo;
import com.cyl.payment.enums.OrderStatus;
import com.cyl.payment.enums.wxpay.WXApiUrl;
import com.cyl.payment.enums.wxpay.WXNotifyUrl;
import com.cyl.payment.enums.wxpay.WXTradeState;
import com.cyl.payment.service.OrderInfoService;
import com.cyl.payment.service.PaymentInfoService;
import com.cyl.payment.service.RefundInfoService;
import com.cyl.payment.service.WXPayService;
import com.google.gson.Gson;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author cyl
 * @date 2022-11-27 17:46
 * @description
 */
@Service
@Slf4j
public class WXPayServiceImpl implements WXPayService {

    @Autowired
    private WXPayConfig wxPayConfig;

    @Autowired
    private CloseableHttpClient wxPayHttpClient;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private RefundInfoService refundInfoService;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 发起支付请求
     *
     * @param productId 商品id
     * @return 支付地址 + 订单
     */
    @Override
    public Map<String, Object> nativePay(Long productId) throws Exception {

        log.info("生成订单,商品id:{}", productId);
        // 1.生成订单
        OrderInfo orderInfo = orderInfoService.generateOrderByProductId(productId);
        if (Objects.isNull(orderInfo)) {
            throw new RuntimeException("生成订单失败");
        }

        if (!StringUtils.isEmpty(orderInfo.getCodeUrl())) {
            log.info("订单已存在，直接返回支付地址");
            HashMap<String, Object> resultMap = new HashMap<>();
            resultMap.put("codeUrl", orderInfo.getCodeUrl());
            resultMap.put("orderNo", orderInfo.getOrderNo());
            return resultMap;
        }

        log.info("调用统一下单API");
        // 2.调用统一下单 API
        //请求URL
        HttpPost httpPost = new HttpPost(wxPayConfig.getDomain().concat(WXApiUrl.NATIVE_PAY.getUrl()));
        // 请求body参数
        Gson gson = new Gson();
        Map paramMap = new HashMap();
        paramMap.put("appid", wxPayConfig.getAppid());
        paramMap.put("mchid", wxPayConfig.getMchId());
        paramMap.put("description", orderInfo.getTitle());
        paramMap.put("out_trade_no", orderInfo.getOrderNo());
        paramMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WXNotifyUrl.NATIVE_NOTIFY.getUrl()));

        Map amountMap = new HashMap();
        amountMap.put("total", orderInfo.getTotalFee());
        amountMap.put("currency", "CNY");
        paramMap.put("amount", amountMap);

        String reqdata = gson.toJson(paramMap);
        log.info("调用统一下单API,请求参数:{}", reqdata);

        StringEntity entity = new StringEntity(reqdata, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayHttpClient.execute(httpPost);

        try {
            // 响应状态码
            int statusCode = response.getStatusLine().getStatusCode();
            // 响应体
            String bodyAsString = EntityUtils.toString(response.getEntity());

            if (statusCode == 200) {
                //处理成功
                log.info("调用统一下单API,下单成功,返回参数:{}", bodyAsString);
            } else if (statusCode == 204) {
                //处理成功，无返回Body
                log.info("调用统一下单API,下单成功,无返回参数");
            } else {
                log.info("调用统一下单API,下单失败,响应码:{},返回结果:{}", statusCode, bodyAsString);
                throw new IOException("request failed");
            }

            // 响应结果
            HashMap<String, String> responseMap = gson.fromJson(bodyAsString, HashMap.class);
            String codeUrl = responseMap.get("code_url");

            HashMap<String, Object> resultMap = new HashMap<>();
            resultMap.put("codeUrl", codeUrl);
            resultMap.put("orderNo", orderInfo.getOrderNo());

            // 保存支付地址
            orderInfo.setCodeUrl(codeUrl);
            orderInfoService.saveOrUpdateCodeUrl(orderInfo);

            return resultMap;
        } finally {
            response.close();
        }

    }

    @Override
    public void processOrderInfo(Map<String, Object> map) throws GeneralSecurityException {
        log.info("处理订单");
        // 解密
        String plainText = decryptFromResource(map);

        Gson gson = new Gson();
        HashMap plainMap = gson.fromJson(plainText, HashMap.class);
        String orderNo = (String) plainMap.get("out_trade_no");

        // 处理重复的通知
        // 接口调用的幂等性：无论接口被调用多少次，产生的结果是一致的
        /**
         * 在对业务数据进行状态检查和处理之前，要采用数据锁进行并发控制，以避免函数重入造成的数据混乱。
         */
        // 尝试获取锁：成功获取则立即返回 true,获取失败则立即返回 false,不会一致等待锁的释放
        if (lock.tryLock()) {
            try {
                String orderStatus = orderInfoService.getOrderStatusByOrderNo(orderNo);
                if (!OrderStatus.NOTPAY.getType().equals(orderStatus)) {
                    return;
                }

                // 模拟通知并发
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 更新订单状态
                orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.SUCCESS);

                // 记录支付日志
                paymentInfoService.createPaymentInfo(plainText);
            } finally {
                // 释放锁
                lock.unlock();
            }
        }

    }

    @Override
    public void nativeCancel(String orderNo) throws Exception {
        // 调用微信支付的关单接口
        closeOrder(orderNo);

        // 更新商户端的订单
        orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.CANCEL);
    }

    @Override
    public String queryOrder(String orderNo) throws Exception {

        log.info("微信支付查询订单,订单号:{}", orderNo);

        String url = String.format(WXApiUrl.ORDER_QUERY_BY_NO.getUrl(), orderNo);
        url = wxPayConfig.getDomain().concat(url).concat("?mchid=").concat(wxPayConfig.getMchId());
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayHttpClient.execute(httpGet);

        try {
            // 响应状态码
            int statusCode = response.getStatusLine().getStatusCode();
            // 响应体
            String bodyAsString = EntityUtils.toString(response.getEntity());

            if (statusCode == 200) {
                //处理成功
                log.info("微信支付查询订单,查询成功,返回参数:{}", bodyAsString);
            } else if (statusCode == 204) {
                //处理成功，无返回Body
                log.info("微信支付查询订单,查询成功,无返回参数");
            } else {
                log.info("微信支付查询订单,查询失败,响应码:{},返回结果:{}", statusCode, bodyAsString);
                throw new IOException("request failed");
            }

            return bodyAsString;
        } finally {
            response.close();
        }

    }

    @Override
    public void checkOrderStatus(String orderNo) throws Exception {
        log.warn("根据订单号核实订单号,订单号:{]", orderNo);

        // 调用微信支付查询订单接口
        String queryOrder = this.queryOrder(orderNo);

        Gson gson = new Gson();
        HashMap resultMap = gson.fromJson(queryOrder, HashMap.class);

        // 获取微信支付订单状态
        String tradeState = (String) resultMap.get("trade_state");
        if (WXTradeState.SUCCESS.getType().equals(tradeState)) {
            log.warn("核实订单已支付,订单号:{}", orderNo);

            // 如果确认订单已支付则更新本地订单状态
            orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
            // 记录支付日志
            paymentInfoService.createPaymentInfo(queryOrder);
        }

        if (WXTradeState.NOTPAY.getType().equals(tradeState)) {
            log.warn("核实订单未支付,订单号:{}", orderNo);

            // 如果订单未支付，则调用关闭订单接口
            this.closeOrder(orderNo);

            // 更新本地订单接口
            orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.CLOSED);

        }

    }

    @Override
    public void refund(String orderNo, String reason) throws IOException {
        log.info("微信支付申请退款,订单号:{}", orderNo);

        // 根据订单编号创建退款记录
        RefundInfo refundInfo = refundInfoService.createRefundByOrderNo(orderNo, reason);

        // 调用退款 API
        String url = wxPayConfig.getDomain().concat(WXApiUrl.DOMESTIC_REFUNDS.getUrl());
        HttpPost httpPost = new HttpPost(url);

        Gson gson = new Gson();
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("out_trade_no", orderNo);
        paramMap.put("out_refund_no", refundInfo.getRefundNo());
        paramMap.put("reason", reason);
        paramMap.put("notify_url", wxPayConfig.getDomain().concat(WXNotifyUrl.REFUND_NOTIFY.getUrl()));

        Map<String, Object> amoutMap = new HashMap<>();
        amoutMap.put("refund", refundInfo.getRefund());
        amoutMap.put("total", refundInfo.getTotalFee());
        amoutMap.put("currency", "CNY");

        paramMap.put("amount", amoutMap);

        String param = gson.toJson(paramMap);
        log.info("微信支付申请退款,请求参数:{}", param);

        StringEntity entity = new StringEntity(param, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayHttpClient.execute(httpPost);

        try {
            // 响应状态码
            int statusCode = response.getStatusLine().getStatusCode();
            // 响应体
            String bodyAsString = EntityUtils.toString(response.getEntity());

            if (statusCode == 200) {
                //处理成功
                log.info("微信支付申请退款,调用成功,返回参数:{}", bodyAsString);
            } else if (statusCode == 204) {
                //处理成功，无返回Body
                log.info("微信支付申请退款,调用成功,无返回参数");
            } else {
                log.info("微信支付申请退款,调用失败,响应码:{},返回结果:{}", statusCode, bodyAsString);
                throw new RuntimeException("微信支付申请退款异常,响应码:" + statusCode + ",返回结果:" + bodyAsString);
            }

            // 更新订单状态
            orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.REFUND_PROCESSING);

            // 更新腿狂单
            refundInfoService.updateRefund(bodyAsString);

        } finally {
            response.close();
        }

    }

    /**
     * 关闭订单
     *
     * @param orderNo
     */
    private void closeOrder(String orderNo) throws Exception {
        log.info("微信支付关闭订单，订单号:{}", orderNo);

        // 创建远程调用请求
        String url = String.format(WXApiUrl.CLOSE_ORDER_BY_NO.getUrl(), orderNo);
        url = wxPayConfig.getDomain().concat(url);
        HttpPost httpPost = new HttpPost(url);

        // 组装 json 请求体
        Gson gson = new Gson();
        HashMap<String, String> paramsMap = new HashMap<>();
        paramsMap.put("mchid", wxPayConfig.getMchId());
        String params = gson.toJson(paramsMap);
        log.info("微信支付关闭订单，请求参数:{}", params);

        StringEntity entity = new StringEntity(params, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayHttpClient.execute(httpPost);

        try {
            // 响应状态码
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                //处理成功
                log.info("微信支付关闭订单,关闭订单成功");
            } else if (statusCode == 204) {
                //处理成功，无返回Body
                log.info("微信支付关闭订单,关闭订单成功,无返回参数");
            } else {
                log.info("微信支付关闭订单,关闭订单失败,响应码:{}", statusCode);
            }
        } finally {
            response.close();
        }

    }

    /**
     * 对称解密
     *
     * @param map
     * @return
     */
    private String decryptFromResource(Map<String, Object> map) throws GeneralSecurityException {
        log.info("密文解密");

        // 通知数据
        Map<String, String> resourceMap = (Map<String, String>) map.get("resource");
        // 数据密文
        String ciphertext = resourceMap.get("ciphertext");
        // 随机串
        String nonce = resourceMap.get("nonce");
        // 附加数据
        String associatedData = resourceMap.get("associated_data");

        log.info("数据密文:{}", ciphertext);

        AesUtil aesUtil = new AesUtil(wxPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        String plainText = aesUtil.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext);

        log.info("数据明文:{}", plainText);


        return plainText;


    }
}
