package com.cyl.payment.service.impl;

import com.cyl.payment.config.WxPayConfig;
import com.cyl.payment.entity.OrderInfo;
import com.cyl.payment.entity.RefundInfo;
import com.cyl.payment.enums.OrderStatus;
import com.cyl.payment.enums.wxpay.WxApiUrl;
import com.cyl.payment.enums.wxpay.WxNotifyUrl;
import com.cyl.payment.enums.wxpay.WxTradeState;
import com.cyl.payment.service.OrderInfoService;
import com.cyl.payment.service.PaymentInfoService;
import com.cyl.payment.service.RefundInfoService;
import com.cyl.payment.service.WxPayService;
import com.cyl.payment.util.HttpClientUtil;
import com.cyl.payment.util.HttpUtil;
import com.github.wxpay.sdk.WXPayUtil;
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

import javax.servlet.http.HttpServletRequest;
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
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private WxPayConfig wxPayConfig;

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
        HttpPost httpPost = new HttpPost(wxPayConfig.getDomain().concat(WxApiUrl.NATIVE_PAY.getUrl()));
        // 请求body参数
        Gson gson = new Gson();
        Map paramMap = new HashMap();
        paramMap.put("appid", wxPayConfig.getAppid());
        paramMap.put("mchid", wxPayConfig.getMchId());
        paramMap.put("description", orderInfo.getTitle());
        paramMap.put("out_trade_no", orderInfo.getOrderNo());
        paramMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyUrl.NATIVE_NOTIFY.getUrl()));

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

        String url = String.format(WxApiUrl.ORDER_QUERY_BY_NO.getUrl(), orderNo);
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
                throw new RuntimeException("微信支付查询订单,查询失败,响应码:" + statusCode + ",返回结果:" + bodyAsString);
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
        if (WxTradeState.SUCCESS.getType().equals(tradeState)) {
            log.warn("核实订单已支付,订单号:{}", orderNo);

            // 如果确认订单已支付则更新本地订单状态
            orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
            // 记录支付日志
            paymentInfoService.createPaymentInfo(queryOrder);
        }

        if (WxTradeState.NOTPAY.getType().equals(tradeState)) {
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
        String url = wxPayConfig.getDomain().concat(WxApiUrl.DOMESTIC_REFUNDS.getUrl());
        HttpPost httpPost = new HttpPost(url);

        Gson gson = new Gson();
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("out_trade_no", orderNo);
        paramMap.put("out_refund_no", refundInfo.getRefundNo());
        paramMap.put("reason", reason);
        paramMap.put("notify_url", wxPayConfig.getDomain().concat(WxNotifyUrl.REFUND_NOTIFY.getUrl()));

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

    @Override
    public String queryRefund(String refundNo) throws Exception {
        log.info("微信支付查询退款,退款单号:{}", refundNo);

        String url = String.format(WxApiUrl.DOMESTIC_REFUNDS_QUERY.getUrl(), refundNo);
        url = wxPayConfig.getDomain().concat(url);

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
                log.info("微信支付查询退款,查询成功,返回参数:{}", bodyAsString);
            } else if (statusCode == 204) {
                //处理成功，无返回Body
                log.info("微信支付查询退款,查询成功,无返回参数");
            } else {
                log.info("微信支付查询退款,查询失败,响应码:{},返回结果:{}", statusCode, bodyAsString);
                throw new RuntimeException("微信支付查询退款,查询失败,响应码:" + statusCode + ",返回结果:" + bodyAsString);
            }

            return bodyAsString;
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
        String url = String.format(WxApiUrl.CLOSE_ORDER_BY_NO.getUrl(), orderNo);
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

    @Override
    public void processRefund(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("处理退款单");
        // 解密
        String plainText = decryptFromResource(bodyMap);

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
                if (!OrderStatus.REFUND_PROCESSING.getType().equals(orderStatus)) {
                    return;
                }

                // 更新订单状态
                orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);

                // 记录支付日志
                refundInfoService.updateRefund(plainText);
            } finally {
                // 释放锁
                lock.unlock();
            }
        }
    }

    @Override
    public String queryBill(String type, String billDate) throws Exception {
        log.info("微信支付查询账单,交易日期:{}", billDate);
        String url = "";
        if ("tradebill".equals(type)) {
            url = wxPayConfig.getDomain().concat(WxApiUrl.TRADE_BILLS.getUrl());
        } else if ("fundflowbill".equals(type)) {
            url = wxPayConfig.getDomain().concat(WxApiUrl.FUND_FLOW_BILLS.getUrl());
        } else {
            throw new IllegalArgumentException("不支持的账单类型");
        }

        url = url.concat("?bill_date=").concat(billDate);

        // 创建远程调用请求
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
                log.info("微信支付查询账单,查询成功,返回结果:{}", bodyAsString);
            } else if (statusCode == 204) {
                //处理成功，无返回Body
                log.info("微信支付查询账单,查询成功,无返回参数");
            } else {
                log.info("微信支付查询账单,查询失败,响应码:{}", statusCode);
                throw new RuntimeException("微信支付查询账单,查询失败,响应码:" + statusCode);
            }

            Gson gson = new Gson();
            HashMap resultMap = gson.fromJson(bodyAsString, HashMap.class);
            String downloadUrl = (String) resultMap.get("download_url");

            return downloadUrl;
        } finally {
            response.close();
        }
    }

    @Override
    public String downloadBill(String type, String billDate) throws Exception {
        log.info("微信支付下载账单,账单类型:{},交易日期:{}", type, billDate);
        String downloadUrl = queryBill(type, billDate);

        // 创建远程调用请求
        HttpGet httpGet = new HttpGet(downloadUrl);
        httpGet.addHeader("Accept", "application/json");

        CloseableHttpResponse response = wxPayHttpClient.execute(httpGet);
        try {
            // 响应状态码
            int statusCode = response.getStatusLine().getStatusCode();
            // 响应体
            String bodyAsString = EntityUtils.toString(response.getEntity());

            if (statusCode == 200) {
                //处理成功
                log.info("微信支付下载账单,查询成功,返回结果:{}", bodyAsString);
            } else if (statusCode == 204) {
                //处理成功，无返回Body
                log.info("微信支付下载账单,查询成功,无返回参数");
            } else {
                log.info("微信支付下载账单,查询失败,响应码:{}", statusCode);
                throw new RuntimeException("微信支付下载账单,查询失败,响应码:" + statusCode);
            }

            return bodyAsString;
        } finally {
            response.close();
        }

    }

    @Override
    public Map<String, Object> nativeV2Pay(Long productId, String remoteAddr) throws Exception {
        log.info("微信支付V2版本,统一下单,商品id:{},客户端ip:{}", productId, remoteAddr);

        // 生成订单
        OrderInfo orderInfo = orderInfoService.generateOrderByProductId(productId);
        if (Objects.isNull(orderInfo)) {
            throw new RuntimeException("生成订单失败");
        }
        if (!StringUtils.isEmpty(orderInfo.getCodeUrl())) {
            // 订单已存在,直接返回
            log.info("订单已存在,商品id:{},订单号:{}", productId, orderInfo.getOrderNo());

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("codeUrl", orderInfo.getCodeUrl());
            resultMap.put("orderNo", orderInfo.getOrderNo());
            return resultMap;
        }

        HttpClientUtil httpClientUtil = new HttpClientUtil(wxPayConfig.getDomain().concat(WxApiUrl.NATIVE_PAY_V2.getUrl()));

        // 组装接口参数
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("appid", wxPayConfig.getAppid());
        paramMap.put("mch_id", wxPayConfig.getMchId());
        paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
        paramMap.put("body", orderInfo.getTitle());
        paramMap.put("out_trade_no", orderInfo.getOrderNo());

        // 注意：这里必须使用字符串类型的参数（总金额：分）
        String totalFee = orderInfo.getTotalFee() + "";
        paramMap.put("total_fee", totalFee);
        paramMap.put("spbill_create_ip", remoteAddr);
        paramMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyUrl.NATIVE_NOTIFY_V2.getUrl()));
        paramMap.put("trade_type", "NATIVE");

        // 将参数转成 xml 字符串格式，生成带有签名的 xml 格式字符串
        String paramsXml = WXPayUtil.generateSignedXml(paramMap, wxPayConfig.getPartnerKey());
        log.info("\n paramsXml：\n" + paramsXml);

        httpClientUtil.setXmlParam(paramsXml);
        httpClientUtil.setHttps(true);

        // 发送请求
        httpClientUtil.post();

        // 得到响应结果
        String resultXml = httpClientUtil.getContent();
        log.info("\n resultXml：\n" + resultXml);

        // 将 xml 格式转成 map
        Map<String, String> responseMap = WXPayUtil.xmlToMap(resultXml);

        // 错误处理
        if ("FAIL".equals(responseMap.get("return_code")) || "FAIL".equals(responseMap.get("result_code"))) {
            log.info("微信支付V2版本,统一下单,下单失败:{}", resultXml);
            throw new RuntimeException("微信支付V2版本,统一下单,下单失败");
        }

        // 支付地址
        String codeUrl = responseMap.get("code_url");
        // 订单号
        String orderNo = orderInfo.getOrderNo();

        orderInfo.setCodeUrl(codeUrl);
        orderInfoService.saveOrUpdateCodeUrl(orderInfo);

        // 返回支付地址
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("codeUrl", codeUrl);
        resultMap.put("orderNo", orderNo);

        return resultMap;
    }

    @Override
    public String nativeV2PayNotify(HttpServletRequest request) throws Exception {

        log.info("微信支付V2,支付通知回调");
        // 应答对象
        Map<String, String> reponseMap = new HashMap<>();

        // 处理通知参数
        String notifyBody = HttpUtil.readData(request);
        log.info("微信支付V2,支付通知回调,请求参数:{}", notifyBody);

        // 验签
        if (!WXPayUtil.isSignatureValid(notifyBody, wxPayConfig.getPartnerKey())) {
            log.error("微信支付V2,支付通知回调,验签失败");

            // 返回失败应答
            reponseMap.put("return_code", "FAIL");
            reponseMap.put("return_msg", "验签失败");

            String responseXml = WXPayUtil.mapToXml(reponseMap);
            return responseXml;
        }

        // 解析 xml 数据
        Map<String, String> notifyMap = WXPayUtil.xmlToMap(notifyBody);
        // 判断通信和业务是否成功
        if (!"SUCCESS".equals(notifyMap.get("return_code")) || !"SUCCESS".equals(notifyMap.get("result_code"))) {
            log.error("微信支付V2,支付通知回调,业务失败");
            // 返回失败应答
            reponseMap.put("return_code", "FAIL");
            reponseMap.put("return_msg", "失败");

            String responseXml = WXPayUtil.mapToXml(reponseMap);
            return responseXml;
        }

        // 获取商户订单号
        String orderNo = notifyMap.get("out_trade_no");
        long totalFee = Long.parseLong(notifyMap.get("total_fee"));
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);

        //校验返回的订单金额是否与商户侧的订单金额一致
        if (Objects.isNull(orderInfo) || orderInfo.getTotalFee() != totalFee) {
            log.error("微信支付V2,支付通知回调,订单不存在或订单金额不一致,订单号:{},微信侧金额:{}", orderNo, totalFee);
            // 返回失败应答
            reponseMap.put("return_code", "FAIL");
            reponseMap.put("return_msg", "失败");

            String responseXml = WXPayUtil.mapToXml(reponseMap);
            return responseXml;
        }

        // 处理订单
        if (lock.tryLock()) {
            try {
                // 处理重复的通知
                // 接口的幂等性：无论接口调用多少起，产生的结果是一致的
                String orderStatus = orderInfoService.getOrderStatusByOrderNo(orderNo);
                if (OrderStatus.NOTPAY.getType().equals(orderStatus)) {
                    // 更新订单状态
                    orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.SUCCESS);

                    // 记录支付日志
                    paymentInfoService.createPaymentInfo(notifyBody);
                }
            } finally {
                lock.unlock();
            }
        }

        // 返回成功应答
        reponseMap.put("return_code", "SUCCESS");
        reponseMap.put("return_msg", "OK");

        String responseXml = WXPayUtil.mapToXml(reponseMap);
        return responseXml;
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
