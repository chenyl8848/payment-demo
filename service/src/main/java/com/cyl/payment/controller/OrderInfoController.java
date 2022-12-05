package com.cyl.payment.controller;

import com.cyl.payment.entity.OrderInfo;
import com.cyl.payment.enums.OrderStatus;
import com.cyl.payment.service.OrderInfoService;
import com.cyl.payment.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author cyl
 * @date 2022-12-03 10:39
 * @description 订单接口
 */
@Api(tags = "订单管理")
@RestController
@RequestMapping("/orderInfo")
public class OrderInfoController {

    @Autowired
    private OrderInfoService orderInfoService;

    @ApiOperation("订单列表")
    @GetMapping("/list")
    public R list() {
        List<OrderInfo> list = orderInfoService.listByDescCreateTime();

        return R.ok().data("orderInfoList", list);
    }

    @ApiOperation("查询订单状态")
    @GetMapping("/queryOrderStatus/{orderNo}")
    public R queryOrderStatus(@PathVariable String orderNo) {
        String orderStatus = orderInfoService.getOrderStatusByOrderNo(orderNo);

        if (OrderStatus.SUCCESS.getType().equals(orderStatus)) {
            return R.ok().setMessage("支付成功");
        }

        return R.ok().setCode(101).setMessage("支付中");
    }


}
