package com.cyl.payment.controller;

import com.cyl.payment.service.WxPayService;
import com.cyl.payment.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author cyl
 * @date 2022-12-07 9:06
 * @description 微信支付V2 接口
 */
@Api(tags = "微信支付V2版本")
@RestController
@RequestMapping("/wx-pay-v2")
@Slf4j
public class WxPayV2Controller {

    @Autowired
    private WxPayService wxPayService;

    @ApiOperation("统一下单")
    @PostMapping("/native/{productId}")
    public R nativePay(@PathVariable Long productId, HttpServletRequest request) throws Exception {

        String remoteAddr = request.getRemoteAddr();
        Map<String, Object> resultMap = wxPayService.nativeV2Pay(productId, remoteAddr);
        return R.ok().setMessage("下单成功").setData(resultMap);
    }

    @ApiOperation("支付通知")
    @PostMapping("/native/notify")
    public String nativeNotify(HttpServletRequest request) throws Exception {

        String response = wxPayService.nativeV2PayNotify(request);
        return response;

    }


}
