package com.cyl.payment.controller;

import com.cyl.payment.config.WxPayConfig;
import com.cyl.payment.vo.R;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author cyl
 * @date 2022-11-27 10:46
 * @description
 */
@Api(tags = "测试接口")
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private WxPayConfig wxPayConfig;

    @GetMapping("/getWXPayConfig")
    public R getWXPayConfig() {
        return R.ok().data("mchId", wxPayConfig.getMchId());
    }
}
