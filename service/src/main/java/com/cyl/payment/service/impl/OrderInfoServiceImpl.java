package com.cyl.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cyl.payment.entity.OrderInfo;
import com.cyl.payment.entity.Product;
import com.cyl.payment.enums.OrderStatus;
import com.cyl.payment.mapper.OrderInfoMapper;
import com.cyl.payment.mapper.ProductMapper;
import com.cyl.payment.service.OrderInfoService;
import com.cyl.payment.util.OrderNoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public OrderInfo generateOrderByProductId(Long productId) {
        Product product = productMapper.selectById(productId);
        if (Objects.isNull(product)) {
            throw new RuntimeException("商品不存在");
        }

        OrderInfo orderInfo = getNotPayOrderByProductId(productId);
        if (Objects.nonNull(orderInfo)) {
            return orderInfo;
        }

        orderInfo = new OrderInfo();
        orderInfo.setTitle(product.getTitle());
        // 订单号
        orderInfo.setOrderNo(OrderNoUtil.getOrderNo());
        orderInfo.setProductId(productId);
        // 订单金额 单位：分
        orderInfo.setTotalFee(product.getPrice());
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        // 保存订单
        baseMapper.insert(orderInfo);

        return orderInfo;
    }

    @Override
    public void saveOrUpdateCodeUrl(OrderInfo orderInfo) {
        baseMapper.updateById(orderInfo);
    }

    @Override
    public List<OrderInfo> listByDescCreateTime() {
        LambdaQueryWrapper<OrderInfo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.orderByDesc(OrderInfo::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public void updateOrderStatusByOrderNo(String orderNo, OrderStatus orderStatus) {
        log.info("更新订单状态,订单号:{},订单状态:{}", orderNo, orderStatus.getType());

        LambdaQueryWrapper<OrderInfo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(OrderInfo::getOrderNo, orderNo);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(orderStatus.getType());
        baseMapper.update(orderInfo, queryWrapper);

    }

    @Override
    public String getOrderStatusByOrderNo(String orderNo) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo orderInfo = baseMapper.selectOne(queryWrapper);

        if (Objects.isNull(orderInfo)) {
            return null;
        }

        return orderInfo.getOrderStatus();
    }

    @Override
    public List<OrderInfo> getNotPayOrderByDuration(int minutes) {
        Instant instant = Instant.now().minus(Duration.ofMinutes(minutes));

        LambdaQueryWrapper<OrderInfo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(OrderInfo::getOrderStatus, OrderStatus.NOTPAY.getType());
        queryWrapper.le(OrderInfo::getCreateTime, instant);

        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public OrderInfo getOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(OrderInfo::getOrderNo, orderNo);

        return baseMapper.selectOne(queryWrapper);
    }

    private OrderInfo getNotPayOrderByProductId(Long productId) {

        LambdaQueryWrapper<OrderInfo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(OrderInfo::getProductId, productId);
        queryWrapper.eq(OrderInfo::getOrderStatus, OrderStatus.NOTPAY.getType());

        return baseMapper.selectOne(queryWrapper);
    }


}
