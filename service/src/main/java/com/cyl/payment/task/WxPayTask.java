package com.cyl.payment.task;

import com.cyl.payment.entity.OrderInfo;
import com.cyl.payment.service.OrderInfoService;
import com.cyl.payment.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author cyl
 * @date 2022-12-04 21:23
 * @description 微信支付定时任务
 */
@Component
@Slf4j
public class WxPayTask {

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private WxPayService wxPayService;

    /**
     * 秒 分 时 日 月 周
     * <p>
     * 以秒为例
     * *: 每秒都执行
     * 1-3: 从第 1 秒开始执行，到第 3 秒结束执行
     * 0/3: 从第 0 秒开始，每隔 3 秒执行一次
     * 1,2,3: 在指定的第 1、2、3 秒执行
     * ?: 不指定
     * 日和周时互斥的，不能同时指定，指定其中一个。另一个则设置 ?
     */
//    @Scheduled(cron = "0/3 * * * * ?")
    public void task1() {
        log.info("task1 被执行...");
    }

    /**
     * 从第 0 秒开始每隔 30 秒执行 1 次，查询创建超过 5 分钟，并且未支付的订单
     */
//    @Scheduled(cron = "0/30 * * * * ?")
    public void orderConfirm() {
        log.info("orderConfirm 被执行...");

        List<OrderInfo> list = orderInfoService.getNotPayOrderByDuration(1);

        list.forEach(item -> {
            String orderNo = item.getOrderNo();
            log.warn("微信支付超时订单,订单号:{}", orderNo);

            // 核实订单状态：调用微信支付查询订单接口
            try {
                wxPayService.checkOrderStatus(orderNo);
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }
}
