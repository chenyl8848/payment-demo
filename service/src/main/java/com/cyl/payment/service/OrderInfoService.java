package com.cyl.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cyl.payment.entity.OrderInfo;
import com.cyl.payment.enums.OrderStatus;

import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 根据商品 id 生成订单
     *
     * @param productId 商品
     * @return
     */
    OrderInfo generateOrderByProductId(Long productId);

    /**
     * 保存或更新订单支付地址
     *
     * @param orderInfo
     */
    void saveOrUpdateCodeUrl(OrderInfo orderInfo);

    /**
     * 根据创建时间倒叙查询订单列表
     *
     * @return
     */
    List<OrderInfo> listByDescCreateTime();

    /**
     * 根据订单号更新订单状态
     *
     * @param orderNo     订单号
     * @param orderStatus 订单状态
     */
    void updateOrderStatusByOrderNo(String orderNo, OrderStatus orderStatus);

    /**
     * 根据订单号获取订单状态
     *
     * @param orderNo
     * @return
     */
    String getOrderStatusByOrderNo(String orderNo);

    /**
     * 查询超过一定时间未支付的订单
     *
     * @param minutes 时间 单位：分钟
     * @return
     */
    List<OrderInfo> getNotPayOrderByDuration(int minutes);

    /**
     * 根据订单号获取订单信息
     *
     * @param orderNo 订单号
     * @return
     */
    OrderInfo getOrderByOrderNo(String orderNo);
}
