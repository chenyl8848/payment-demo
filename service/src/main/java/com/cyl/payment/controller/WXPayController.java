package com.cyl.payment.controller;

import com.cyl.payment.service.WXPayService;
import com.cyl.payment.util.HttpUtil;
import com.cyl.payment.util.WechatPay2ValidatorUtil;
import com.cyl.payment.vo.R;
import com.google.gson.Gson;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author cyl
 * @date 2022-11-27 17:45
 * @description 微信支付接口
 */
@Api(tags = "微信支付接口")
@RestController
@RequestMapping("/wx-pay")
@Slf4j
public class WXPayController {

    @Autowired
    private WXPayService wxPayService;

    @Autowired
    private Verifier verifier;

    @ApiOperation("调用统一API,生成支付二维码")
    @PostMapping("/native/{productId}")
    public R nativePay(@PathVariable Long productId) throws Exception {
        log.info("发起支付请求,商品：{}", productId);

        // 返回支付二维码连接和订单号
        Map<String, Object> result = wxPayService.nativePay(productId);

        return R.ok().setData(result);
    }

    @ApiOperation("微信支付通知")
    @PostMapping("/native/notify")
    public String nativeNotify(HttpServletRequest request, HttpServletResponse response) {

        Gson gson = new Gson();
        Map<String, String> responseData = new HashMap<>();
        try {
            String data = HttpUtil.readData(request);

            Map<String, Object> map = gson.fromJson(data, HashMap.class);
            String requestId = (String) map.get("id");
            log.info("支付通知的id:{}", requestId);
            log.info("支付通知的数据:{}", data);

            WechatPay2ValidatorUtil wechatPay2ValidatorUtil = new WechatPay2ValidatorUtil(verifier, requestId, data);
            if (wechatPay2ValidatorUtil.validate(request)) {
                log.info("通知验签失败");
                // 失败应答
                response.setStatus(500);
                responseData.put("code", "ERROR");
                responseData.put("message", "验签失败");

                return gson.toJson(responseData);
            }
            log.info("通知验签成功");

            //处理订单
            wxPayService.processOrderInfo(map);

            // 模拟失败应答
            // int i = 10 / 0;
            // 应答超时
            // TimeUnit.SECONDS.sleep(5);

            // 返回成功应答
            response.setStatus(200);
            responseData.put("code", "SUCCESS");
            responseData.put("message", "成功");

            return gson.toJson(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            // 返回失败应答
            response.setStatus(500);
            responseData.put("code", "ERROR");
            responseData.put("message", "失败");

            return gson.toJson(responseData);

        }

    }

    @ApiOperation("微信支付取消订单")
    @PostMapping("/native/cancel/{orderNo}")
    public R cancel(@PathVariable String orderNo) throws Exception {

        wxPayService.nativeCancel(orderNo);

        return R.ok().setMessage("订单取消成功");
    }

    @ApiOperation("微信支付查询订单")
    @GetMapping("/native/queryOrder/{orderNo}")
    public R queryOrder(@PathVariable String orderNo) throws Exception {

        String result = wxPayService.queryOrder(orderNo);

        return R.ok().setMessage("查询成功").data("result", result);
    }

    @ApiOperation("申请退款")
    @PostMapping("/native/refund/{orderNo}/{reason}")
    public R refund(String orderNo, String reason) throws IOException {
        wxPayService.refund(orderNo, reason);
        return R.ok();

    }
}
